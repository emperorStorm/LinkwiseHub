from types import SimpleNamespace

from fastapi import FastAPI
from fastapi.testclient import TestClient

from linkwisehub_ai.api.routes import router
from linkwisehub_ai.schemas import DocumentParseTask


class FakeDocumentParseService:
    async def submit(self, payload):
        return DocumentParseTask(task_id="task-1", status="PENDING", progress=0)


def create_client() -> TestClient:
    app = FastAPI()
    app.state.settings = SimpleNamespace(ai_service_token="secret")
    app.state.document_parse_service = FakeDocumentParseService()
    app.include_router(router)
    return TestClient(app)


def test_internal_endpoint_requires_service_token() -> None:
    response = create_client().post(
        "/internal/v1/document-parses",
        json={
            "document_id": 1,
            "source": {
                "bucket": "knowledge-base",
                "object_key": "documents/sample.pdf",
                "file_name": "sample.pdf",
                "file_type": "pdf",
            },
        },
    )

    assert response.status_code == 401


def test_internal_endpoint_checks_token_before_body_validation() -> None:
    response = create_client().post("/internal/v1/document-parses", json={})

    assert response.status_code == 401


def test_submit_returns_normalized_task() -> None:
    response = create_client().post(
        "/internal/v1/document-parses",
        headers={"X-Service-Token": "secret", "X-Request-Id": "request-1"},
        json={
            "document_id": 1,
            "source": {
                "bucket": "knowledge-base",
                "object_key": "documents/sample.pdf",
                "file_name": "sample.pdf",
                "file_type": "pdf",
            },
        },
    )

    assert response.status_code == 202
    assert response.json()["task_id"] == "task-1"
