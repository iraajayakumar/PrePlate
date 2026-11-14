package com.preplate.intelligence;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.preplate.analytics.entity.ConsumptionRecord;
import com.preplate.analytics.repository.ConsumptionRecordRepository;
import com.preplate.auth.TokenStore;
import com.preplate.common.enums.AttendanceStatus;
import com.preplate.common.enums.MealType;
import com.preplate.intelligence.entity.DishForecast;
import com.preplate.intelligence.entity.MealForecast;
import com.preplate.intelligence.repository.DishForecastRepository;
import com.preplate.intelligence.repository.MealForecastRepository;
import com.preplate.intelligence.support.MockMlForecastConfiguration;
import com.preplate.intelligence.support.MockMlForecastService;
import com.preplate.meal.entity.DishPreference;
import com.preplate.meal.entity.Meal;
import com.preplate.meal.entity.MealResponse;
import com.preplate.meal.repository.DishPreferenceRepository;
import com.preplate.meal.repository.MealRepository;
import com.preplate.meal.repository.MealResponseRepository;
import com.preplate.support.TestClockConfiguration;
import com.preplate.support.TestDataFactory;
import com.preplate.support.TestDataFactory.Fixture;
import com.preplate.user.entity.User;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestClockConfiguration.class, MockMlForecastConfiguration.class})
@TestPropertySource(properties = {
        "preplate.forecast.enabled=true",
        "preplate.forecast.base-url=http://127.0.0.1:8000",
        "preplate.forecast.timeout-ms=100"
})
class MlForecastIntegrationTest {
    private static final String FORECAST_URL = "http://127.0.0.1:8000/forecast/details";
    private static final Instant RESPONSE_TIME = Instant.parse("2026-11-14T08:40:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 14);

    @Autowired private MockMvc mvc;
    @Autowired private TestDataFactory dataFactory;
    @Autowired private TokenStore tokens;
    @Autowired private MockMlForecastService mockMl;
    @Autowired private MealRepository meals;
    @Autowired private MealResponseRepository responses;
    @Autowired private DishPreferenceRepository preferences;
    @Autowired private ConsumptionRecordRepository consumption;
    @Autowired private MealForecastRepository mealForecasts;
    @Autowired private DishForecastRepository dishForecasts;
    private Fixture fixture;
    private String adminToken;
    private String studentToken;

    @BeforeEach
    void setUp() {
        mockMl.server().reset();
        fixture = dataFactory.resetAndCreate();
        adminToken = tokens.create(fixture.admin().getId());
        studentToken = tokens.create(fixture.student().getId());
    }

    @Test
    void shouldStoreMlForecastWithDecimalDishValuesAndReturnItOnDashboard() throws Exception {
        addCompletedHistory();
        List<Long> dishIds = dinnerDishIds();
        mockMl.server().expect(requestTo(FORECAST_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(expectedRequest(false)))
                .andRespond(withSuccess(detailsResponse(fixture.dinner().getId(), dishIds, "ml", null),
                        MediaType.APPLICATION_JSON));

        mvc.perform(get("/api/admin/dashboard").param("mealDate", TODAY.toString()).param("mealType", "DINNER")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forecast.source").value("ML_SERVICE"))
                .andExpect(jsonPath("$.forecast.modelVersion").value("preplate-ml-v1-e1581e0c93"))
                .andExpect(jsonPath("$.forecast.forecastMethod").value("ml"))
                .andExpect(jsonPath("$.forecast.dataSource").value("synthetic"))
                .andExpect(jsonPath("$.forecast.lowerBound").value(0))
                .andExpect(jsonPath("$.forecast.upperBound").value(1))
                .andExpect(jsonPath("$.dishes[0].predictedDemand").value(243.96))
                .andExpect(jsonPath("$.dishes[0].recommendedPreparation").value(256.16))
                .andExpect(jsonPath("$.dishes[0].unit").value("servings"))
                .andExpect(jsonPath("$.dishes[0].method").value("ml"))
                .andExpect(jsonPath("$.dishes[0].note").value("sample dish diagnostic"));

        MealForecast stored = mealForecasts.findByMealId(fixture.dinner().getId()).orElseThrow();
        DishForecast storedDish = dishForecasts.findByMealId(fixture.dinner().getId()).get(0);
        org.junit.jupiter.api.Assertions.assertEquals(1, stored.getPredictedAttendance());
        org.junit.jupiter.api.Assertions.assertEquals("ML_SERVICE", stored.getSource().name());
        org.junit.jupiter.api.Assertions.assertEquals("preplate-ml-v1-e1581e0c93", stored.getModelVersion());
        org.junit.jupiter.api.Assertions.assertEquals(new java.math.BigDecimal("256.16"),
                storedDish.getRecommendedPreparation());
        mockMl.server().verify();
    }

    @Test
    void shouldLabelFallbackForecastReturnedByMlService() throws Exception {
        addCompletedHistory();
        mockMl.server().expect(requestTo(FORECAST_URL))
                .andRespond(withSuccess(detailsResponse(fixture.dinner().getId(), dinnerDishIds(),
                        "fallback", "model_bundle_missing"), MediaType.APPLICATION_JSON));

        mvc.perform(post("/api/admin/forecasts/{mealId}/refresh", fixture.dinner().getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forecast.source").value("ML_FALLBACK"))
                .andExpect(jsonPath("$.forecast.forecastMethod").value("fallback"))
                .andExpect(jsonPath("$.forecast.fallbackReason").value("model_bundle_missing"));
        mockMl.server().verify();
    }

    @Test
    void shouldSkipMlAndExplainZeroPlaceholderWhenHistoryIsMissing() throws Exception {
        mvc.perform(get("/api/admin/dashboard").param("mealDate", TODAY.toString()).param("mealType", "DINNER")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forecast.source").value("HEURISTIC"))
                .andExpect(jsonPath("$.forecast.fallbackReason").value("insufficient_history"))
                .andExpect(jsonPath("$.dishes[0].predictedDemand").value(0.0))
                .andExpect(jsonPath("$.dishes[0].note")
                        .value("No dish consumption history. Zero is a placeholder, not estimated zero demand."));
        mockMl.server().verify();
    }

    @Test
    void shouldSendUpdatedResponseCountsWhenForecastIsRefreshed() throws Exception {
        addCompletedHistory();
        responses.saveAndFlush(new MealResponse(fixture.student().getId(), fixture.dinner().getId(),
                AttendanceStatus.ATTENDING, RESPONSE_TIME));
        mockMl.server().expect(requestTo(FORECAST_URL))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers
                        .jsonPath("$.responses.yes", is(1)))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers
                        .jsonPath("$.responses.no_response", is(0)))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers
                        .jsonPath("$.responses.no", is(0)))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers
                        .jsonPath("$.responses.unsure", is(0)))
                .andRespond(withSuccess(detailsResponse(fixture.dinner().getId(), dinnerDishIds(), "ml", null),
                        MediaType.APPLICATION_JSON));

        mvc.perform(post("/api/admin/forecasts/{mealId}/refresh", fixture.dinner().getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedAttendance").value(1))
                .andExpect(jsonPath("$.noResponse").value(0));
        mockMl.server().verify();
    }

    @Test
    void shouldUseHeuristicForHttpErrorsTimeoutConnectionAndInvalidResponse() throws Exception {
        addCompletedHistory();
        List<FailureResponse> failures = List.of(
                new FailureResponse(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).body("bad input"), "invalid_request"),
                new FailureResponse(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("server fault"),
                        "ml_service_unavailable"),
                new FailureResponse(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withException(new SocketTimeoutException("timed out")), "ml_service_unavailable"),
                new FailureResponse(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withException(new ConnectException("connection refused")), "ml_service_unavailable"),
                new FailureResponse(withSuccess("{invalid", MediaType.APPLICATION_JSON), "invalid_response"));

        for (FailureResponse failure : failures) {
            mockMl.server().reset();
            mockMl.server().expect(requestTo(FORECAST_URL))
                    .andRespond(failure.response());
            mvc.perform(post("/api/admin/forecasts/{mealId}/refresh", fixture.dinner().getId())
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.forecast.source").value("HEURISTIC"))
                    .andExpect(jsonPath("$.forecast.fallbackReason").value(failure.reason()));
            mockMl.server().verify();
        }
    }

    @Test
    void shouldKeepStudentCheckInAvailableWhenMlServiceIsDown() throws Exception {
        addCompletedHistory();
        mockMl.server().expect(requestTo(FORECAST_URL))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("offline"));

        mvc.perform(post("/api/admin/forecasts/{mealId}/refresh", fixture.dinner().getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forecast.source").value("HEURISTIC"));
        mvc.perform(put("/api/meals/{mealId}/response", fixture.dinner().getId())
                        .header("Authorization", bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ATTENDING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATTENDING"));
        mockMl.server().verify();
    }

    private void addCompletedHistory() {
        User student = fixture.student();
        List<com.preplate.meal.entity.Dish> dishList = fixture.dishes().subList(0, 2);
        for (int day = 11; day <= 13; day++) {
            Meal previous = meals.saveAndFlush(new Meal(LocalDate.of(2026, 11, day), MealType.DINNER,
                    new LinkedHashSet<>(dishList)));
            responses.saveAndFlush(new MealResponse(student.getId(), previous.getId(),
                    AttendanceStatus.ATTENDING, RESPONSE_TIME));
            preferences.save(new DishPreference(student.getId(), previous.getId(), dishList.get(0).getId()));
            List<ConsumptionRecord> rows = new ArrayList<>();
            for (com.preplate.meal.entity.Dish dish : dishList) {
                rows.add(new ConsumptionRecord(previous.getId(), dish.getId(), null, 10, 8, RESPONSE_TIME));
            }
            consumption.saveAllAndFlush(rows);
        }
    }

    private List<Long> dinnerDishIds() {
        return fixture.dinner().getDishes().stream().map(com.preplate.meal.entity.Dish::getId).sorted().toList();
    }

    private String expectedRequest(boolean yes) {
        List<Long> ids = dinnerDishIds();
        return """
                {
                  "meal_id": %d,
                  "meal_type": "dinner",
                  "day_of_week": 5,
                  "eligible_students": 1,
                  "responses": {"yes": %d, "no": 0, "unsure": 0, "no_response": %d},
                  "historical": {"avg_attendance": 1.0, "avg_waste_percentage": 20.0},
                  "dishes": [
                    {"dish_id": %d, "student_interest": 0, "historical_popularity": 1.0,
                     "avg_consumption_quantity": 8.0, "unit": "servings"},
                    {"dish_id": %d, "student_interest": 0, "historical_popularity": 0.0,
                     "avg_consumption_quantity": 8.0, "unit": "servings"}
                  ]
                }
                """.formatted(fixture.dinner().getId(), yes ? 1 : 0, yes ? 0 : 1, ids.get(0), ids.get(1));
    }

    private String detailsResponse(Long mealId, List<Long> dishIds, String method, String fallbackReason) {
        String forecastDishes = dishIds.stream().map(id -> """
                {"dish_id": %d, "predicted_quantity": 243.96, "recommended_quantity": 256.16, "unit": "servings"}
                """.formatted(id)).collect(java.util.stream.Collectors.joining(","));
        String diagnosticDishes = dishIds.stream().map(id -> """
                {"dish_id": %d, "method": "ml", "algorithm": "linear_regression",
                 "interval": {"lower": 215.11, "upper": 272.81, "target_coverage": 0.9},
                 "buffer_percent": 5.0, "note": "sample dish diagnostic"}
                """.formatted(id)).collect(java.util.stream.Collectors.joining(","));
        String source = "fallback".equals(method) ? "null" : "\"synthetic\"";
        String fallback = fallbackReason == null ? "null" : "\"" + fallbackReason + "\"";
        return """
                {
                  "forecast": {
                    "meal_id": %d, "predicted_attendance": 1,
                    "dish_forecasts": [%s],
                    "model_version": "preplate-ml-v1-e1581e0c93", "forecast_method": "%s"
                  },
                  "data_source": %s, "attendance_algorithm": "linear_regression",
                  "attendance_interval": {"lower": 0, "upper": 1, "target_coverage": 0.9},
                  "dish_details": [%s], "fallback_reason": %s
                }
                """.formatted(mealId, forecastDishes, method, source, diagnosticDishes, fallback);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record FailureResponse(org.springframework.test.web.client.ResponseCreator response, String reason) {
    }
}
