import httpx


class JavaToolError(RuntimeError):
    """Java 内部工具调用失败。"""


class JavaApiClient:
    def __init__(self, base_url: str, timeout_seconds: float = 10.0) -> None:
        self.base_url = base_url.rstrip("/")
        self.timeout_seconds = timeout_seconds

    async def get_my_reservations(
        self,
        context_token: str,
        request_id: str,
        status: str | None = None,
        page_num: int = 1,
        page_size: int = 20,
    ) -> dict:
        params: dict[str, str | int] = {
            "pageNum": page_num,
            "pageSize": page_size,
        }
        if status:
            params["status"] = status
        try:
            async with httpx.AsyncClient(timeout=self.timeout_seconds) as client:
                response = await client.get(
                    f"{self.base_url}/internal/agent/tools/reservations/my",
                    params=params,
                    headers={
                        "X-Agent-Context": context_token,
                        "X-Request-Id": request_id,
                    },
                )
            payload = response.json()
        except Exception as exception:
            raise JavaToolError("实验室预约工具暂时不可用") from exception
        if response.status_code != 200 or payload.get("code") != 200:
            raise JavaToolError(payload.get("msg") or "实验室预约工具返回错误")
        return payload.get("data") or {}
