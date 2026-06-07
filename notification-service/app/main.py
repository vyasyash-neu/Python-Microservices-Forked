import asyncio
from contextlib import asynccontextmanager
from fastapi import FastAPI
from app.config import settings
from app.kafka.consumer import start_consumer
from prometheus_fastapi_instrumentator import Instrumentator
from app.logging_config import setup_logging
from app.tracing_config import setup_tracing

setup_logging("notification-service")

# ─── Lifespan ─────────────────────────────────────────────────────────────────
@asynccontextmanager
async def lifespan(app: FastAPI):
    consumer_task = asyncio.create_task(start_consumer())
    print(f"✅ {settings.APP_NAME} started — Kafka consumer running")
    print(f"📧 Email sending: {'ENABLED' if settings.EMAIL_ENABLED else 'DISABLED (log-only)'}")

    yield

    consumer_task.cancel()
    try:
        await consumer_task
    except asyncio.CancelledError:
        pass
    print("🔌 Kafka consumer stopped")


# ─── App ──────────────────────────────────────────────────────────────────────
app = FastAPI(
    title="Notification Service",
    description="Consumes Kafka events and sends email notifications",
    version="1.0.0",
    lifespan=lifespan,
)

Instrumentator().instrument(app).expose(app, endpoint="/metrics", include_in_schema=False)

setup_tracing("notification-service", app)

@app.get("/health")
async def health():
    return {"status": "UP", "service": settings.APP_NAME}