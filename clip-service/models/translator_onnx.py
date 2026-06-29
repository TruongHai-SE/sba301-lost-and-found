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
        """Translate Vietnamese input to English using GoogleTranslator with local fallback."""
        if not text or not text.strip():
            return text
        
        # 1. Try GoogleTranslator (online)
        try:
            from deep_translator import GoogleTranslator
            translated = GoogleTranslator(source='auto', target='en').translate(text)
            if translated and translated.strip():
                print(f"[Translator] Google Translate success: '{text}' -> '{translated}'")
                return translated
        except Exception as e:
            print(f"[Translator] Google Translate failed: {e}. Falling back to local model.")

        # 2. Local fallback model
        try:
            res = self.pipeline(text)
            translated = res[0]["translation_text"]
            print(f"[Translator] Local Helsinki Translate: '{text}' -> '{translated}'")
            return translated
        except Exception as e:
            print(f"[Translator] Local translation error: {e}")
            return text # Fallback to original text on failure
