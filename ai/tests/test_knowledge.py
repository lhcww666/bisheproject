import tempfile
from pathlib import Path

import pytest

from knowledge.loader import load_documents
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


def test_knowledge_loader_reads_title_and_allowed_roles() -> None:
    with tempfile.TemporaryDirectory(dir=Path.cwd()) as directory:
        knowledge_dir = Path(directory)
        (knowledge_dir / "admin.md").write_text(
            (
                "---\n"
                "title: 管理员安全处置\n"
                "allowed_roles: [ADMIN, TEACHER]\n"
                "---\n\n"
                "# 正文标题\n\n"
                "仅管理员和教师可见。"
            ),
            encoding="utf-8",
        )

        documents = load_documents(knowledge_dir)

    assert len(documents) == 1
    assert documents[0].metadata["title"] == "管理员安全处置"
    assert documents[0].metadata["allowed_roles"] == ["ADMIN", "TEACHER"]
    assert "---" not in documents[0].page_content
    assert "仅管理员和教师可见。" in documents[0].page_content


def test_role_restricted_knowledge_only_returns_visible_documents() -> None:
    with tempfile.TemporaryDirectory(dir=Path.cwd()) as directory:
        knowledge_dir = Path(directory)
        (knowledge_dir / "student.md").write_text(
            (
                "---\n"
                "allowed_roles: STUDENT\n"
                "---\n\n"
                "# 学生火灾处置\n\n"
                "电气着火应切断电源并使用适用灭火器。"
            ),
            encoding="utf-8",
        )
        (knowledge_dir / "admin.md").write_text(
            (
                "---\n"
                "allowed_roles: ADMIN\n"
                "---\n\n"
                "# 管理员火灾处置\n\n"
                "管理员按照应急预案组织灭火和人员疏散。"
            ),
            encoding="utf-8",
        )
        service = KnowledgeService(knowledge_dir, top_k=5, min_score=0.1)

        student_results = service.search("火灾灭火", roles={"STUDENT"})
        admin_results = service.search("火灾灭火", roles={"ADMIN"})
        anonymous_results = service.search("火灾灭火")

    assert [result.source for result in student_results] == ["student.md"]
    assert [result.source for result in admin_results] == ["admin.md"]
    assert anonymous_results == []


def test_knowledge_loader_rejects_unknown_roles() -> None:
    with tempfile.TemporaryDirectory(dir=Path.cwd()) as directory:
        knowledge_dir = Path(directory)
        (knowledge_dir / "invalid.md").write_text(
            "---\nallowed_roles: ROOT\n---\n\n# 非法角色",
            encoding="utf-8",
        )

        with pytest.raises(ValueError, match="无效角色"):
            load_documents(knowledge_dir)
