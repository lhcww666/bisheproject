from pydantic import BaseModel, ConfigDict, Field


class KnowledgeCitation(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    document_id: str = Field(alias="documentId")
    title: str
    source: str
    snippet: str
    score: float
