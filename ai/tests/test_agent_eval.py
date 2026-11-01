from pathlib import Path

from evaluation.agent_eval import load_agent_cases, run_agent_evaluation

AI_DIR = Path(__file__).resolve().parents[1]


def test_agent_evaluation_dataset_meets_baseline() -> None:
    report = run_agent_evaluation(
        AI_DIR / "evaluation" / "agent_cases.jsonl",
    )

    assert report.metrics.case_count >= 18
    assert report.metrics.pass_rate == 1.0
    assert report.metrics.intent_accuracy == 1.0
    assert report.metrics.tool_selection_accuracy == 1.0
    assert report.metrics.parameter_accuracy == 1.0
    assert report.metrics.end_to_end_success_rate == 1.0


def test_agent_eval_cases_do_not_reuse_ids() -> None:
    cases = load_agent_cases(
        AI_DIR / "evaluation" / "agent_cases.jsonl",
    )

    assert len({case.case_id for case in cases}) == len(cases)
