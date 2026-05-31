from pathlib import Path

from dotenv import load_dotenv
from pydantic_settings import BaseSettings


# Load .env from the repository root.
REPO_ROOT = Path(__file__).resolve().parent.parent
load_dotenv(REPO_ROOT / ".env")


class Settings(BaseSettings):
    # PostgreSQL
    postgres_host: str = "localhost"
    postgres_port: int = 5433
    postgres_db: str = "lostfound"
    postgres_user: str = "postgres"
    postgres_password: str

    # CLIP
    clip_model_dir: str = ".local/models/clip_onnx_large_model"
    yolo_model_path: str = ".local/models/yolov8n.pt"
    clip_score_min: float = 21.0
    clip_score_max: float = 29.0
    clip_match_threshold: float = 0.5

    @property
    def database_url(self) -> str:
        return (
            f"postgresql://{self.postgres_user}:{self.postgres_password}"
            f"@{self.postgres_host}:{self.postgres_port}/{self.postgres_db}"
        )

    @property
    def resolved_clip_model_dir(self) -> str:
        return self._resolve_repo_path(self.clip_model_dir)

    @property
    def resolved_yolo_model_path(self) -> str:
        return self._resolve_repo_path(self.yolo_model_path)

    @staticmethod
    def _resolve_repo_path(path: str) -> str:
        candidate = Path(path)
        if not candidate.is_absolute():
            candidate = REPO_ROOT / candidate
        return str(candidate.resolve())

    class Config:
        env_file = "../.env"
        extra = "ignore"


settings = Settings()
