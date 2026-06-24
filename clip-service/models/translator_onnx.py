import os
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM, pipeline
from config import settings

class OfflineTranslator:
    """Helsinki-NLP/opus-mt-vi-en translation running locally via PyTorch on CPU."""
    
    def __init__(self, model_dir: str | None = None):
        model_dir = model_dir or settings.resolved_translation_model_dir
        print(f"[Translator] Loading offline translator from {model_dir} ...")
        
        self.tokenizer = AutoTokenizer.from_pretrained(model_dir)
        self.model = AutoModelForSeq2SeqLM.from_pretrained(model_dir)
        self.pipeline = pipeline(
            "translation",
            model=self.model,
            tokenizer=self.tokenizer,
            device="cpu"
        )
        print("[Translator] Offline translator loaded successfully.")

    def translate(self, text: str) -> str:
        """Translate Vietnamese input to English."""
        if not text or not text.strip():
            return text
        try:
            res = self.pipeline(text)
            translated = res[0]["translation_text"]
            return translated
        except Exception as e:
            print(f"[Translator] Translation error: {e}")
            return text # Fallback to original text on failure
