from pathlib import Path
import shutil
from urllib.request import urlretrieve

from huggingface_hub import snapshot_download


REPO_ROOT = Path(__file__).resolve().parent.parent
MODEL_ROOT = REPO_ROOT / ".local" / "models"
CLIP_DIR = MODEL_ROOT / "clip_onnx_large_model"
YOLO_PATH = MODEL_ROOT / "yolov8n.pt"

CLIP_REPO = "openai/clip-vit-large-patch14"
YOLO_URL = (
    "https://github.com/ultralytics/assets/releases/download/v8.3.0/yolov8n.pt"
)
CLIP_FILES = [
    "config.json",
    "merges.txt",
    "onnx/model.onnx",
    "preprocessor_config.json",
    "special_tokens_map.json",
    "tokenizer.json",
    "tokenizer_config.json",
    "vocab.json",
]


def download_clip() -> None:
    target_files = [CLIP_DIR / Path(relative_path).name for relative_path in CLIP_FILES]
    if all(target.exists() for target in target_files):
        print("[1/2] CLIP ONNX model already exists; skipping.")
        return

    print("[1/2] Downloading CLIP ONNX model from Hugging Face...")
    snapshot_dir = Path(
        snapshot_download(repo_id=CLIP_REPO, allow_patterns=CLIP_FILES)
    )
    CLIP_DIR.mkdir(parents=True, exist_ok=True)

    for relative_path in CLIP_FILES:
        source = snapshot_dir / relative_path
        target = CLIP_DIR / Path(relative_path).name
        if not target.exists():
            shutil.copy2(source, target)


def download_yolo() -> None:
    if YOLO_PATH.exists():
        print("[2/2] YOLOv8 Nano weights already exist; skipping.")
        return

    print("[2/2] Downloading YOLOv8 Nano weights from Ultralytics...")
    MODEL_ROOT.mkdir(parents=True, exist_ok=True)
    urlretrieve(YOLO_URL, YOLO_PATH)


if __name__ == "__main__":
    download_clip()
    download_yolo()
    print(f"Models are ready in {MODEL_ROOT}")
