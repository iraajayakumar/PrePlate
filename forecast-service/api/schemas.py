from typing import Literal
from pydantic import BaseModel, Field, model_validator

class Responses(BaseModel):
    yes: int = Field(ge=0)
    no: int = Field(ge=0)
    unsure: int = Field(ge=0)
    no_response: int = Field(ge=0)

class Historical(BaseModel):
    avg_attendance: float = Field(ge=0, allow_inf_nan=False)
    avg_waste_percentage: float = Field(ge=0, le=100, allow_inf_nan=False)

class DishInput(BaseModel):
    dish_id: int
    student_interest: int = Field(ge=0)
    historical_popularity: float = Field(ge=0, le=1, allow_inf_nan=False)
    avg_consumption_quantity: float = Field(ge=0, allow_inf_nan=False)
    unit: str = Field(min_length=1)

class ForecastRequest(BaseModel):
    meal_id: int
    meal_type: Literal['breakfast', 'lunch', 'dinner', 'snacks']
    day_of_week: int = Field(ge=0, le=6)
    eligible_students: int = Field(gt=0)
    responses: Responses
    historical: Historical
    dishes: list[DishInput]

    @model_validator(mode='after')
    def validate_counts(self):
        r = self.responses
        if r.yes + r.no + r.unsure + r.no_response != self.eligible_students:
            raise ValueError('response counts must sum to eligible_students')
        if any(d.student_interest > self.eligible_students for d in self.dishes):
            raise ValueError('dish student_interest cannot exceed eligible_students')
        if len({d.dish_id for d in self.dishes}) != len(self.dishes):
            raise ValueError('dish IDs must be unique within a meal')
        return self

class DishForecast(BaseModel):
    dish_id: int
    predicted_quantity: float = Field(ge=0, allow_inf_nan=False)
    recommended_quantity: float = Field(ge=0, allow_inf_nan=False)
    unit: str

class ForecastResponse(BaseModel):
    meal_id: int
    predicted_attendance: int = Field(ge=0)
    dish_forecasts: list[DishForecast]
    model_version: str
    forecast_method: Literal['ml', 'fallback']


class PredictionInterval(BaseModel):
    lower: float
    upper: float
    target_coverage: float = 0.90

class DishDiagnostics(BaseModel):
    dish_id: int
    method: Literal["ml", "fallback"]
    algorithm: str
    interval: PredictionInterval | None
    buffer_percent: float
    note: str | None = None

class ForecastDetails(BaseModel):
    forecast: ForecastResponse
    data_source: Literal["synthetic", "observed"] | None
    attendance_algorithm: str
    attendance_interval: PredictionInterval | None
    dish_details: list[DishDiagnostics]
    fallback_reason: str | None
