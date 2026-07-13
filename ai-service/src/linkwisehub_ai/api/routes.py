import hmac
import uuid

from fastapi import APIRouter, Depends, Header, HTTPException, Request, status

from linkwisehub_ai.schemas import (
    DocumentParseRequest,
    DocumentParseTask,
    MaterializeRequest,
    MaterializeResponse,
)

router = APIRouter()


def require_service_token(request: Request, x_service_token: str | None = Header(default=None)) -> None:
    configured_token = request.app.state.settings.ai_service_token
    if not configured_token or not x_service_token or not hmac.compare_digest(configured_token, x_service_token):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid service token")


@router.post(
    "/internal/v1/document-parses",
    response_model=DocumentParseTask,
    status_code=status.HTTP_202_ACCEPTED,
    dependencies=[Depends(require_service_token)],
)
async def submit_document_parse(
    payload: DocumentParseRequest,
    request: Request,
    x_request_id: str | None = Header(default=None),
) -> DocumentParseTask:
    request.state.request_id = x_request_id or str(uuid.uuid4())
    return await request.app.state.document_parse_service.submit(payload)


@router.get(
    "/internal/v1/document-parses/{task_id}",
    response_model=DocumentParseTask,
    dependencies=[Depends(require_service_token)],
)
async def get_document_parse(
    task_id: str,
    request: Request,
) -> DocumentParseTask:
    return await request.app.state.document_parse_service.get_status(task_id)


@router.post(
    "/internal/v1/document-parses/{task_id}/materialize",
    response_model=MaterializeResponse,
    dependencies=[Depends(require_service_token)],
)
async def materialize_document_parse(
    task_id: str,
    payload: MaterializeRequest,
    request: Request,
) -> MaterializeResponse:
    return await request.app.state.document_parse_service.materialize(task_id, payload.document_id)


@router.get("/health/live")
async def live() -> dict[str, str]:
    return {"status": "UP"}


@router.get("/health/ready")
async def ready(request: Request) -> dict[str, str]:
    missing = request.app.state.settings.missing_required_values()
    if missing or not await request.app.state.document_parse_service.ready():
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail={"status": "DOWN", "missing": missing},
        )
    return {"status": "UP"}
