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


CATEGORIES = {
    "backpack": {"balo", "cap", "tui", "bag", "backpack", "vali"},
    "wallet": {"vi", "bop", "wallet"},
    "phone": {"phone", "iphone", "samsung", "oppo", "xiaomi", "redmi", "realme", "vivo", "dien", "thoai"},
    "laptop": {"laptop", "macbook", "asus", "dell", "hp", "lenovo", "thinkpad", "computer", "pc"},
    "keys": {"key", "keys", "chia", "khoa"},
    "card": {"the", "card", "cccd", "cmnd", "atm", "sinh", "vien", "bpl", "gplx"},
    "watch": {"watch", "dong", "ho"},
    "earphones": {"airpods", "tai", "nghe", "headphone", "earphone", "earphones"},
    "jewelry": {"nhan", "vong", "ring", "necklace", "day", "chuyen", "khuyen", "tai", "trang", "suc"},
    "document": {"ho", "so", "giay", "to", "document", "paper"}
}


def get_categories(words: set[str]) -> set[str]:
    matched_cats = set()
    for cat_name, cat_words in CATEGORIES.items():
        if words.intersection(cat_words):
            matched_cats.add(cat_name)
    return matched_cats


CATEGORY_PROMPTS = {
    "backpack": "a photo of a backpack, bag, or suitcase",
    "wallet": "a photo of a wallet, purse, or clutch",
    "phone": "a photo of a phone, smartphone, or cell phone",
    "laptop": "a photo of a laptop computer",
    "keys": "a photo of keys or keychains",
    "card": "a photo of an identity card, credit card, or driver's license",
    "watch": "a photo of a watch or wristwatch",
    "earphones": "a photo of earphones, headphones, or earbuds",
    "jewelry": "a photo of jewelry, a ring, necklace, or earrings",
    "document": "a photo of documents, papers, or certificates"
}


class EmbeddingService:
    """Orchestrates CLIP encoding, YOLO cropping, vector storage, and matching."""

    def __init__(self):
        self.clip = CLIPOnnxEngine()
        self.yolo = YOLODetector(settings.resolved_yolo_model_path)
        self.store = VectorStore()

        # Precompute category embeddings for zero-shot image classification
        self.category_embeddings = {}
        for cat_name, prompt in CATEGORY_PROMPTS.items():
            self.category_embeddings[cat_name] = self.clip.encode_text(prompt, translate=False)


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

        q_text = None
        if query_image_url:
            img = self._download_image(query_image_url)
            cropped = self.yolo.crop_main_object(img)
            vec = self.clip.encode_image(cropped)
            query_type = "IMAGE"
        elif query_text:
            vec = self.clip.encode_text(query_text, translate=True)
            query_type = "TEXT"
            q_text = query_text
        else:
            return []

        return self._match(vec, target_post_type=target_post_type, query_type=query_type, top_k=top_k, threshold=threshold, query_text=q_text)

    def delete_post_embeddings(self, post_id: int) -> int:
        return self.store.delete_by_post(post_id)

    # Internal methods
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
        # Fetch slightly more than top_k from db to account for duplicate post_ids
        results = self.store.search(query_vec, target_post_type, top_k * 2, threshold)

        query_words = get_normalized_words(query_text) if query_text else set()

        # Determine query categories (zero-shot for image, lexical for text)
        q_cats = set()
        if query_type == "IMAGE":
            best_cat = None
            best_sim = -1.0
            for cat_name, cat_emb in self.category_embeddings.items():
                sim = float(np.dot(query_vec, cat_emb))
                if sim > best_sim:
                    best_sim = sim
                    best_cat = cat_name
            if best_sim > 0.20:
                q_cats.add(best_cat)
        else:
            q_cats = get_categories(query_words)

        best_matches = {}
        for result in results:
            post_id = result["post_id"]
            score = float(result["score"])
            source_type = result.get("source_type", "IMAGE")
            title = result.get("title", "")
            description = result.get("description", "")

            # Determine scaling bounds dynamically depending on modality match
            if query_type == source_type:
                if query_type == "TEXT":
                    # Same modality TEXT-TEXT uses elevated bounds to reduce cone noise
                    zero_b = 50.0
                    min_b = 74.0
                    max_b = 90.0
                else:
                    # Same modality IMAGE-IMAGE uses standard image bounds
                    zero_b = 20.0
                    min_b = 40.0
                    max_b = 85.0
            else:
                # Cross modality (TEXT-IMAGE or IMAGE-TEXT) uses standard CLIP bounds
                zero_b = 10.0
                min_b = settings.clip_score_min # 21.0
                max_b = settings.clip_score_max # 29.0

            raw = score * 100

            # Lexical boosting and category mismatch checks
            has_overlap = False
            post_words = set()
            if query_words and (title or description):
                title_desc = f"{title} {description if description else ''}"
                post_words = get_normalized_words(title_desc)
                overlap = query_words.intersection(post_words)
                if overlap:
                    has_overlap = True
                    # 8% boost per overlapping word
                    raw = raw + (len(overlap) * 8.0)

            is_cat_mismatch = False
            if q_cats and (title or description):
                if not post_words:
                    title_desc = f"{title} {description if description else ''}"
                    post_words = get_normalized_words(title_desc)
                post_cats = get_categories(post_words)
                if post_cats and q_cats.isdisjoint(post_cats):
                    is_cat_mismatch = True
                    raw = raw - 15.0 # Apply raw score category penalty

            # For TEXT-to-TEXT search: penalize and cap if there is absolutely no lexical overlap
            is_text_text_no_overlap = (query_type == "TEXT" and source_type == "TEXT" and query_words and not has_overlap)
            if is_text_text_no_overlap:
                raw = raw - 5.0 # Apply raw score penalty

            if raw >= min_b:
                scaled_score = 50.0 + (raw - min_b) / (max_b - min_b) * 50.0
            else:
                scaled_score = (raw - zero_b) / (min_b - zero_b) * 50.0

            scaled_score = round(max(0.0, min(100.0, scaled_score)), 2)

            if is_text_text_no_overlap or is_cat_mismatch:
                scaled_score = min(30.0, scaled_score) # Cap scaled score at 30%


            if post_id not in best_matches or scaled_score > best_matches[post_id]["scaled_score"]:
                best_matches[post_id] = {
                    "result": result,
                    "score": score,
                    "scaled_score": scaled_score
                }

        final_results = []
        for post_id, info in best_matches.items():
            res = info["result"]
            res["human_score"] = f"{info['scaled_score']:.2f}%"
            res["score"] = round(info["score"], 4)
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
