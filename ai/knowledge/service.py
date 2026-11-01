from collections.abc import Collection
from functools import lru_cache
from pathlib import Path

from config.settings import get_settings
from knowledge.loader import load_documents
from knowledge.retriever import LocalBm25Retriever
from knowledge.splitter import split_documents
from schemas.knowledge import KnowledgeCitation


class KnowledgeService:
    def __init__(
        self,
        directory: Path,
        top_k: int = 4,
        min_score: float = 0.2,
    ) -> None:
        self.top_k = top_k
        chunks = split_documents(load_documents(directory))
        self.retriever = LocalBm25Retriever(chunks, min_score=min_score)

    def search(
        self,
        query: str,
        roles: Collection[str] | None = None,
        top_k: int | None = None,
    ) -> list[KnowledgeCitation]:
        return self.retriever.search(
            query,
            roles=roles,
            top_k=top_k or self.top_k,
        )


@lru_cache
def get_knowledge_service() -> KnowledgeService:
    settings = get_settings()
    directory = Path(settings.knowledge_dir)
    if not directory.is_absolute():
        directory = Path(__file__).resolve().parents[1] / directory
    return KnowledgeService(
        directory,
        top_k=settings.knowledge_top_k,
        min_score=settings.knowledge_min_score,
    )
