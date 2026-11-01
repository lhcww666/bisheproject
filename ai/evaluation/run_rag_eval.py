import argparse
import json
from pathlib import Path

from evaluation.rag_eval import run_evaluation

EVALUATION_DIR = Path(__file__).resolve().parent
DEFAULT_DATASET = EVALUATION_DIR / "rag_cases.jsonl"
DEFAULT_KNOWLEDGE_DIR = EVALUATION_DIR.parent / "data" / "knowledge"


def main() -> int:
    parser = argparse.ArgumentParser(description="运行校园实验室离线 RAG 评测")
    parser.add_argument("--dataset", type=Path, default=DEFAULT_DATASET)
    parser.add_argument("--knowledge-dir", type=Path, default=DEFAULT_KNOWLEDGE_DIR)
    parser.add_argument("--top-k", type=int, default=4)
    parser.add_argument("--min-score", type=float, default=0.2)
    parser.add_argument("--min-coverage", type=float, default=0.15)
    parser.add_argument("--min-pass-rate", type=float, default=0.9)
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()

    report = run_evaluation(
        args.dataset,
        args.knowledge_dir,
        top_k=args.top_k,
        min_score=args.min_score,
        min_query_coverage=args.min_coverage,
    )
    if args.json:
        print(json.dumps(report.to_dict(), ensure_ascii=False, indent=2))
    else:
        metrics = report.metrics
        print(f"cases: {metrics.case_count}")
        print(f"pass_rate: {metrics.pass_rate:.2%}")
        print(f"top1_accuracy: {metrics.top1_accuracy:.2%}")
        print(f"retrieval_hit_rate: {metrics.retrieval_hit_rate:.2%}")
        print(f"mrr: {metrics.mean_reciprocal_rank:.4f}")
        print(f"abstention_accuracy: {metrics.abstention_accuracy:.2%}")
        print(f"role_isolation_rate: {metrics.role_isolation_rate:.2%}")
        failed = [case for case in report.cases if not case.passed]
        for case in failed:
            print(
                f"FAIL {case.case_id}: "
                f"role={case.role}, returned={case.returned_sources}, "
                f"expected={case.expected_sources}"
            )
    return 0 if report.metrics.pass_rate >= args.min_pass_rate else 1


if __name__ == "__main__":
    raise SystemExit(main())
