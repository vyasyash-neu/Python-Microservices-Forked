import logging
import os
import sys
from pathlib import Path

from opentelemetry import trace as _trace
from pythonjsonlogger import jsonlogger

_PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent
LOG_DIR = Path(os.getenv("LOG_DIR", _PROJECT_ROOT / "logs"))


def setup_logging(service_name: str, level: str = "INFO") -> None:
    """JSON logging to stdout + file. Pulls trace_id/span_id from OTel current span."""
    formatter = jsonlogger.JsonFormatter(
        "%(asctime)s %(levelname)s %(name)s %(message)s %(trace_id)s %(span_id)s",
        rename_fields={"asctime": "timestamp", "levelname": "level", "name": "logger"},
    )

    stdout_handler = logging.StreamHandler(sys.stdout)
    stdout_handler.setFormatter(formatter)

    LOG_DIR.mkdir(parents=True, exist_ok=True)
    log_path = LOG_DIR / f"{service_name}.log"
    file_handler = logging.FileHandler(log_path)
    file_handler.setFormatter(formatter)

    class ContextFilter(logging.Filter):
        def filter(self, record):
            record.service = service_name
            span = _trace.get_current_span()
            ctx = span.get_span_context() if span else None
            if ctx and ctx.is_valid:
                record.trace_id = format(ctx.trace_id, "032x")
                record.span_id = format(ctx.span_id, "016x")
            else:
                record.trace_id = None
                record.span_id = None
            return True

    ctx_filter = ContextFilter()
    stdout_handler.addFilter(ctx_filter)
    file_handler.addFilter(ctx_filter)

    root_logger = logging.getLogger()
    root_logger.handlers = [stdout_handler, file_handler]
    root_logger.setLevel(level)

    logging.getLogger("uvicorn.access").setLevel(logging.WARNING)
    logging.getLogger("aiokafka").setLevel(logging.WARNING)
    logging.getLogger("httpx").setLevel(logging.WARNING)

    logging.info(
        f"Logging initialized for {service_name}",
        extra={"log_path": str(log_path)},
    )