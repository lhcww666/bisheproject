from pathlib import Path

from evaluation.rag_eval import load_eval_cases, run_evaluation

AI_DIR = Path(__file__).resolve().parents[1]


def test_rag_evaluation_dataset_meets_baseline() -> None:
    report = run_evaluation(
        AI_DIR / "evaluation" / "rag_cases.jsonl",
        AI_DIR / "data" / "knowledge",
    )

    assert report.metrics.case_count >= 25
    assert report.metrics.pass_rate == 1.0
    assert report.metrics.top1_accuracy == 1.0
    assert report.metrics.abstention_accuracy == 1.0
    assert report.metrics.role_isolation_rate == 1.0


def test_eval_cases_do_not_reuse_ids() -> None:
    cases = load_eval_cases(AI_DIR / "evaluation" / "rag_cases.jsonl")

    assert len({case.case_id for case in cases}) == len(cases)
