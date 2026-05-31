import os

import numpy as np
import onnxruntime as ort
from deep_translator import GoogleTranslator
from PIL import Image
from transformers import CLIPProcessor

from config import settings


class CLIPOnnxEngine:
    """CLIP ViT-L/14 inference engine using ONNX Runtime directly."""

    def __init__(self, model_dir: str | None = None):
        model_dir = model_dir or settings.resolved_clip_model_dir
        print(f"[CLIP] Loading ONNX model from {model_dir} ...")

        self.processor = CLIPProcessor.from_pretrained(model_dir)

        # The ONNX model handles both text and image input.
        model_path = os.path.join(model_dir, "model.onnx")
        self.session = ort.InferenceSession(
            model_path,
            providers=["CPUExecutionProvider"],
        )

        self._input_names = [inp.name for inp in self.session.get_inputs()]
        self._output_names = [out.name for out in self.session.get_outputs()]
        print(f"[CLIP] Model inputs: {self._input_names}")
        print(f"[CLIP] Model outputs: {self._output_names}")

        self.translator = GoogleTranslator(source="auto", target="en")

        # CLIP requires both inputs, so text-only encoding uses a dummy image.
        self._dummy_image = Image.new("RGB", (224, 224), (0, 0, 0))

        print("[CLIP] Model loaded successfully.")

    # Encode
    def encode_image(self, pil_image) -> np.ndarray:
        """Convert a PIL image to a normalized 768-dimensional vector."""
        inputs = self.processor(
            text=[""], images=pil_image, return_tensors="np", padding=True
        )
        feed = {key: value for key, value in inputs.items() if key in self._input_names}
        outputs = self.session.run(self._output_names, feed)

        image_embeds = self._get_output(outputs, "image_embeds")
        return self._normalize(image_embeds)[0]

    def encode_text(self, text: str, translate: bool = True) -> np.ndarray:
        """Convert text to a normalized 768-dimensional vector."""
        if translate:
            try:
                text = self.translator.translate(text)
                print(f"[CLIP] Translated: {text}")
            except Exception:
                pass

        inputs = self.processor(
            text=[text], images=self._dummy_image, return_tensors="np", padding=True
        )
        feed = {key: value for key, value in inputs.items() if key in self._input_names}
        outputs = self.session.run(self._output_names, feed)

        text_embeds = self._get_output(outputs, "text_embeds")
        return self._normalize(text_embeds)[0]

    # Scoring
    @staticmethod
    def cosine_similarity(vec_a: np.ndarray, vec_b: np.ndarray) -> float:
        """Calculate cosine similarity between normalized vectors."""
        return float(np.dot(vec_a, vec_b))

    def to_human_score(self, cosine_sim: float) -> float:
        """Map the expected CLIP cosine range to a 0-100 score."""
        raw = cosine_sim * 100
        score = (raw - settings.clip_score_min) / (
            settings.clip_score_max - settings.clip_score_min
        ) * 100
        return round(max(0.0, min(100.0, score)), 2)

    # Internal methods
    def _get_output(self, outputs: list, name: str) -> np.ndarray:
        """Get a named output from ONNX session results."""
        for index, out_name in enumerate(self._output_names):
            if name in out_name:
                return np.array(outputs[index])

        # Fallback convention:
        # [logits_per_image, logits_per_text, text_embeds, image_embeds]
        if name == "image_embeds":
            return np.array(outputs[-1])
        if name == "text_embeds":
            return np.array(outputs[-2])
        return np.array(outputs[0])

    @staticmethod
    def _normalize(arr: np.ndarray) -> np.ndarray:
        norms = np.linalg.norm(arr, axis=-1, keepdims=True)
        return arr / norms
