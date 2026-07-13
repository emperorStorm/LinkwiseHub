import json
from io import BytesIO
from pathlib import Path
from typing import Any

from minio import Minio

from linkwisehub_ai.schemas import MaterializedArtifacts, ParsedBlock, SourceObject


class ArtifactStore:
    def __init__(self, client: Minio, result_bucket: str) -> None:
        self.client = client
        self.result_bucket = result_bucket

    def download_source(self, source: SourceObject, destination: Path) -> None:
        self.client.fget_object(source.bucket, source.object_key, str(destination))

    def materialize(
        self,
        document_id: int,
        task_id: str,
        markdown: str,
        blocks: list[ParsedBlock],
        manifest: dict[str, Any],
        assets: dict[str, bytes],
    ) -> MaterializedArtifacts:
        self._ensure_result_bucket()
        prefix = f"parsed/{document_id}/{task_id}"
        markdown_object = f"{prefix}/content.md"
        blocks_object = f"{prefix}/blocks.json"
        manifest_object = f"{prefix}/manifest.json"
        self._put_text(markdown_object, markdown, "text/markdown; charset=utf-8")
        self._put_json(blocks_object, [block.model_dump(mode="json") for block in blocks])
        self._put_json(manifest_object, manifest)
        for file_name, content in assets.items():
            self._put_bytes(f"{prefix}/images/{file_name}", content, self._asset_content_type(file_name))
        return MaterializedArtifacts(
            bucket=self.result_bucket,
            manifest_object=manifest_object,
            markdown_object=markdown_object,
            blocks_object=blocks_object,
            asset_prefix=f"{prefix}/images/",
            block_count=len(blocks),
        )

    def ready(self) -> bool:
        try:
            self._ensure_result_bucket()
            return True
        except Exception:
            return False

    def _ensure_result_bucket(self) -> None:
        if not self.client.bucket_exists(self.result_bucket):
            self.client.make_bucket(self.result_bucket)

    def _put_json(self, object_name: str, value: Any) -> None:
        content = json.dumps(value, ensure_ascii=False, separators=(",", ":")).encode()
        self._put_bytes(object_name, content, "application/json")

    def _put_text(self, object_name: str, value: str, content_type: str) -> None:
        self._put_bytes(object_name, value.encode(), content_type)

    def _put_bytes(self, object_name: str, content: bytes, content_type: str) -> None:
        self.client.put_object(
            self.result_bucket,
            object_name,
            BytesIO(content),
            length=len(content),
            content_type=content_type,
        )

    def _asset_content_type(self, file_name: str) -> str:
        suffix = Path(file_name).suffix.lower()
        return {
            ".png": "image/png",
            ".jpg": "image/jpeg",
            ".jpeg": "image/jpeg",
            ".webp": "image/webp",
            ".gif": "image/gif",
        }.get(suffix, "application/octet-stream")
