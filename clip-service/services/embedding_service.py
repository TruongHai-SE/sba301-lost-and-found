import io

import numpy as np
import requests
from PIL import Image

from config import settings
from models.clip_onnx import CLIPOnnxEngine
from models.yolo_detector import YOLODetector
from services.vector_store import VectorStore


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

        # Cross match: LOST searches FOUND, and FOUND searches LOST
        target = "FOUND" if post_type == "LOST" else "LOST"
        matches = self._match(embedding, target_post_type=target)

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
        matches = self._match(embedding, target_post_type=target)

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

        if query_image_url:
            img = self._download_image(query_image_url)
            cropped = self.yolo.crop_main_object(img)
            vec = self.clip.encode_image(cropped)
        elif query_text:
            vec = self.clip.encode_text(query_text, translate=True)
        else:
            return []

        return self._match(vec, target_post_type=target_post_type, top_k=top_k, threshold=threshold)

    def delete_post_embeddings(self, post_id: int) -> int:
        return self.store.delete_by_post(post_id)

    # Internal methods
    def _match(
        self,
        query_vec: np.ndarray,
        target_post_type: str,
        top_k: int = 10,
        threshold: float | None = None,
    ) -> list[dict]:
        threshold = (
            settings.clip_match_threshold if threshold is None else threshold
        )
        results = self.store.search(query_vec, target_post_type, top_k, threshold)

        for result in results:
            score = float(result["score"])
            result["human_score"] = f"{self.clip.to_human_score(score):.2f}%"
            result["score"] = round(score, 4)
            result.pop("source_type", None)

        return results

    @staticmethod
    def _download_image(url: str) -> Image.Image:
        headers = {"User-Agent": "Mozilla/5.0"}
        response = requests.get(url, headers=headers, timeout=15)
        response.raise_for_status()
        return Image.open(io.BytesIO(response.content)).convert("RGB")
