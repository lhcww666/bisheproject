from collections.abc import Callable

from langchain_core.language_models.chat_models import BaseChatModel
from langgraph.graph import END, START, StateGraph

from agent.nodes import AgentNodes
from agent.state import AgentState
from config.settings import Settings
from integrations.llm import create_chat_model
from knowledge.service import KnowledgeService
from tools.business_tools import BusinessToolService


def build_agent_graph(
    settings: Settings,
    model_factory: Callable[[Settings], BaseChatModel] = create_chat_model,
    business_tools: BusinessToolService | None = None,
    knowledge_service: KnowledgeService | None = None,
):
    nodes = AgentNodes(settings, model_factory, business_tools, knowledge_service)
    graph = StateGraph(AgentState)
    graph.add_node("route_intent", nodes.route_intent)
    graph.add_node("query_business_tool", nodes.query_business_tool)
    graph.add_node("extract_reservation", nodes.extract_reservation)
    graph.add_node("ask_reservation_fields", nodes.ask_reservation_fields)
    graph.add_node("query_reservation_rooms", nodes.query_reservation_rooms)
    graph.add_node("create_reservation_draft", nodes.create_reservation_draft)
    graph.add_node("business_query", nodes.answer_business_query)
    graph.add_node("reservation", nodes.answer_reservation)
    graph.add_node("retrieve_safety_knowledge", nodes.retrieve_safety_knowledge)
    graph.add_node("safety_knowledge", nodes.answer_safety)
    graph.add_node("general", nodes.answer_general)
    graph.add_edge(START, "route_intent")
    graph.add_conditional_edges(
        "route_intent",
        lambda state: state["intent"],
        {
            "BUSINESS_QUERY": "query_business_tool",
            "RESERVATION": "extract_reservation",
            "SAFETY_KNOWLEDGE": "retrieve_safety_knowledge",
            "GENERAL": "general",
        },
    )
    graph.add_conditional_edges(
        "extract_reservation",
        nodes.next_reservation_step,
        {
            "ask": "ask_reservation_fields",
            "query": "query_reservation_rooms",
        },
    )
    graph.add_edge("query_business_tool", "business_query")
    graph.add_edge("ask_reservation_fields", END)
    graph.add_conditional_edges(
        "query_reservation_rooms",
        nodes.next_after_room_query,
        {
            "answer": "reservation",
            "draft": "create_reservation_draft",
        },
    )
    graph.add_edge("create_reservation_draft", "reservation")
    graph.add_edge("retrieve_safety_knowledge", "safety_knowledge")
    graph.add_edge("business_query", END)
    graph.add_edge("reservation", END)
    graph.add_edge("safety_knowledge", END)
    graph.add_edge("general", END)
    return graph.compile()
