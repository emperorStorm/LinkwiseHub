import base64
import binascii
import json
from typing import Any

from linkwisehub_ai.schemas import ParsedBlock


class MinerUResultNormalizer:
    def normalize(
        self, payload: dict[str, Any]
    ) -> tuple[str, list[ParsedBlock], dict[str, Any], dict[str, bytes]]:
        results = payload.get("results") or {}
        if not results:
            raise ValueError("MinerU result does not contain documents")
        file_name, result = next(iter(results.items()))
        if not isinstance(result, dict):
            raise ValueError("MinerU document result is invalid")
        markdown = str(result.get("md_content") or "").strip()
        raw_blocks = self._decode_blocks(result.get("content_list"))
        blocks = self._normalize_blocks(raw_blocks)
        if not blocks and markdown:
            blocks = [ParsedBlock(index=1, block_type="text", content=markdown)]
        if not markdown and blocks:
            markdown = "\n\n".join(block.content for block in blocks)
        if not markdown:
            raise ValueError("MinerU result is empty")
        assets = self._decode_assets(result.get("images"))
        manifest = {
            "schema_version": "1.0",
            "source_file": file_name,
            "block_count": len(blocks),
            "has_markdown": True,
            "mineru_backend": payload.get("backend"),
            "asset_count": len(assets),
        }
        return markdown, blocks, manifest, assets

    def _decode_assets(self, value: Any) -> dict[str, bytes]:
        if not isinstance(value, dict):
            return {}
        assets: dict[str, bytes] = {}
        for file_name, data_uri in value.items():
            if not isinstance(file_name, str) or not isinstance(data_uri, str) or "," not in data_uri:
                continue
            try:
                assets[file_name] = base64.b64decode(data_uri.split(",", 1)[1], validate=True)
            except (ValueError, binascii.Error):
                continue
        return assets

    def _decode_blocks(self, value: Any) -> list[dict[str, Any]]:
        if isinstance(value, str):
            try:
                value = json.loads(value)
            except json.JSONDecodeError:
                return []
        return [item for item in value if isinstance(item, dict)] if isinstance(value, list) else []

    def _normalize_blocks(self, values: list[dict[str, Any]]) -> list[ParsedBlock]:
        blocks: list[ParsedBlock] = []
        for value in values:
            content = self._resolve_content(value)
            if not content:
                continue
            page = self._resolve_page(value)
            block_type = str(value.get("type") or value.get("block_type") or "text")
            title = value.get("title") or value.get("heading")
            metadata = {
                key: item
                for key, item in value.items()
                if key not in {"text", "content", "table_body", "table_content", "latex", "page_idx"}
            }
            blocks.append(
                ParsedBlock(
                    index=len(blocks) + 1,
                    block_type=block_type,
                    content=content,
                    page=page,
                    title=str(title) if title else None,
                    metadata=metadata,
                )
            )
        return blocks

    def _resolve_content(self, value: dict[str, Any]) -> str:
        for key in ("text", "content", "table_body", "table_content", "latex", "image_caption"):
            content = value.get(key)
            if content is not None and str(content).strip():
                return str(content).strip()
        return ""

    def _resolve_page(self, value: dict[str, Any]) -> int | None:
        page = value.get("page_idx", value.get("page"))
        if isinstance(page, int):
            return page + 1 if "page_idx" in value else page
        return None
