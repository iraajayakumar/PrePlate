from contextlib import asynccontextmanager
import json
import os
from pathlib import Path

from fastapi import Body, FastAPI, Request

from api.schemas import ForecastDetails, ForecastRequest, ForecastResponse
from models.bundle import DEFAULT_BUNDLE
from services.forecast import ForecastEngine, Settings, load_bundle

ROOT = Path(__file__).resolve().parent
SAMPLE = json.loads((ROOT / "sample-request.json").read_text(encoding="utf-8"))

def create_app(model_path=None, settings=None):
    @asynccontextmanager
    async def lifespan(app):
        policy = settings or Settings(
            buffer=float(os.getenv("PREPLATE_SAFETY_BUFFER", "0.05")),
            max_buffer=float(os.getenv("PREPLATE_MAX_SAFETY_BUFFER", "0.25")),
            buffer_mode=os.getenv("PREPLATE_BUFFER_MODE", "fixed"),
        )
        path = Path(model_path if model_path is not None else
                    os.getenv("PREPLATE_MODEL_PATH", str(ROOT / "models" / DEFAULT_BUNDLE)))
        bundle, reason = load_bundle(path)
        app.state.forecaster = ForecastEngine(bundle, reason, policy)
        yield

    app = FastAPI(title="PrePlate Forecasting Service", version="1.0.0", lifespan=lifespan)

    @app.get("/health")
    def health():
        return {"status": "ok", "service": "preplate-ml"}

    @app.post("/forecast", response_model=ForecastResponse)
    def forecast(request: Request, payload: ForecastRequest = Body(openapi_examples={
        "lunch": {"summary": "Valid sample meal", "value": SAMPLE},
    })):
        return request.app.state.forecaster.predict(payload).forecast

    @app.post("/forecast/details", response_model=ForecastDetails)
    def forecast_details(request: Request, payload: ForecastRequest = Body(openapi_examples={
        "lunch": {"summary": "Valid sample meal", "value": SAMPLE},
    })):
        return request.app.state.forecaster.predict(payload)

    return app

app = create_app()
