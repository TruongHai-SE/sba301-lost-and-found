import os
import requests
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM, pipeline
from config import settings

class OfflineTranslator:
    """Hybrid translator: Azure API → Google → Helsinki-NLP local fallback."""
    
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
        self._cache = {}

        # Azure Translator config (read from environment variables)
        self._azure_key = os.environ.get("AZURE_TRANSLATOR_KEY")
        self._azure_region = os.environ.get("AZURE_TRANSLATOR_REGION", "koreacentral")
        if self._azure_key:
            print(f"[Translator] Azure Translator enabled (region={self._azure_region})")
        else:
            print("[Translator] Azure Translator not configured, will use Google/local fallback.")
        
        print("[Translator] Offline translator loaded successfully.")

    def _azure_translate(self, text: str) -> str | None:
        """Call Azure Cognitive Services Translator API. Returns translated text or None."""
        try:
            url = "https://api.cognitive.microsofttranslator.com/translate"
            params = {"api-version": "3.0", "from": "vi", "to": "en"}
            headers = {
                "Ocp-Apim-Subscription-Key": self._azure_key,
                "Ocp-Apim-Subscription-Region": self._azure_region,
                "Content-Type": "application/json",
            }
            body = [{"text": text}]
            resp = requests.post(url, params=params, headers=headers, json=body, timeout=5)
            resp.raise_for_status()
            result = resp.json()
            translated = result[0]["translations"][0]["text"]
            if translated and translated.strip():
                return translated
        except Exception as e:
            print(f"[Translator] Azure Translate failed: {e}")
        return None

    def translate(self, text: str) -> str:
        """Translate Vietnamese → English. Priority: Cache → Azure → Google → Helsinki local."""
        if not text or not text.strip():
            return text
        
        # 0. Check cache first
        cache_key = text.strip().lower()
        if cache_key in self._cache:
            cached = self._cache[cache_key]
            print(f"[Translator] Cache hit: '{text}' -> '{cached}'")
            return cached
        
        # 1. Try Azure Translator API (fastest official API, ~100ms)
        if self._azure_key:
            translated = self._azure_translate(text)
            if translated:
                print(f"[Translator] Azure Translate success: '{text}' -> '{translated}'")
                self._cache[cache_key] = translated
                return translated

        # 2. Try GoogleTranslator (online, fallback)
        try:
            from deep_translator import GoogleTranslator
            translated = GoogleTranslator(source='auto', target='en').translate(text)
            if translated and translated.strip():
                print(f"[Translator] Google Translate success: '{text}' -> '{translated}'")
                self._cache[cache_key] = translated
                return translated
        except Exception as e:
            print(f"[Translator] Google Translate failed: {e}. Falling back to local model.")

        # 3. Local fallback model (offline, always works)
        try:
            res = self.pipeline(text)
            translated = res[0]["translation_text"]
            print(f"[Translator] Local Helsinki Translate: '{text}' -> '{translated}'")
            self._cache[cache_key] = translated
            return translated
        except Exception as e:
            print(f"[Translator] Local translation error: {e}")
            return text  # Fallback to original text on failure
