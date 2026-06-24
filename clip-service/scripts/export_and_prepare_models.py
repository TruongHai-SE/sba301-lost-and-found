import os
import sys
from pathlib import Path
import subprocess
from urllib.request import urlretrieve

REPO_ROOT = Path(__file__).resolve().parent.parent
MODEL_ROOT = REPO_ROOT / ".local" / "models"
CLIP_DIR = MODEL_ROOT / "clip_onnx_large_model"
YOLO_PATH = MODEL_ROOT / "yolov8n.pt"
TRANS_DIR = MODEL_ROOT / "opus_mt_vi_en_onnx"

CLIP_REPO = "openai/clip-vit-large-patch14"
YOLO_URL = "https://github.com/ultralytics/assets/releases/download/v8.3.0/yolov8n.pt"
TRANS_REPO = "Helsinki-NLP/opus-mt-vi-en"

def run_cmd(cmd):
    print(f"Running: {' '.join(cmd)}")
    result = subprocess.run(cmd)
    if result.returncode != 0:
        raise RuntimeError(f"Command failed: {' '.join(cmd)}")

def prepare_all():
    MODEL_ROOT.mkdir(parents=True, exist_ok=True)

    # 1. Download pre-quantized CLIP from Hugging Face Hub directly
    if not (CLIP_DIR / "model_quantized.onnx").exists():
        print("[1/3] Downloading pre-quantized CLIP model from Xenova/clip-vit-large-patch14 on HF Hub...")
        CLIP_DIR.mkdir(parents=True, exist_ok=True)
        import shutil
        from huggingface_hub import hf_hub_download
        
        files_to_download = [
            ("config.json", "config.json"),
            ("preprocessor_config.json", "preprocessor_config.json"),
            ("tokenizer_config.json", "tokenizer_config.json"),
            ("tokenizer.json", "tokenizer.json"),
            ("vocab.json", "vocab.json"),
            ("merges.txt", "merges.txt"),
            ("special_tokens_map.json", "special_tokens_map.json"),
            ("onnx/model_quantized.onnx", "model_quantized.onnx")
        ]
        
        for src_name, dst_name in files_to_download:
            print(f"Downloading {src_name}...")
            cached_path = hf_hub_download(repo_id="Xenova/clip-vit-large-patch14", filename=src_name)
            shutil.copy(cached_path, CLIP_DIR / dst_name)
    else:
        print("[1/3] Quantized CLIP already exists; skipping.")

    # 2. Download and Save Translator (PyTorch CPU)
    if not TRANS_DIR.exists() or not (
        (TRANS_DIR / "pytorch_model.bin").exists() or 
        (TRANS_DIR / "model.safetensors").exists()
    ):
        print("[2/3] Downloading and saving Helsinki Translator...")
        TRANS_DIR.mkdir(parents=True, exist_ok=True)
        from transformers import AutoTokenizer, AutoModelForSeq2SeqLM
        tokenizer = AutoTokenizer.from_pretrained(TRANS_REPO)
        model = AutoModelForSeq2SeqLM.from_pretrained(TRANS_REPO)
        tokenizer.save_pretrained(str(TRANS_DIR))
        model.save_pretrained(str(TRANS_DIR))
        print(f"Translator files saved in {TRANS_DIR}: {os.listdir(TRANS_DIR)}")
    else:
        print("[2/3] Translator already exists; skipping.")

    # 3. Download YOLO
    if not YOLO_PATH.exists():
        print("[3/3] Downloading YOLOv8 Nano...")
        urlretrieve(YOLO_URL, YOLO_PATH)
    else:
        print("[3/3] YOLOv8 Nano weights already exist; skipping.")

if __name__ == "__main__":
    prepare_all()
