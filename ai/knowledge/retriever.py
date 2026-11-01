import math
import re
from collections import Counter

from langchain_core.documents import Document

from schemas.knowledge import KnowledgeCitation


class LocalBm25Retriever:
    def __init__(
        self,
        documents: list[Document],
        min_score: float = 0.2,
    ) -> None:
        self.documents = documents
        self.min_score = min_score
        self.term_frequencies = [
            Counter(_tokenize(document.page_content)) for document in documents
        ]
        self.average_length = (
            sum(sum(freq.values()) for freq in self.term_frequencies)
            / max(len(self.term_frequencies), 1)
        )
        self.document_frequency = Counter()
        for frequency in self.term_frequencies:
            self.document_frequency.update(frequency.keys())

    def search(self, query: str, top_k: int = 4) -> list[KnowledgeCitation]:
        query_terms = _tokenize(query)
        if not query_terms or not self.documents:
            return []
        scored = []
        for index, document in enumerate(self.documents):
            score = self._score(query_terms, index)
            if score > 0:
                scored.append((score, document))
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

    def _score(self, query_terms: list[str], index: int) -> float:
        frequencies = self.term_frequencies[index]
        length = sum(frequencies.values())
        score = 0.0
        k1 = 1.5
        b = 0.75
        for term in query_terms:
            frequency = frequencies.get(term, 0)
            if frequency == 0:
                continue
            document_frequency = self.document_frequency.get(term, 0)
            idf = math.log(
                1
                + (len(self.documents) - document_frequency + 0.5)
                / (document_frequency + 0.5)
            )
            denominator = frequency + k1 * (
                1 - b + b * length / max(self.average_length, 1)
            )
            score += idf * frequency * (k1 + 1) / denominator
        return score


def _tokenize(text: str) -> list[str]:
    normalized = text.lower()
    latin_terms = re.findall(r"[a-z0-9_]+", normalized)
    chinese_runs = re.findall(r"[\u4e00-\u9fff]+", normalized)
    tokens = list(latin_terms)
    for run in chinese_runs:
        tokens.extend(run)
        tokens.extend(run[index : index + 2] for index in range(len(run) - 1))
    return tokens
