from datetime import datetime
from pathlib import Path
from typing import Any

import httpx

from linkwisehub_ai.schemas import DocumentParseTask, ParseOptions


class MinerUError(RuntimeError):
    pass


class MinerUClient:
    STATUS_MAP = {
        "pending": "PENDING",
        "processing": "RUNNING",
        "completed": "SUCCESS",
        "failed": "FAILED",
    }

    def __init__(self, client: httpx.AsyncClient, result_timeout_seconds: float) -> None:
        self.client = client
        self.result_timeout_seconds = result_timeout_seconds

    async def submit(self, file_path: Path, file_name: str, options: ParseOptions) -> DocumentParseTask:
        form = {
            "backend": options.backend,
            "parse_method": options.parse_method,
            "lang_list": options.language,
            "formula_enable": str(options.formula_enabled).lower(),
            "table_enable": str(options.table_enabled).lower(),
            "return_md": "true",
            "return_content_list": "true",
            "return_middle_json": "false",
            "return_model_output": "false",
            "return_images": "true",
            "response_format_zip": "false",
        }
        with file_path.open("rb") as source:
            response = await self.client.post("/tasks", data=form, files={"files": (file_name, source)})
        return self._parse_task_response(response)

    async def get_status(self, task_id: str) -> DocumentParseTask:
        response = await self.client.get(f"/tasks/{task_id}")
        return self._parse_task_response(response)

    async def get_result(self, task_id: str) -> dict[str, Any]:
        response = await self.client.get(
            f"/tasks/{task_id}/result",
            timeout=self.result_timeout_seconds,
        )
        if response.status_code == 202:
            raise MinerUError("MinerU task is not completed")
        self._raise_for_status(response)
        payload = response.json()
        if not isinstance(payload, dict) or not isinstance(payload.get("results"), dict):
            raise MinerUError("MinerU returned an invalid result payload")
        return payload

    async def health(self) -> bool:
        try:
            response = await self.client.get("/health", timeout=10)
            return response.is_success
        except httpx.HTTPError:
            return False

    def _parse_task_response(self, response: httpx.Response) -> DocumentParseTask:
        self._raise_for_status(response)
        payload = response.json()
        task_id = payload.get("task_id")
        raw_status = str(payload.get("status", "")).lower()
        if not task_id or raw_status not in self.STATUS_MAP:
            raise MinerUError("MinerU returned an invalid task payload")
        return DocumentParseTask(
            task_id=task_id,
            status=self.STATUS_MAP[raw_status],
            progress=self._resolve_progress(raw_status, payload.get("queued_ahead")),
            error_message=payload.get("error"),
            created_at=self._parse_datetime(payload.get("created_at")),
            started_at=self._parse_datetime(payload.get("started_at")),
            completed_at=self._parse_datetime(payload.get("completed_at")),
        )

    def _raise_for_status(self, response: httpx.Response) -> None:
        try:
            response.raise_for_status()
        except httpx.HTTPStatusError as exc:
            detail = response.text[:1000]
            raise MinerUError(f"MinerU request failed ({response.status_code}): {detail}") from exc

    def _resolve_progress(self, status: str, queued_ahead: Any) -> int:
        if status == "completed":
            return 100
        if status == "processing":
            return 50
        if status == "failed":
            return 0
        return 1 if queued_ahead is None else 0

    def _parse_datetime(self, value: Any) -> datetime | None:
        if not value or not isinstance(value, str):
            return None
        try:
            return datetime.fromisoformat(value.replace("Z", "+00:00"))
        except ValueError:
            return None
