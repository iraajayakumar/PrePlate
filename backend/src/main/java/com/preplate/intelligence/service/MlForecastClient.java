package com.preplate.intelligence.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Component
public class MlForecastClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(MlForecastClient.class);
    private static final ObjectMapper RESPONSE_MAPPER = JsonMapper.builder().build();
    private final RestClient restClient;

    @Autowired
    public MlForecastClient(@Value("${preplate.forecast.base-url:http://127.0.0.1:8000}") String baseUrl,
            @Value("${preplate.forecast.timeout-ms:5000}") int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public MlForecastClient(String baseUrl, RestClient.Builder builder) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public Optional<MlResponse> predict(MlRequest request) {
        return forecast(request).response();
    }

    public ForecastAttempt forecast(MlRequest request) {
        try {
            return restClient.post().uri("/forecast/details").body(request).exchange((clientRequest, response) -> {
                int status = response.getStatusCode().value();
                byte[] responseBody = response.getBody().readAllBytes();
                String bodySummary = summarize(new String(responseBody, java.nio.charset.StandardCharsets.UTF_8));
                if (status >= 400) {
                    String reason = status < 500 ? "invalid_request" : "ml_service_unavailable";
                    LOGGER.warn("Forecast service returned HTTP {}: {}", status, bodySummary);
                    return new ForecastAttempt(Optional.empty(), reason);
                }
                try {
                    MlResponse forecast = RESPONSE_MAPPER.readValue(responseBody, MlResponse.class);
                    validate(request, forecast);
                    return new ForecastAttempt(Optional.of(forecast), null);
                } catch (Exception exception) {
                    LOGGER.warn("Forecast service returned an invalid response (HTTP {}): {} ({})",
                            status, bodySummary, exception.getMessage());
                    return new ForecastAttempt(Optional.empty(), "invalid_response");
                }
            });
        } catch (ResourceAccessException exception) {
            LOGGER.warn("Forecast service is unavailable: {}", exception.getMessage());
            return new ForecastAttempt(Optional.empty(), "ml_service_unavailable");
        } catch (RestClientException exception) {
            LOGGER.warn("Forecast service returned an unreadable response: {}", exception.getMessage());
            return new ForecastAttempt(Optional.empty(), "invalid_response");
        }
    }

    private void validate(MlRequest request, MlResponse response) {
        if (response == null || response.forecast() == null) {
            throw new IllegalArgumentException("Forecast details are missing.");
        }
        Forecast forecast = response.forecast();
        if (forecast.mealId() != request.mealId()) {
            throw new IllegalArgumentException("Forecast meal id does not match the request.");
        }
        if (forecast.predictedAttendance() < 0 || forecast.forecastMethod() == null
                || !Set.of("ml", "fallback").contains(forecast.forecastMethod())) {
            throw new IllegalArgumentException("Forecast metadata is invalid.");
        }
        if (forecast.modelVersion() == null || forecast.dishForecasts() == null || response.dishDetails() == null) {
            throw new IllegalArgumentException("Forecast response is incomplete.");
        }
        if (response.dataSource() != null
                && !Set.of("synthetic", "observed").contains(response.dataSource())) {
            throw new IllegalArgumentException("Forecast data source is invalid.");
        }
        validateInterval(response.attendanceInterval(), false);
        Set<Long> requestedDishIds = request.dishes().stream().map(DishInput::dishId)
                .collect(java.util.stream.Collectors.toSet());
        Set<Long> forecastDishIds = new HashSet<>();
        for (DishOutput dish : forecast.dishForecasts()) {
            if (dish == null || dish.dishId() == null || !requestedDishIds.contains(dish.dishId())
                    || !forecastDishIds.add(dish.dishId())) {
                throw new IllegalArgumentException("Dish forecast id is missing, unexpected, or duplicated.");
            }
            if (!finiteNonNegative(dish.predictedQuantity())
                    || !finiteNonNegative(dish.recommendedQuantity())
                    || dish.unit() == null || dish.unit().isBlank()) {
                throw new IllegalArgumentException("Dish forecast quantity or unit is invalid.");
            }
        }
        Set<Long> diagnosticDishIds = new HashSet<>();
        for (DishDetails dish : response.dishDetails()) {
            if (dish == null || dish.dishId() == null || !requestedDishIds.contains(dish.dishId())
                    || !diagnosticDishIds.add(dish.dishId()) || dish.method() == null
                    || !Set.of("ml", "fallback").contains(dish.method())
                    || !finiteNonNegative(dish.bufferPercent())) {
                throw new IllegalArgumentException("Dish diagnostics are invalid.");
            }
            validateInterval(dish.interval(), true);
        }
        if (!forecastDishIds.equals(requestedDishIds) || !diagnosticDishIds.equals(requestedDishIds)) {
            throw new IllegalArgumentException("Forecast response is missing a requested dish.");
        }
    }

    private void validateInterval(PredictionInterval interval, boolean nullable) {
        if (interval == null) {
            if (!nullable) {
                throw new IllegalArgumentException("Attendance interval is missing.");
            }
            return;
        }
        if (!finiteNonNegative(interval.lower()) || !finiteNonNegative(interval.upper())
                || interval.targetCoverage() == null || !Double.isFinite(interval.targetCoverage())
                || interval.targetCoverage() < 0 || interval.targetCoverage() > 1
                || interval.lower() > interval.upper()) {
            throw new IllegalArgumentException("Prediction interval is invalid.");
        }
    }

    private boolean finiteNonNegative(Double value) {
        return value != null && Double.isFinite(value) && value >= 0;
    }

    private String summarize(String body) {
        if (body == null || body.isBlank()) {
            return "<empty body>";
        }
        String compact = body.replaceAll("\\s+", " ").trim();
        return compact.length() <= 300 ? compact : compact.substring(0, 300) + "...";
    }

    public record ForecastAttempt(Optional<MlResponse> response, String failureReason) {
    }

    public record MlRequest(@JsonProperty("meal_id") long mealId,
            @JsonProperty("meal_type") String mealType,
            @JsonProperty("day_of_week") int dayOfWeek,
            @JsonProperty("eligible_students") int eligibleStudents,
            Responses responses, Historical historical, List<DishInput> dishes) {
    }

    public record Responses(int yes, int no, int unsure,
            @JsonProperty("no_response") int noResponse) {
    }

    public record Historical(@JsonProperty("avg_attendance") double avgAttendance,
            @JsonProperty("avg_waste_percentage") double avgWastePercentage) {
    }

    public record DishInput(@JsonProperty("dish_id") Long dishId,
            @JsonProperty("student_interest") int studentInterest,
            @JsonProperty("historical_popularity") double historicalPopularity,
            @JsonProperty("avg_consumption_quantity") double avgConsumptionQuantity, String unit) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MlResponse(Forecast forecast,
            @JsonProperty("data_source") String dataSource,
            @JsonProperty("attendance_algorithm") String attendanceAlgorithm,
            @JsonProperty("attendance_interval") PredictionInterval attendanceInterval,
            @JsonProperty("dish_details") List<DishDetails> dishDetails,
            @JsonProperty("fallback_reason") String fallbackReason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Forecast(@JsonProperty("meal_id") long mealId,
            @JsonProperty("predicted_attendance") int predictedAttendance,
            @JsonProperty("dish_forecasts") List<DishOutput> dishForecasts,
            @JsonProperty("model_version") String modelVersion,
            @JsonProperty("forecast_method") String forecastMethod) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DishOutput(@JsonProperty("dish_id") Long dishId,
            @JsonProperty("predicted_quantity") Double predictedQuantity,
            @JsonProperty("recommended_quantity") Double recommendedQuantity, String unit) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PredictionInterval(Double lower, Double upper,
            @JsonProperty("target_coverage") Double targetCoverage) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DishDetails(@JsonProperty("dish_id") Long dishId, String method, String algorithm,
            PredictionInterval interval, @JsonProperty("buffer_percent") Double bufferPercent, String note) {
    }
}
