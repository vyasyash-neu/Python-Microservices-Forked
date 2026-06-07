import os
import logging

from opentelemetry import trace
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
from opentelemetry.sdk.resources import Resource
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor
from opentelemetry.instrumentation.httpx import HTTPXClientInstrumentor
from opentelemetry.instrumentation.logging import LoggingInstrumentor

OTLP_ENDPOINT = os.getenv("OTLP_ENDPOINT", "http://localhost:4317")


def setup_tracing(service_name: str, app):
    """Configure OpenTelemetry: export spans to Tempo, instrument FastAPI + httpx."""
    resource = Resource.create({"service.name": service_name})
    provider = TracerProvider(resource=resource)
    
    exporter = OTLPSpanExporter(endpoint=OTLP_ENDPOINT, insecure=True)
    provider.add_span_processor(BatchSpanProcessor(exporter))
    trace.set_tracer_provider(provider)
    
    # Inject trace_id/span_id into log records FIRST (before app instrumentation)
    LoggingInstrumentor().instrument(set_logging_format=False)
    
    # Auto-instrument FastAPI HTTP server
    FastAPIInstrumentor.instrument_app(app)
    
    # Auto-instrument httpx for outgoing calls (propagates trace context)
    HTTPXClientInstrumentor().instrument()
    
    # Auto-instrument aiokafka for trace propagation across Kafka topics
    try:
        from opentelemetry.instrumentation.aiokafka import AIOKafkaInstrumentor
        AIOKafkaInstrumentor().instrument()
    except ImportError:
        pass

    logging.info(f"Tracing initialized for {service_name} → {OTLP_ENDPOINT}")