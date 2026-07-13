import asyncio
import tempfile
from pathlib import Path

from linkwisehub_ai.adapters.mineru import MinerUClient
from linkwisehub_ai.adapters.storage import ArtifactStore
from linkwisehub_ai.schemas import (
    DocumentParseRequest,
    DocumentParseTask,
    MaterializeResponse,
)
from linkwisehub_ai.services.normalizer import MinerUResultNormalizer


class DocumentParseService:
    def __init__(
        self,
        mineru: MinerUClient,
        storage: ArtifactStore,
        normalizer: MinerUResultNormalizer,
    ) -> None:
        self.mineru = mineru
        self.storage = storage
        self.normalizer = normalizer

    async def submit(self, request: DocumentParseRequest) -> DocumentParseTask:
        suffix = Path(request.source.file_name).suffix
        with tempfile.TemporaryDirectory(prefix="linkwisehub-ai-") as temp_dir:
            source_path = Path(temp_dir) / f"source{suffix}"
            await asyncio.to_thread(self.storage.download_source, request.source, source_path)
            return await self.mineru.submit(source_path, request.source.file_name, request.options)

    async def get_status(self, task_id: str) -> DocumentParseTask:
        return await self.mineru.get_status(task_id)

    async def materialize(self, task_id: str, document_id: int) -> MaterializeResponse:
        payload = await self.mineru.get_result(task_id)
        markdown, blocks, manifest, assets = self.normalizer.normalize(payload)
        manifest.update({"document_id": document_id, "task_id": task_id})
        artifacts = await asyncio.to_thread(
            self.storage.materialize,
            document_id,
            task_id,
            markdown,
            blocks,
            manifest,
            assets,
        )
        return MaterializeResponse(task_id=task_id, status="SUCCESS", artifacts=artifacts)

    async def ready(self) -> bool:
        mineru_ready, storage_ready = await asyncio.gather(
            self.mineru.health(),
            asyncio.to_thread(self.storage.ready),
        )
        return mineru_ready and storage_ready
