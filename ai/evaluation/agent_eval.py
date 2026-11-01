import asyncio
import json
from collections.abc import Collection
from dataclasses import asdict, dataclass
from pathlib import Path

from langchain_core.messages import AIMessage

from config.settings import Settings
from schemas.agent import AgentAuthContext, AgentContextClaims
from schemas.chat import ChatHistoryItem, ChatRequest
from services.chat_service import ChatService
from services.reservation_service import missing_field_message

VALID_INTENTS = {
    "BUSINESS_QUERY",
    "RESERVATION",
    "SAFETY_KNOWLEDGE",
    "GENERAL",
}


@dataclass(frozen=True)
class AgentEvalCase:
    case_id: str
    category: str
    role: str
    message: str
    expected_intent: str
    expected_tools: list[str]
    extraction: dict | None
    expected_missing_fields: list[str]
    expected_query_arguments: dict | None
    expected_draft_status: str | None
    expected_answer_contains: str | None
    history: list[dict]


@dataclass(frozen=True)
class AgentEvalCaseResult:
    case_id: str
    category: str
    actual_intent: str
    actual_tools: list[str]
    actual_query_arguments: dict | None
    actual_draft_status: str | None
    intent_correct: bool
    tools_correct: bool
    parameters_correct: bool
    answer_correct: bool
    passed: bool


@dataclass(frozen=True)
class AgentEvalMetrics:
    case_count: int
    passed_count: int
    pass_rate: float
    intent_accuracy: float
    tool_selection_accuracy: float
    parameter_accuracy: float
    end_to_end_success_rate: float


@dataclass(frozen=True)
class AgentEvalReport:
    metrics: AgentEvalMetrics
    cases: list[AgentEvalCaseResult]

    def to_dict(self) -> dict:
        return {
            "metrics": asdict(self.metrics),
            "cases": [asdict(case) for case in self.cases],
        }


class ScriptedModel:
    def __init__(self, extraction: dict | None) -> None:
        self.extraction = extraction

    async def ainvoke(self, messages):
        if "预约参数抽取器" in str(messages[0].content):
            return AIMessage(
                content=json.dumps(self.extraction or {}, ensure_ascii=False)
            )
        return AIMessage(content="这是脚本模型回答")


class RecordingBusinessTools:
    def __init__(self) -> None:
        self.invocations = []

    async def invoke_tool(self, auth, tool_name: str, arguments=None):
        self.invocations.append(
            {
                "tool_name": tool_name,
                "arguments": arguments,
            }
        )
        if tool_name == "queryMyCredit":
            return {"score": 100, "minScore": 60, "canBook": True}
        if tool_name == "checkSafetyExamStatus":
            return {"hasValidPass": True}
        if tool_name == "queryAvailableRooms":
            return {
                "list": [
                    {
                        "id": 12,
                        "roomCode": "CHEM-A101",
                        "roomName": "化学实验室 A101",
                    }
                ],
                "total": 1,
            }
        if tool_name == "queryAvailableDevices":
            return {
                "list": [{"id": 3, "deviceName": "离心机"}],
                "total": 1,
            }
        if tool_name == "createReservationDraft":
            return {
                "id": 70,
                "sessionId": 101,
                "status": "PENDING",
                "request": {
                    "roomId": 12,
                    "deviceIds": [3],
                },
            }
        return {
            "list": [
                {
                    "reservationNo": "R-501",
                    "roomName": "化学实验室 A101",
                }
            ],
            "total": 1,
        }


class EmptyKnowledgeService:
    def search(
        self,
        query: str,
        roles: Collection[str] | None = None,
    ):
        return []


def load_agent_cases(path: Path) -> list[AgentEvalCase]:
    cases = []
    seen_ids = set()
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        payload = json.loads(line)
        case = AgentEvalCase(
            case_id=str(payload["id"]),
            category=str(payload["category"]),
            role=str(payload["role"]).upper(),
            message=str(payload["message"]),
            expected_intent=str(payload["expected_intent"]),
            expected_tools=list(payload.get("expected_tools", [])),
            extraction=payload.get("extraction"),
            expected_missing_fields=list(
                payload.get("expected_missing_fields", [])
            ),
            expected_query_arguments=payload.get(
                "expected_query_arguments"
            ),
            expected_draft_status=payload.get("expected_draft_status"),
            expected_answer_contains=payload.get("expected_answer_contains"),
            history=list(payload.get("history", [])),
        )
        if case.case_id in seen_ids:
            raise ValueError(f"Agent 评测用例 ID 重复: {case.case_id}")
        if case.expected_intent not in VALID_INTENTS:
            raise ValueError(
                f"Agent 评测用例 {case.case_id} 的意图无效"
            )
        seen_ids.add(case.case_id)
        cases.append(case)
    if not cases:
        raise ValueError(f"Agent 评测数据集为空: {path}")
    return cases


