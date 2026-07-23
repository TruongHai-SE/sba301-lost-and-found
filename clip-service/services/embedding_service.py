import io
import unicodedata

import numpy as np
import requests
from PIL import Image

from config import settings
from models.clip_onnx import CLIPOnnxEngine
from models.yolo_detector import YOLODetector
from services.vector_store import VectorStore


def remove_accents(input_str: str) -> str:
    if not input_str:
        return ""
    # Replace đ and Đ first so they aren't completely deleted during NFKD/ASCII encoding
    input_str = input_str.replace('đ', 'd').replace('Đ', 'd')
    nfkd_form = unicodedata.normalize('NFKD', input_str)
    only_ascii = nfkd_form.encode('ASCII', 'ignore').decode('ASCII')
    return only_ascii


def get_normalized_words(text: str) -> set[str]:
    if not text:
        return set()
    text = remove_accents(text).lower()
    for char in [".", ",", "!", "?", "-", "_", "(", ")", "[", "]", "/", "\\"]:
        text = text.replace(char, " ")
    words = [w.strip() for w in text.split()]
    return set(w for w in words if len(w) >= 2)





class EmbeddingService:
    """Orchestrates CLIP encoding, YOLO cropping, vector storage, and matching."""

    def __init__(self):
        self.clip = CLIPOnnxEngine()
        self.yolo = YOLODetector(settings.resolved_yolo_model_path)
        self.store = VectorStore()


    # Public methods
    def embed_and_index_image(
        self, post_id: int, image_url: str, post_type: str, image_id: int | None = None
    ) -> dict:
        """Download image, crop, encode, store, and match against opposite post type."""
        img = self._download_image(image_url)
        cropped = self.yolo.crop_main_object(img)
        embedding = self.clip.encode_image(cropped)

        emb_id = self.store.upsert(
            post_id=post_id,
            embedding=embedding,
            source_type="IMAGE",
            image_id=image_id,
        )

        # Consistency check: compare IMAGE embedding with existing TEXT embedding
        self._check_consistency(post_id, embedding, "IMAGE")

        # Cross match: LOST searches FOUND, and FOUND searches LOST
        target = "FOUND" if post_type == "LOST" else "LOST"
        matches = self._match(embedding, target_post_type=target, query_type="IMAGE")

        return {"embedding_id": emb_id, "dimension": 768, "matches": matches}

    def embed_and_index_text(
        self, post_id: int, text: str, post_type: str, translate: bool = True
    ) -> dict:
        """Encode text, store the vector, and match against opposite post type."""
        embedding = self.clip.encode_text(text, translate=translate)

        emb_id = self.store.upsert(
            post_id=post_id,
            embedding=embedding,
            source_type="TEXT",
            image_id=None,
        )

        # Consistency check: compare TEXT embedding with existing IMAGE embedding
        self._check_consistency(post_id, embedding, "TEXT")

        # Cross match: LOST searches FOUND, and FOUND searches LOST
        target = "FOUND" if post_type == "LOST" else "LOST"
        matches = self._match(embedding, target_post_type=target, query_type="TEXT", query_text=text)

        return {"embedding_id": emb_id, "dimension": 768, "matches": matches}

    def search(
        self,
        query_text: str | None = None,
        query_image_url: str | None = None,
        target_post_type: str = "ALL",
        top_k: int = 10,
        threshold: float | None = None,
    ) -> list[dict]:
        """Manually search by text or image."""
        threshold = (
            settings.clip_match_threshold if threshold is None else threshold
        )

        q_text = query_text
        if query_image_url:
            img = self._download_image(query_image_url)
            cropped = self.yolo.crop_main_object(img)
            vec = self.clip.encode_image(cropped)
            query_type = "IMAGE"
        elif query_text:
            vec = self.clip.encode_text(query_text, translate=True)
            query_type = "TEXT"
        else:
            return []

        return self._match(vec, target_post_type=target_post_type, query_type=query_type, top_k=top_k, threshold=threshold, query_text=q_text)

    def search_by_image_bytes(
        self,
        image_bytes: bytes,
        target_post_type: str = "ALL",
        top_k: int = 10,
        threshold: float | None = None,
    ) -> list[dict]:
        """Directly search by raw image bytes (RAM processing)."""
        img = Image.open(io.BytesIO(image_bytes)).convert("RGB")
        cropped = self.yolo.crop_main_object(img)
        vec = self.clip.encode_image(cropped)
        return self._match(vec, target_post_type=target_post_type, query_type="IMAGE", top_k=top_k, threshold=threshold)

    def validate_image_and_consistency(
        self,
        image_bytes: bytes | None = None,
        image_url: str | None = None,
        title: str | None = None,
        tags: list[str] | None = None,
    ) -> dict:
        """Validate if image is junk (selfie, screenshot, blank) and check Title-Image consistency."""
        if image_bytes:
            img = Image.open(io.BytesIO(image_bytes)).convert("RGB")
        elif image_url:
            img = self._download_image(image_url)
        else:
            return {"is_valid": False, "reason_code": "NO_IMAGE", "message": "Không tìm thấy hình ảnh."}

        # 0. Check for solid color / blank image via pixel variance
        img_np = np.array(img)
        if float(img_np.std()) < 10.0:
            return {
                "is_valid": False,
                "reason_code": "BLANK",
                "message": "Hình ảnh quá mờ hoặc không có vật thể rõ ràng. Vui lòng chọn ảnh chụp rõ nét hơn."
            }

        # 1. Zero-shot prompts comparison
        img_vec = self.clip.encode_image(img)
        
        forbidden_prompts = [
            ("SELFIE", "a selfie or close-up portrait of a person's face"),
            ("PEOPLE", "a photo of people or group of friends"),
            ("SCREENSHOT", "a screenshot of a phone screen, banking app, or text chat"),
            ("MEME", "an internet meme, text graphic, or cartoon"),
            ("BLANK", "a blank, black, white, or extremely blurry out of focus picture"),
        ]

        allowed_prompt = "a photo of a physical lost or found object like a wallet, bag, card, phone, key, or pet"
        allowed_vec = self.clip.encode_text(allowed_prompt, translate=False)

        allowed_score = float(np.dot(img_vec, allowed_vec))

        highest_forbidden_score = -1.0
        matched_reason = None

        for code, prompt in forbidden_prompts:
            p_vec = self.clip.encode_text(prompt, translate=False)
            score = float(np.dot(img_vec, p_vec))
            if score > highest_forbidden_score:
                highest_forbidden_score = score
                matched_reason = code

        # Run YOLO to check object presence
        cropped = self.yolo.crop_main_object(img)
        yolo_found_object = cropped.size != img.size

        if highest_forbidden_score > 0.70 and highest_forbidden_score > allowed_score and not yolo_found_object:
            messages = {
                "SELFIE": "Hình ảnh chứa chủ yếu mặt người/ảnh chân dung. Vui lòng chọn ảnh chụp rõ đồ vật (Ví, Balo, Giấy tờ...).",
                "PEOPLE": "Hình ảnh chứa mặt người hoặc nhóm người. Vui lòng chọn ảnh chụp rõ món đồ bị mất hoặc nhặt được.",
                "SCREENSHOT": "Hình ảnh là ảnh chụp màn hình/mã QR/ảnh chữ. Vui lòng chọn ảnh chụp món đồ thực tế.",
                "MEME": "Hình ảnh là ảnh minh họa/meme/ảnh chữ. Vui lòng chọn ảnh chụp món đồ thực tế.",
                "BLANK": "Hình ảnh quá mờ hoặc không có vật thể rõ ràng. Vui lòng chọn ảnh chụp rõ nét hơn.",
            }
            msg = messages.get(matched_reason, "Hình ảnh không chứa vật thể tìm kiếm phù hợp. Vui lòng chọn ảnh khác.")
            return {"is_valid": False, "reason_code": matched_reason, "message": msg}

        # 2. Check title-image consistency if title is provided
        if title and title.strip():
            title_vec = self.clip.encode_text(title, translate=True)
            cosine = float(np.dot(img_vec, title_vec))
            if cosine < 0.15:
                return {
                    "is_valid": False,
                    "reason_code": "TITLE_MISMATCH",
                    "message": "Tiêu đề và hình ảnh không khớp nhau. Vui lòng kiểm tra lại thông tin trước khi đăng."
                }

        return {"is_valid": True, "reason_code": "OK", "message": "Hình ảnh hợp lệ."}

    def delete_post_embeddings(self, post_id: int) -> int:
        return self.store.delete_by_post(post_id)

    # Internal methods
    def _scale_score(self, score: float, query_type: str, source_type: str) -> float:
        raw = score * 100.0
        if query_type == source_type:
            if query_type == "TEXT":
                zero_b = 50.0
                min_b = 74.0
                max_b = 99.0
            else:
                zero_b = 20.0
                min_b = 40.0
                max_b = 95.0
        else:
            zero_b = 10.0
            min_b = settings.clip_score_min  # 21.0
            max_b = settings.clip_score_max  # 29.0

        if raw >= min_b:
            scaled = 50.0 + (raw - min_b) / (max_b - min_b) * 50.0
        else:
            scaled = (raw - zero_b) / (min_b - zero_b) * 50.0

        return max(0.0, min(100.0, scaled))

    def _check_consistency(self, post_id: int, new_embedding: np.ndarray, new_type: str):
        """Check consistency between TEXT and IMAGE embeddings of the same post."""
        other_type = "TEXT" if new_type == "IMAGE" else "IMAGE"
        try:
            with self.store.conn:
                with self.store.conn.cursor() as cursor:
                    cursor.execute(
                        "SELECT embedding FROM clip_embeddings WHERE post_id = %s AND source_type = %s LIMIT 1",
                        (post_id, other_type),
                    )
                    row = cursor.fetchone()
                    if row is None:
                        return  # Other embedding not yet indexed
                    # Parse the stored vector string back to numpy array
                    vec_str = row[0]
                    if isinstance(vec_str, str):
                        other_vec = np.array([float(x) for x in vec_str.strip("[]").split(",")])
                    else:
                        other_vec = np.array(vec_str)
                    other_vec = other_vec / np.linalg.norm(other_vec)
                    cosine = float(np.dot(new_embedding, other_vec))
                    if cosine < 0.15:
                        print(f"[Consistency] ⚠️ LOW consistency for post {post_id}: "
                              f"{new_type} vs {other_type} cosine = {cosine:.4f}. "
                              f"Title and image may not match.")
                    else:
                        print(f"[Consistency] ✅ Post {post_id}: "
                              f"{new_type} vs {other_type} cosine = {cosine:.4f}")
        except Exception as e:
            print(f"[Consistency] Error checking post {post_id}: {e}")

    def _match(
        self,
        query_vec: np.ndarray,
        target_post_type: str,
        query_type: str,
        top_k: int = 10,
        threshold: float | None = None,
        query_text: str | None = None,
    ) -> list[dict]:
        threshold = (
            settings.clip_match_threshold if threshold is None else threshold
        )
        # Fetch slightly more than top_k from db to account for grouping by post_id
        results = self.store.search(query_vec, target_post_type, top_k * 2, threshold)

        query_words = get_normalized_words(query_text) if query_text else set()

        posts_data = {}
        for result in results:
            post_id = result["post_id"]
            if post_id not in posts_data:
                posts_data[post_id] = {
                    "result": result,
                    "image_scores": [],
                    "text_scores": []
                }

            source_type = result.get("source_type", "IMAGE")
            score = float(result["score"])
            if source_type == "IMAGE":
                posts_data[post_id]["image_scores"].append(score)
            elif source_type == "TEXT":
                posts_data[post_id]["text_scores"].append(score)

        final_results = []
        for post_id, data in posts_data.items():
            result = data["result"]
            title = result.get("title", "")
            description = result.get("description", "")

            best_img = max(data["image_scores"]) if data["image_scores"] else None
            best_txt = max(data["text_scores"]) if data["text_scores"] else None

            scaled_img = None
            if best_img is not None:
                scaled_img = self._scale_score(best_img, query_type, "IMAGE")

            scaled_txt = None
            if best_txt is not None:
                scaled_txt = self._scale_score(best_txt, query_type, "TEXT")

            has_overlap = False
            overlap_count = 0
            if query_words and (title or description):
                title_desc = f"{title} {description if description else ''}"
                post_words = get_normalized_words(title_desc)
                overlap = query_words.intersection(post_words)
                if overlap:
                    has_overlap = True
                    overlap_count = len(overlap)

            # Combine scores using same-modality weighting
            if scaled_img is not None and scaled_txt is not None:
                if query_type == "IMAGE":
                    combined_score = 0.7 * scaled_img + 0.3 * scaled_txt
                else:
                    combined_score = 0.7 * scaled_txt + 0.3 * scaled_img
            elif scaled_img is not None:
                combined_score = scaled_img
            elif scaled_txt is not None:
                combined_score = scaled_txt
            else:
                combined_score = 0.0

            # Apply lexical boost to combined score
            if has_overlap:
                combined_score = combined_score + (overlap_count * 3.0)

            combined_score = round(max(0.0, min(100.0, combined_score)), 2)

            # Reconstruct result to match expectations
            res = result.copy()
            res["human_score"] = f"{combined_score:.2f}%"
            res["score"] = round(best_img if query_type == "IMAGE" and best_img is not None else (best_txt if best_txt is not None else (best_img if best_img is not None else 0.0)), 4)
            # Remove helper fields to maintain backend compatibility
            res.pop("source_type", None)
            res.pop("title", None)
            res.pop("description", None)
            final_results.append(res)

        final_results.sort(key=lambda x: float(x["human_score"].replace("%", "")), reverse=True)
        return final_results[:top_k]


    @staticmethod
    def _download_image(url: str) -> Image.Image:
        headers = {"User-Agent": "Mozilla/5.0"}
        response = requests.get(url, headers=headers, timeout=15)
        response.raise_for_status()
        return Image.open(io.BytesIO(response.content)).convert("RGB")
