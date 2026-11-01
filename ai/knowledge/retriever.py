import math
import re
from collections import Counter
from collections.abc import Collection

from langchain_core.documents import Document

from schemas.knowledge import KnowledgeCitation


class LocalBm25Retriever:
    def __init__(
        self,
        documents: list[Document],
        min_score: float = 0.2,
        min_query_coverage: float = 0.15,
    ) -> None:
        self.documents = documents
        self.min_score = min_score
        self.min_query_coverage = min_query_coverage
        self.term_frequencies = [
            Counter(_tokenize(document.page_content)) for document in documents
        ]

    def search(
        self,
        query: str,
        roles: Collection[str] | None = None,
        top_k: int = 4,
    ) -> list[KnowledgeCitation]:
        query_terms = _tokenize(query)
        if not query_terms or not self.documents:
            return []
        query_term_set = set(query_terms)
        normalized_roles = {
            role.strip().upper()
            for role in (roles or [])
            if role.strip()
        }
        visible_indexes = [
            index
            for index, document in enumerate(self.documents)
            if _is_visible(document, normalized_roles)
        ]
        if not visible_indexes:
            return []
        visible_frequencies = [
            self.term_frequencies[index]
            for index in visible_indexes
        ]
        average_length = (
            sum(sum(frequency.values()) for frequency in visible_frequencies)
            / len(visible_frequencies)
        )
        document_frequency = Counter()
        for frequency in visible_frequencies:
            document_frequency.update(frequency.keys())

        scored = []
        for index in visible_indexes:
            coverage = len(
                query_term_set
                & set(self.term_frequencies[index].keys())
            ) / len(query_term_set)
            if coverage < self.min_query_coverage:
                continue
            score = self._score(
                query_terms,
                index,
                document_frequency,
                average_length,
                len(visible_indexes),
            )
            if score > 0:
                scored.append((score, self.documents[index]))
        scored.sort(key=lambda item: item[0], reverse=True)
        if not scored:
            return []
        best = scored[0][0]
        threshold = max(self.min_score, best * 0.2)
        results = []
        seen = set()
        for score, document in scored:
            document_id = str(document.metadata.get("document_id", ""))
            if score < threshold or document_id in seen:
                continue
            seen.add(document_id)
            results.append(
                KnowledgeCitation(
                    documentId=document_id,
                    title=str(document.metadata.get("title", "未命名资料")),
                    source=str(document.metadata.get("source", "")),
                    snippet=document.page_content[:300],
                    score=round(score, 4),
                )
            )
            if len(results) >= top_k:
                break
        return results

    def _score(
        self,
        query_terms: list[str],
        index: int,
        document_frequency: Counter,
        average_length: float,
        corpus_size: int,
    ) -> float:
        frequencies = self.term_frequencies[index]
        length = sum(frequencies.values())
        score = 0.0
        k1 = 1.5
        b = 0.75
        for term in query_terms:
            frequency = frequencies.get(term, 0)
            if frequency == 0:
                continue
            frequency_in_documents = document_frequency.get(term, 0)
            idf = math.log(
                1
                + (corpus_size - frequency_in_documents + 0.5)
                / (frequency_in_documents + 0.5)
            )
            denominator = frequency + k1 * (
                1 - b + b * length / max(average_length, 1)
            )
            score += idf * frequency * (k1 + 1) / denominator
        return score


def _tokenize(text: str) -> list[str]:
    normalized = text.lower()
    latin_terms = re.findall(r"[a-z0-9_]+", normalized)
    chinese_runs = re.findall(r"[\u4e00-\u9fff]+", normalized)
    tokens = list(latin_terms)
    for run in chinese_runs:
        if len(run) == 1:
            tokens.append(run)
        else:
            tokens.extend(
                run[index : index + 2]
                for index in range(len(run) - 1)
            )
    return tokens


def _is_visible(document: Document, roles: set[str]) -> bool:
    allowed_roles = {
        str(role).strip().upper()
        for role in document.metadata.get("allowed_roles", [])
        if str(role).strip()
    }
    return not allowed_roles or bool(allowed_roles & roles)