def evaluate_agent_cases(
    cases: list[AgentEvalCase],
) -> AgentEvalReport:
    results = []
    for index, case in enumerate(cases, start=1):
        model = ScriptedModel(case.extraction)
        tools = RecordingBusinessTools()
        service = ChatService(
            _settings(),
            lambda _: model,
            tools,
            EmptyKnowledgeService(),
        )
        response = asyncio.run(
            service.chat(
                ChatRequest(
                    sessionId=100 + index,
                    message=case.message,
                    history=[
                        ChatHistoryItem.model_validate(item)
                        for item in case.history
                    ],
                ),
                _context(case.role, 100 + index),
            )
        )
        actual_tools = [
            tool_call.tool_name for tool_call in response.tool_calls
        ]
        query_arguments = next(
            (
                invocation["arguments"]
                for invocation in tools.invocations
                if invocation["tool_name"] == "queryAvailableRooms"
            ),
            None,
        )
        draft_status = (
            response.draft.get("status")
            if isinstance(response.draft, dict)
            else None
        )
        intent_correct = response.intent == case.expected_intent
        tools_correct = actual_tools == case.expected_tools
        parameters_correct = _parameters_correct(
            case,
            response.answer,
            query_arguments,
        )
        answer_correct = (
            case.expected_answer_contains is None
            or case.expected_answer_contains in response.answer
        )
        draft_correct = (
            case.expected_draft_status is None
            or draft_status == case.expected_draft_status
        )
        results.append(
            AgentEvalCaseResult(
                case_id=case.case_id,
                category=case.category,
                actual_intent=response.intent,
                actual_tools=actual_tools,
                actual_query_arguments=query_arguments,
                actual_draft_status=draft_status,
                intent_correct=intent_correct,
                tools_correct=tools_correct,
                parameters_correct=parameters_correct,
                answer_correct=answer_correct and draft_correct,
                passed=(
                    intent_correct
                    and tools_correct
                    and parameters_correct
                    and answer_correct
                    and draft_correct
                ),
            )
        )
    return AgentEvalReport(
        metrics=_metrics(results),
        cases=results,
    )


def run_agent_evaluation(dataset_path: Path) -> AgentEvalReport:
    return evaluate_agent_cases(load_agent_cases(dataset_path))


def _parameters_correct(
    case: AgentEvalCase,
    answer: str,
    query_arguments: dict | None,
) -> bool:
    if case.extraction is None:
        return True
    if case.expected_missing_fields:
        return answer == missing_field_message(case.expected_missing_fields)
    return (
        case.expected_query_arguments is None
        or query_arguments == case.expected_query_arguments
    )


def _metrics(results: list[AgentEvalCaseResult]) -> AgentEvalMetrics:
    parameter_results = [
        result
        for result in results
        if result.category in {"reservation", "end_to_end"}
    ]
    end_to_end = [
        result
        for result in results
        if result.category == "end_to_end"
    ]
    return AgentEvalMetrics(
        case_count=len(results),
        passed_count=sum(result.passed for result in results),
        pass_rate=_ratio(
            sum(result.passed for result in results),
            len(results),
        ),
        intent_accuracy=_ratio(
            sum(result.intent_correct for result in results),
            len(results),
        ),
        tool_selection_accuracy=_ratio(
            sum(result.tools_correct for result in results),
            len(results),
        ),
        parameter_accuracy=_ratio(
            sum(result.parameters_correct for result in parameter_results),
            len(parameter_results),
        ),
        end_to_end_success_rate=_ratio(
            sum(result.passed for result in end_to_end),
            len(end_to_end),
        ),
    )


def _ratio(numerator: float, denominator: int) -> float:
    return round(numerator / denominator, 4) if denominator else 1.0


def _settings() -> Settings:
    return Settings(
        agent_context_secret="0123456789abcdef0123456789abcdef",
        llm_api_key="scripted-model",
        llm_base_url="http://127.0.0.1:9999",
        llm_model="scripted-model",
        java_base_url="http://127.0.0.1:8087",
    )


def _context(role: str, session_id: int) -> AgentAuthContext:
    return AgentAuthContext(
        claims=AgentContextClaims(
            version=1,
            userId=7,
            role=role,
            sessionId=session_id,
            traceId=f"eval-{session_id}",
            allowedTools=[],
            issuedAt=1,
            expiresAt=2,
        ),
        token="scripted-context",
        request_id=f"eval-{session_id}",
    )
