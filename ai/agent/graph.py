from collections.abc import Callable

from langchain_core.language_models.chat_models import BaseChatModel
from langgraph.graph import END, START, StateGraph

from agent.nodes import AgentNodes
from agent.state import AgentState
from config.settings import Settings
from integrations.llm import create_chat_model
from tools.business_tools import BusinessToolService


def build_agent_graph(
    settings: Settings,
    model_factory: Callable[[Settings], BaseChatModel] = create_chat_model,
    business_tools: BusinessToolService | None = None,
):
    nodes = AgentNodes(settings, model_factory, business_tools)
    graph = StateGraph(AgentState)
    graph.add_node("route_intent", nodes.route_intent)
    graph.add_node("query_business_tool", nodes.query_business_tool)
    graph.add_node("business_query", nodes.answer_business_query)
    graph.add_node("reservation", nodes.answer_reservation)
    graph.add_node("safety_knowledge", nodes.answer_safety)
    graph.add_node("general", nodes.answer_general)
    graph.add_edge(START, "route_intent")
    graph.add_conditional_edges(
        "route_intent",
        lambda state: state["intent"],
        {
            "BUSINESS_QUERY": "query_business_tool",
            "RESERVATION": "reservation",
            "SAFETY_KNOWLEDGE": "safety_knowledge",
            "GENERAL": "general",
        },
    )
    graph.add_edge("query_business_tool", "business_query")
    graph.add_edge("business_query", END)
    graph.add_edge("reservation", END)
    graph.add_edge("safety_knowledge", END)
    graph.add_edge("general", END)
    return graph.compile()
