from contextlib import asynccontextmanager
from typing import Literal

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from services.embedding_service import EmbeddingService


# Globals
svc: EmbeddingService | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global svc
    svc = EmbeddingService()
    yield
    if svc:
        svc.store.close()


app = FastAPI(
    title="CLIP Service - Lost & Found",
    version="1.0.0",
    lifespan=lifespan,
)


# Request / Response models
class EmbedImageRequest(BaseModel):
    post_id: int
    image_url: str
    image_id: int | None = None


class EmbedTextRequest(BaseModel):
    post_id: int
    text: str
    translate: bool = True


class SearchRequest(BaseModel):
    query_text: str | None = None
    query_image_url: str | None = None
    search_in: Literal["IMAGE", "TEXT"] = "IMAGE"
    top_k: int = Field(default=10, gt=0, le=100)
    threshold: float = Field(default=0.5, ge=-1, le=1)


# Routes
@app.post("/api/clip/embed-and-index/image")
async def embed_image(req: EmbedImageRequest):
    """Encode image, store vector, and auto reverse-match lost posts."""
    try:
        result = svc.embed_and_index_image(req.post_id, req.image_url, req.image_id)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/clip/embed-and-index/text")
async def embed_text(req: EmbedTextRequest):
    """Encode text, store vector, and auto forward-match found posts."""
    try:
        result = svc.embed_and_index_text(req.post_id, req.text, req.translate)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/clip/search")
async def search(req: SearchRequest):
    """Manually search by text or image URL."""
    if not req.query_text and not req.query_image_url:
        raise HTTPException(400, "Provide query_text or query_image_url")
    try:
        results = svc.search(
            query_text=req.query_text,
            query_image_url=req.query_image_url,
            search_in=req.search_in,
            top_k=req.top_k,
            threshold=req.threshold,
        )
        return {"results": results}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.delete("/api/clip/embeddings/{post_id}")
async def delete_embeddings(post_id: int):
    """Delete all vectors for a post."""
    count = svc.delete_post_embeddings(post_id)
    return {"status": "deleted", "count": count}


@app.get("/api/clip/health")
async def health():
    """Check service health."""
    return {
        "status": "ok",
        "model": "clip-vit-large-patch14",
        "runtime": "onnx",
    }
