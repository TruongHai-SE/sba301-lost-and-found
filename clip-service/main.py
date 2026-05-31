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
    post_type: Literal["LOST", "FOUND"] = Field(
        ..., description="The type of the post being embedded (LOST or FOUND). Used to auto-match against the opposite type."
    )


class EmbedTextRequest(BaseModel):
    post_id: int
    text: str
    translate: bool = True
    post_type: Literal["LOST", "FOUND"] = Field(
        ..., description="The type of the post being embedded (LOST or FOUND). Used to auto-match against the opposite type."
    )


class SearchRequest(BaseModel):
    query_text: str | None = None
    query_image_url: str | None = None
    target_post_type: Literal["LOST", "FOUND", "ALL"] = Field(
        default="ALL", description="Filter results by post type. Use 'LOST' to search lost items, 'FOUND' for found items, or 'ALL' for both."
    )
    top_k: int = Field(default=10, gt=0, le=100)
    threshold: float = Field(default=0.5, ge=-1, le=1)


# Routes
@app.post("/api/clip/embed-and-index/image")
async def embed_image(req: EmbedImageRequest):
    """Encode image, store vector, and auto cross-match with opposite post type."""
    try:
        result = svc.embed_and_index_image(req.post_id, req.image_url, req.post_type, req.image_id)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/clip/embed-and-index/text")
async def embed_text(req: EmbedTextRequest):
    """Encode text, store vector, and auto cross-match with opposite post type."""
    try:
        result = svc.embed_and_index_text(req.post_id, req.text, req.post_type, req.translate)
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
            target_post_type=req.target_post_type,
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
