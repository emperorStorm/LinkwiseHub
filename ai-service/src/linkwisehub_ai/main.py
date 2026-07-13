import logging
import uuid
from contextlib import asynccontextmanager

import httpx
from fastapi import FastAPI
from minio import Minio

from linkwisehub_ai.adapters.mineru import MinerUClient
from linkwisehub_ai.adapters.storage import ArtifactStore
from linkwisehub_ai.api.routes import router
from linkwisehub_ai.config import get_settings
from linkwisehub_ai.services.document_parse import DocumentParseService
from linkwisehub_ai.services.normalizer import MinerUResultNormalizer


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = get_settings()
    http_client = httpx.AsyncClient(
        base_url=settings.mineru_base_url.rstrip("/"),
        timeout=settings.mineru_request_timeout_seconds,
    )
    minio_client = Minio(
        settings.minio_endpoint,
        access_key=settings.minio_access_key,
        secret_key=settings.minio_secret_key,
        secure=settings.minio_secure,
    )
    app.state.settings = settings
    app.state.document_parse_service = DocumentParseService(
        MinerUClient(http_client, settings.mineru_result_timeout_seconds),
        ArtifactStore(minio_client, settings.minio_result_bucket),
        MinerUResultNormalizer(),
    )
    try:
        yield
    finally:
        await http_client.aclose()


app = FastAPI(
    title="LinkwiseHub AI Service",
    version="0.1.0",
    docs_url=None,
    redoc_url=None,
    openapi_url=None,
    lifespan=lifespan,
)
app.include_router(router)

logger = logging.getLogger("linkwisehub_ai.requests")


@app.middleware("http")
async def request_context(request, call_next):
    request_id = request.headers.get("X-Request-Id") or str(uuid.uuid4())
    request.state.request_id = request_id
    logger.info("AI request started request_id=%s method=%s path=%s", request_id, request.method, request.url.path)
    response = await call_next(request)
    response.headers["X-Request-Id"] = request_id
    logger.info("AI request finished request_id=%s status=%s", request_id, response.status_code)
    return response
