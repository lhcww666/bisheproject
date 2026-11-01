import argparse
import json
from pathlib import Path

from evaluation.agent_eval import run_agent_evaluation
from integrations.llm import create_chat_model

EVALUATION_DIR = Path(__file__).resolve().parent
DEFAULT_DATASET = EVALUATION_DIR / "agent_cases.jsonl"


def main() -> int:
    parser = argparse.ArgumentParser(description="运行校园实验室 Agent 离线评测")
    parser.add_argument("--dataset", type=Path, default=DEFAULT_DATASET)
    parser.add_argument("--min-pass-rate", type=float, default=0.9)
    parser.add_argument("--online", action="store_true")
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()

    report = run_agent_evaluation(
        args.dataset,
        model_factory=create_chat_model if args.online else None,
    )
    if args.json:
        print(json.dumps(report.to_dict(), ensure_ascii=False, indent=2))
    else:
        metrics = report.metrics
        print(f"cases: {metrics.case_count}")
        print(f"pass_rate: {metrics.pass_rate:.2%}")
        print(f"intent_accuracy: {metrics.intent_accuracy:.2%}")
        print(f"tool_selection_accuracy: {metrics.tool_selection_accuracy:.2%}")
        print(f"parameter_accuracy: {metrics.parameter_accuracy:.2%}")
        print(
            "end_to_end_success_rate: "
            f"{metrics.end_to_end_success_rate:.2%}"
        )
        print(f"average_latency_ms: {metrics.average_latency_ms:.0f}")
        print(
            "average_tokens_per_case: "
            f"{metrics.average_tokens_per_case:.1f}"
        )
        for case in report.cases:
            if not case.passed:
                print(
                    f"FAIL {case.case_id}: intent={case.actual_intent}, "
                    f"tools={case.actual_tools}, "
                    f"args={case.actual_query_arguments}, "
                    f"draft={case.actual_draft_status}, "
                    f"answer={case.actual_answer}"
                )
    return 0 if report.metrics.pass_rate >= args.min_pass_rate else 1


if __name__ == "__main__":
    raise SystemExit(main())
