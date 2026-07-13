import base64

import pytest

from linkwisehub_ai.services.normalizer import MinerUResultNormalizer


def test_normalize_markdown_and_content_list() -> None:
    payload = {
        "backend": "pipeline",
        "results": {
            "sample.pdf": {
                "md_content": "# 标题\n\n正文",
                "content_list": [
                    {"type": "title", "text": "标题", "page_idx": 0},
                    {"type": "text", "text": "正文", "page_idx": 1},
                ],
            }
        },
    }

    markdown, blocks, manifest, assets = MinerUResultNormalizer().normalize(payload)

    assert markdown == "# 标题\n\n正文"
    assert [block.page for block in blocks] == [1, 2]
    assert manifest["source_file"] == "sample.pdf"
    assert manifest["block_count"] == 2
    assert assets == {}


def test_normalize_falls_back_to_markdown_block() -> None:
    payload = {"results": {"sample.pdf": {"md_content": "只有 Markdown"}}}

    _, blocks, _, _ = MinerUResultNormalizer().normalize(payload)

    assert len(blocks) == 1
    assert blocks[0].content == "只有 Markdown"


def test_normalize_rejects_empty_result() -> None:
    with pytest.raises(ValueError, match="empty"):
        MinerUResultNormalizer().normalize({"results": {"sample.pdf": {}}})


def test_normalize_decodes_image_assets() -> None:
    encoded = base64.b64encode(b"image-content").decode()
    payload = {
        "results": {
            "sample.pdf": {
                "md_content": "正文",
                "images": {"figure.png": f"data:image/png;base64,{encoded}"},
            }
        }
    }

    _, _, manifest, assets = MinerUResultNormalizer().normalize(payload)

    assert assets["figure.png"] == b"image-content"
    assert manifest["asset_count"] == 1
