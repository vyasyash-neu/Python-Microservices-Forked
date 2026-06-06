import json
import logging
from typing import Optional
from aiokafka import AIOKafkaProducer
from app.config import settings

logger = logging.getLogger(__name__)

_producer: Optional[AIOKafkaProducer] = None


async def start_producer():
    global _producer
    _producer = AIOKafkaProducer(
        bootstrap_servers=settings.KAFKA_BOOTSTRAP_SERVERS,
        value_serializer=lambda v: json.dumps(v, default=str).encode("utf-8"),
    )
    await _producer.start()
    logger.info("✅ Product Service Kafka producer started")


async def stop_producer():
    global _producer
    if _producer:
        await _producer.stop()
        logger.info("🔌 Product Service Kafka producer stopped")


async def publish_event(topic: str, payload: dict):
    """
    Fire-and-forget event publish.
    Logs and swallows failures so product writes never fail because of Kafka.
    The reconciliation job in Search Service is the safety net.
    """
    if not _producer:
        logger.warning("Kafka producer not started — skipping event publish")
        return

    try:
        await _producer.send_and_wait(topic, value=payload)
        logger.info(f"📤 Published to '{topic}': {payload.get('event_type')} for product {payload.get('product_id')}")
    except Exception as e:
        logger.error(f"❌ Failed to publish to '{topic}': {e}")