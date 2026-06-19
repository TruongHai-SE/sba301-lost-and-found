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
    target_onnx = CLIP_DIR / "model.onnx"
    if target_onnx.exists():
        print("[1/2] CLIP ONNX model already exists; skipping.")
        return

    print("[1/2] Exporting CLIP model to ONNX using Optimum...")
    CLIP_DIR.mkdir(parents=True, exist_ok=True)

    import subprocess
    import sys

    python_exe = sys.executable
    optimum_cli_script = Path(python_exe).parent / "optimum-cli.exe"

    if optimum_cli_script.exists():
        cmd = [
            str(optimum_cli_script),
            "export",
            "onnx",
            "--model",
            CLIP_REPO,
            "--task",
            "zero-shot-image-classification",
            str(CLIP_DIR)
        ]
    else:
        cmd = [
            python_exe,
            "-m",
            "optimum.exporters.onnx",
            "--model",
            CLIP_REPO,
            "--task",
            "zero-shot-image-classification",
            str(CLIP_DIR)
        ]

    print(f"Running command: {' '.join(cmd)}")
    result = subprocess.run(cmd)
    if result.returncode != 0:
        raise RuntimeError("Optimum export failed!")


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
