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


from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
import logging

logger = logging.getLogger("uvicorn.error")

app = FastAPI(
    title="CLIP Service - Lost & Found",
    version="1.0.0",
    lifespan=lifespan,
)


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request, exc):
    body = await request.body()
    logger.error(f"Validation error: {exc.errors()}")
    logger.error(f"Request body: {body.decode('utf-8', errors='ignore')}")
    return JSONResponse(
        status_code=422,
        content={"detail": exc.errors(), "body": body.decode('utf-8', errors='ignore')},
    )


@app.exception_handler(Exception)
async def global_exception_handler(request, exc):
    import traceback
    logger.error(f"Global error: {str(exc)}")
    logger.error(traceback.format_exc())
    return JSONResponse(
        status_code=500,
        content={"detail": str(exc), "traceback": traceback.format_exc()},
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
@app.post("/api/v1/embeddings/image")
async def embed_image(req: EmbedImageRequest):
    """Encode image, store vector, and auto cross-match with opposite post type."""
    try:
        result = svc.embed_and_index_image(req.post_id, req.image_url, req.post_type, req.image_id)
        return result
    except Exception as e:
        logger.error(f"Error in embed_image: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/v1/embeddings/text")
async def embed_text(req: EmbedTextRequest):
    """Encode text, store vector, and auto cross-match with opposite post type."""
    try:
        result = svc.embed_and_index_text(req.post_id, req.text, req.post_type, req.translate)
        return result
    except Exception as e:
        logger.error(f"Error in embed_text: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/v1/search")
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
        return {"results": results, "matches": results}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.delete("/api/v1/embeddings/posts/{post_id}")
async def delete_embeddings(post_id: int):
    """Delete all vectors for a post."""
    count = svc.delete_post_embeddings(post_id)
    return {"status": "deleted", "count": count}


@app.get("/api/v1/health")
async def health():
    """Check service health."""
    return {
        "status": "ok",
        "model": "clip-vit-large-patch14",
        "runtime": "onnx",
    }
