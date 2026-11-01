import tempfile
from pathlib import Path

from knowledge.service import KnowledgeService


def test_local_knowledge_retrieval_returns_citation() -> None:
    with tempfile.TemporaryDirectory(dir=Path.cwd()) as directory:
        knowledge_dir = Path(directory)
        (knowledge_dir / "fire.md").write_text(
            "# 实验室火灾处置\n\n电气设备着火时应先切断电源，不能直接用水扑救。",
            encoding="utf-8",
        )
        (knowledge_dir / "waste.md").write_text(
            "# 危险废物\n\n废液不得倒入下水道。",
            encoding="utf-8",
        )
        service = KnowledgeService(knowledge_dir, top_k=2, min_score=0.1)

        results = service.search("电气着火可以用水吗")

    assert results
    assert results[0].title == "实验室火灾处置"
    assert results[0].source == "fire.md"
    assert results[0].score > 0
