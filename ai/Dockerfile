FROM python:3.13-slim

WORKDIR /app

COPY pyproject.toml ./
COPY config ./config
COPY routers ./routers
COPY schemas ./schemas
COPY utils ./utils
COPY main.py ./

RUN pip install --no-cache-dir .

EXPOSE 8091

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8091"]
