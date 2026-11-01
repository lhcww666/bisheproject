import json
from dataclasses import asdict, dataclass
from pathlib import Path

from knowledge.loader import VALID_ROLES
from knowledge.service import KnowledgeService


@dataclass(frozen=True)
class RagEvalCase:
    case_id: str
    category: str
    role: str
    query: str
    answerable: bool
    expected_sources: list[str]
    forbidden_sources: list[str]


@dataclass(frozen=True)
class RagEvalCaseResult:
    case_id: str
    category: str
    role: str
    query: str
    answerable: bool
    expected_sources: list[str]
    forbidden_sources: list[str]
    returned_sources: list[str]
    passed: bool


@dataclass(frozen=True)
class RagEvalMetrics:
    case_count: int
    passed_count: int
    pass_rate: float
    top1_accuracy: float
    retrieval_hit_rate: float
    mean_reciprocal_rank: float
    abstention_accuracy: float
    role_isolation_rate: float


@dataclass(frozen=True)
class RagEvalReport:
    metrics: RagEvalMetrics
    cases: list[RagEvalCaseResult]

    def to_dict(self) -> dict:
        return {
            "metrics": asdict(self.metrics),
            "cases": [asdict(case) for case in self.cases],
        }


def load_eval_cases(path: Path) -> list[RagEvalCase]:
    cases = []
    seen_ids = set()
    line_number = 0
    for line_number, line in enumerate(
        path.read_text(encoding="utf-8").splitlines(),
        start=1,
    ):
        if not line.strip():
            continue
        payload = json.loads(line)
        case = RagEvalCase(
            case_id=str(payload["id"]),
            category=str(payload["category"]),
            role=str(payload["role"]).upper(),
            query=str(payload["query"]),
            answerable=bool(payload["answerable"]),
            expected_sources=list(payload.get("expected_sources", [])),
            forbidden_sources=list(payload.get("forbidden_sources", [])),
        )
        if case.case_id in seen_ids:
            raise ValueError(f"评测用例 ID 重复: {case.case_id}")
        if case.role not in VALID_ROLES:
            raise ValueError(
                f"评测用例 {case.case_id} 的角色无效: {case.role}"
            )
        if case.answerable and not case.expected_sources:
            raise ValueError(
                f"可回答用例 {case.case_id} 必须声明 expected_sources"
            )
        if not case.answerable and case.expected_sources:
            raise ValueError(
                f"不可回答用例 {case.case_id} 不应声明 expected_sources"
            )
        seen_ids.add(case.case_id)
        cases.append(case)
    if not cases:
        raise ValueError(f"评测数据集为空: {path}")
    return cases


def evaluate(
    cases: list[RagEvalCase],
    service: KnowledgeService,
) -> RagEvalReport:
    results = []
    for case in cases:
        citations = service.search(
            case.query,
            roles={case.role},
        )
        returned_sources = [citation.source for citation in citations]
        forbidden_hit = bool(
            set(returned_sources) & set(case.forbidden_sources)
        )
        if case.answerable:
            retrieval_hit = bool(
                set(returned_sources) & set(case.expected_sources)
            )
            top1_hit = bool(
                returned_sources
                and returned_sources[0] in case.expected_sources
            )
            passed = retrieval_hit and top1_hit and not forbidden_hit
        else:
            passed = not returned_sources and not forbidden_hit
        results.append(
            RagEvalCaseResult(
                case_id=case.case_id,
                category=case.category,
                role=case.role,
                query=case.query,
                answerable=case.answerable,
                expected_sources=case.expected_sources,
                forbidden_sources=case.forbidden_sources,
                returned_sources=returned_sources,
                passed=passed,
            )
        )
    return RagEvalReport(
        metrics=_metrics(results),
        cases=results,
    )


def run_evaluation(
    dataset_path: Path,
    knowledge_dir: Path,
    top_k: int = 4,
    min_score: float = 0.2,
    min_query_coverage: float = 0.15,
) -> RagEvalReport:
    cases = load_eval_cases(dataset_path)
    service = KnowledgeService(
        knowledge_dir,
        top_k=top_k,
        min_score=min_score,
        min_query_coverage=min_query_coverage,
    )
    return evaluate(cases, service)


def _metrics(results: list[RagEvalCaseResult]) -> RagEvalMetrics:
    answerable = [result for result in results if result.answerable]
    unanswerable = [
        result
        for result in results
        if not result.answerable
    ]
    role_cases = [
        result
        for result in results
        if result.forbidden_sources
    ]
    return RagEvalMetrics(
        case_count=len(results),
        passed_count=sum(result.passed for result in results),
        pass_rate=_ratio(
            sum(result.passed for result in results),
            len(results),
        ),
        top1_accuracy=_ratio(
            sum(_top1_hit(result) for result in answerable),
            len(answerable),
        ),
        retrieval_hit_rate=_ratio(
            sum(_retrieval_hit(result) for result in answerable),
            len(answerable),
        ),
        mean_reciprocal_rank=_ratio(
            sum(_reciprocal_rank(result) for result in answerable),
            len(answerable),
        ),
        abstention_accuracy=_ratio(
            sum(not result.returned_sources for result in unanswerable),
            len(unanswerable),
        ),
        role_isolation_rate=_ratio(
            sum(
                not (
                    set(result.returned_sources)
                    & set(result.forbidden_sources)
                )
                for result in role_cases
            ),
            len(role_cases),
        ),
    )


def _top1_hit(result: RagEvalCaseResult) -> bool:
    return bool(
        result.returned_sources
        and result.returned_sources[0] in result.expected_sources
    )


def _retrieval_hit(result: RagEvalCaseResult) -> bool:
    return bool(
        set(result.returned_sources) & set(result.expected_sources)
    )


def _reciprocal_rank(result: RagEvalCaseResult) -> float:
    for rank, source in enumerate(result.returned_sources, start=1):
        if source in result.expected_sources:
            return 1 / rank
    return 0.0


def _ratio(numerator: float, denominator: int) -> float:
    return round(numerator / denominator, 4) if denominator else 1.0
