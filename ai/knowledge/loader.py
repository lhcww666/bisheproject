import hashlib
import re
from pathlib import Path

from langchain_core.documents import Document

VALID_ROLES = {"STUDENT", "TEACHER", "ADMIN", "USER"}


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
        body, metadata = _parse_front_matter(content, path)
        relative = path.relative_to(directory).as_posix()
        document_id = hashlib.sha1(relative.encode("utf-8")).hexdigest()[:16]
        title = metadata.get("title") or _title_of(body, path.stem)
        documents.append(
            Document(
                page_content=body,
                metadata={
                    "document_id": document_id,
                    "title": title,
                    "source": relative,
                    "allowed_roles": _parse_roles(
                        metadata.get("allowed_roles", ""),
                        path,
                    ),
                },
            )
        )
    return documents


def _parse_front_matter(content: str, path: Path) -> tuple[str, dict[str, str]]:
    lines = content.splitlines()
    if not lines or lines[0].strip() != "---":
        return content, {}

    metadata = {}
    for index, line in enumerate(lines[1:], start=1):
        stripped = line.strip()
        if stripped == "---":
            body = "\n".join(lines[index + 1 :]).strip()
            return body, metadata
        if not stripped or stripped.startswith("#"):
            continue
        if ":" not in stripped:
            raise ValueError(f"知识文档元数据格式无效: {path}")
        key, value = stripped.split(":", 1)
        metadata[key.strip().lower()] = value.strip()
    raise ValueError(f"知识文档元数据缺少结束标记: {path}")


def _parse_roles(value: str, path: Path) -> list[str]:
    normalized = value.strip().strip("[]")
    if not normalized:
        return []
    roles = {
        item.upper()
        for item in re.split(r"[\s,]+", normalized)
        if item
    }
    invalid = roles - VALID_ROLES
    if invalid:
        raise ValueError(f"知识文档包含无效角色 {sorted(invalid)}: {path}")
    return sorted(roles)


def _title_of(content: str, fallback: str) -> str:
    for line in content.splitlines():
        stripped = line.strip()
        if stripped.startswith("#"):
            title = stripped.lstrip("#").strip()
            if title:
                return title
    return fallback
