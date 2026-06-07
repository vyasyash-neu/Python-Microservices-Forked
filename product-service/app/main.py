from contextlib import asynccontextmanager
from fastapi import FastAPI
from app.database import connect_db, close_db
from app.routes.product_routes import router as product_router
from app.kafka.producer import start_producer, stop_producer
from prometheus_fastapi_instrumentator import Instrumentator
from app.logging_config import setup_logging
from app.tracing_config import setup_tracing

setup_logging("product-service")

@asynccontextmanager
async def lifespan(app: FastAPI):
    await connect_db()
    await start_producer()
    yield
    await stop_producer()
    await close_db()

app = FastAPI(title="Product Service", version="1.0.0", lifespan=lifespan)
Instrumentator().instrument(app).expose(app, endpoint="/metrics", include_in_schema=False)

setup_tracing("product-service", app)

app.include_router(product_router, prefix="/api/products", tags=["Products"])

@app.get("/health")
async def health():
    return {"status": "UP", "service": "product-service"}