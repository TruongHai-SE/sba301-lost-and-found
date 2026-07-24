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
    import os
    import logging
    dbg_logger = logging.getLogger("uvicorn.error")
    dbg_logger.info("====== [DEBUG] Listing files in /app/.local ======")
    if os.path.exists("/app/.local"):
        for root, dirs, files in os.walk("/app/.local"):
            dbg_logger.info(f"Dir: {root}")
            for f in files:
                dbg_logger.info(f"  File: {f}")
    else:
         dbg_logger.info("/app/.local directory does not exist!")
    dbg_logger.info("====== [DEBUG] End of file listing ======")

    svc = EmbeddingService()
    # Warm-up models to eliminate cold start latency for the first request
    try:
        logger.info("[Lifespan] Warming up models (CLIP & Translator)...")
        # Warm up CLIP with English text (Fully offline, avoids network calls)
        svc.clip.encode_text("tea set", translate=False)
        # Warm up Helsinki local translator offline
        if getattr(svc, "translator", None):
            svc.translator._local_translate("ấm chén trà")
        logger.info("[Lifespan] Models warmed up successfully!")
    except Exception as e:
        logger.error(f"[Lifespan] Failed to warm up models: {e}")
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



from fastapi.security import APIKeyHeader
from fastapi import Depends
from config import settings

API_KEY_NAME = "X-API-Key"
api_key_header = APIKeyHeader(name=API_KEY_NAME, auto_error=False)

async def get_api_key(header_value: str = Depends(api_key_header)):
    # Bỏ qua xác thực nếu token mặc định chưa được thay đổi (ví dụ ở local)
    if settings.clip_api_token == "your-secure-api-token-here":
        return header_value
    if not header_value or header_value != settings.clip_api_token:
        raise HTTPException(
            status_code=403,
            detail="Could not validate credentials. Invalid or missing X-API-Key."
        )
    return header_value


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
    threshold: float | None = Field(default=None, ge=-1, le=1)


# Routes
@app.post("/api/v1/embeddings/image", dependencies=[Depends(get_api_key)])
async def embed_image(req: EmbedImageRequest):
    """Encode image, store vector, and auto cross-match with opposite post type."""
    try:
        result = svc.embed_and_index_image(req.post_id, req.image_url, req.post_type, req.image_id)
        return result
    except Exception as e:
        logger.error(f"Error in embed_image: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/v1/embeddings/text", dependencies=[Depends(get_api_key)])
async def embed_text(req: EmbedTextRequest):
    """Encode text, store vector, and auto cross-match with opposite post type."""
    try:
        result = svc.embed_and_index_text(req.post_id, req.text, req.post_type, req.translate)
        return result
    except Exception as e:
        logger.error(f"Error in embed_text: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/v1/search", dependencies=[Depends(get_api_key)])
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


from fastapi import UploadFile, File, Form

@app.post("/api/v1/validate-image", dependencies=[Depends(get_api_key)])
async def validate_image(
    image: UploadFile | None = File(None),
    image_url: str | None = Form(None),
    title: str | None = Form(None),
):
    """Validate image against zero-shot junk prompts and check Title-Image consistency."""
    try:
        img_bytes = await image.read() if image else None
        res = svc.validate_image_and_consistency(
            image_bytes=img_bytes,
            image_url=image_url,
            title=title,
        )
        return res
    except Exception as e:
        logger.error(f"Error in validate_image: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/v1/search/image-bytes", dependencies=[Depends(get_api_key)])
async def search_by_image_bytes(
    image: UploadFile = File(...),
    target_post_type: Literal["LOST", "FOUND", "ALL"] = Form("ALL"),
    top_k: int = Form(10),
    threshold: float | None = Form(None),
):
    """Directly search by raw image bytes via RAM processing."""
    try:
        img_bytes = await image.read()
        results = svc.search_by_image_bytes(
            image_bytes=img_bytes,
            target_post_type=target_post_type,
            top_k=top_k,
            threshold=threshold,
        )
        return {"results": results, "matches": results}
    except Exception as e:
        logger.error(f"Error in search_by_image_bytes: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.delete("/api/v1/embeddings/posts/{post_id}", dependencies=[Depends(get_api_key)])
async def delete_embeddings(post_id: int):
    """Delete all vectors for a post."""
    count = svc.delete_post_embeddings(post_id)
    return {"status": "deleted", "count": count}


@app.api_route("/", methods=["GET", "HEAD"])
async def root():
    """Root endpoint to pass Hugging Face health check and readiness probe."""
    return {
        "status": "ok",
        "message": "CLIP Service is running",
        "health_check_url": "/api/v1/health"
    }


@app.api_route("/api/v1/health", methods=["GET", "HEAD"])
async def health():
    """Check service health."""
    return {
        "status": "ok",
        "model": "clip-vit-large-patch14",
        "runtime": "onnx",
    }
