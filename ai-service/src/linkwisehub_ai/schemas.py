from datetime import datetime
from typing import Any, Literal

from pydantic import BaseModel, Field


class SourceObject(BaseModel):
    bucket: str = Field(min_length=1, max_length=100)
    object_key: str = Field(min_length=1, max_length=500)
    file_name: str = Field(min_length=1, max_length=255)
    file_type: str = Field(min_length=1, max_length=20)


class ParseOptions(BaseModel):
    backend: str = "pipeline"
    parse_method: str = "auto"
    language: str = "ch"
    formula_enabled: bool = True
    table_enabled: bool = True


class DocumentParseRequest(BaseModel):
    document_id: int = Field(gt=0)
    source: SourceObject
    options: ParseOptions = Field(default_factory=ParseOptions)


class DocumentParseTask(BaseModel):
    task_id: str
    status: Literal["PENDING", "RUNNING", "SUCCESS", "FAILED"]
    progress: int | None = None
    error_message: str | None = None
    created_at: datetime | None = None
    started_at: datetime | None = None
    completed_at: datetime | None = None


class MaterializeRequest(BaseModel):
    document_id: int = Field(gt=0)


class ParsedBlock(BaseModel):
    index: int
    block_type: str
    content: str
    page: int | None = None
    title: str | None = None
    metadata: dict[str, Any] = Field(default_factory=dict)


class MaterializedArtifacts(BaseModel):
    bucket: str
    manifest_object: str
    markdown_object: str
    blocks_object: str
    asset_prefix: str
    block_count: int


class MaterializeResponse(BaseModel):
    task_id: str
    status: Literal["SUCCESS"]
    artifacts: MaterializedArtifacts
