import asyncio

import pytest
from fastapi import HTTPException

from routers.knowledge import search_knowledge
from schemas.agent import AgentAuthContext, AgentContextClaims
from schemas.knowledge import KnowledgeCitation, KnowledgeSearchRequest


class FakeKnowledgeService:
    def __init__(self) -> None:
        self.query = ""

    def search(self, query: str) -> list[KnowledgeCitation]:
        self.query = query
        return [
            KnowledgeCitation(
                documentId="safety-1",
                title="实验室火灾处置",
                source="lab-safety.md",
                snippet="电气设备着火时应先切断电源。",
                score=0.9,
            )
        ]


def context(session_id: int = 101) -> AgentAuthContext:
    return AgentAuthContext(
        claims=AgentContextClaims(
            version=1,
            userId=7,
            role="STUDENT",
            sessionId=session_id,
            traceId="trace-1",
            allowedTools=[],
            issuedAt=1,
            expiresAt=2,
        ),
        token="signed-context",
        request_id="trace-1",
    )


def test_knowledge_search_returns_citations() -> None:
    service = FakeKnowledgeService()

    response = asyncio.run(
        search_knowledge(
            KnowledgeSearchRequest(sessionId=101, query="电气着火怎么办"),
            context(),
            service,
        )
    )

    assert service.query == "电气着火怎么办"
    assert response.session_id == 101
    assert response.trace_id == "trace-1"
    assert response.citations[0].document_id == "safety-1"


def test_knowledge_search_rejects_session_mismatch() -> None:
    with pytest.raises(HTTPException) as exception:
        asyncio.run(
            search_knowledge(
                KnowledgeSearchRequest(sessionId=102, query="电气着火怎么办"),
                context(101),
                FakeKnowledgeService(),
            )
        )

    assert exception.value.status_code == 403
