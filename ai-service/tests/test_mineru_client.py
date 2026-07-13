import httpx
import pytest

from linkwisehub_ai.adapters.mineru import MinerUClient, MinerUError


@pytest.mark.asyncio
async def test_maps_mineru_task_status() -> None:
    transport = httpx.MockTransport(
        lambda request: httpx.Response(
            200,
            json={"task_id": "task-1", "status": "processing", "created_at": "2026-07-13T12:00:00Z"},
        )
    )
    async with httpx.AsyncClient(base_url="http://mineru", transport=transport) as client:
        task = await MinerUClient(client, 30).get_status("task-1")

    assert task.status == "RUNNING"
    assert task.progress == 50


@pytest.mark.asyncio
async def test_rejects_invalid_mineru_payload() -> None:
    transport = httpx.MockTransport(lambda request: httpx.Response(200, json={"status": "unknown"}))
    async with httpx.AsyncClient(base_url="http://mineru", transport=transport) as client:
        with pytest.raises(MinerUError, match="invalid task payload"):
            await MinerUClient(client, 30).get_status("task-1")
