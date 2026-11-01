import hashlib
from pathlib import Path

from langchain_core.documents import Document


def load_documents(directory: Path) -> list[Document]:
    if not directory.exists():
        return []
    documents = []
    for path in sorted(directory.rglob("*")):
        if not path.is_file() or path.suffix.lower() not in {".md", ".txt"}:
            continue
        content = path.read_text(encoding="utf-8").strip()
        if not content:
            continue
        relative = path.relative_to(directory).as_posix()
        document_id = hashlib.sha1(relative.encode("utf-8")).hexdigest()[:16]
        title = _title_of(content, path.stem)
        documents.append(
            Document(
                page_content=content,
                metadata={
                    "document_id": document_id,
                    "title": title,
                    "source": relative,
                },
            )
        )
    return documents


def _title_of(content: str, fallback: str) -> str:
    for line in content.splitlines():
        stripped = line.strip()
        if stripped.startswith("#"):
            title = stripped.lstrip("#").strip()
            if title:
                return title
    return fallback
