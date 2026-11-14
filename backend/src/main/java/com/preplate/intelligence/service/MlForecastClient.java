package com.preplate.intelligence.service;

import com.preplate.common.enums.MealType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MlForecastClient {
    private final RestClient restClient;

    public MlForecastClient(@Value("${preplate.forecast.base-url:http://localhost:8000}") String baseUrl,
            @Value("${preplate.forecast.timeout-ms:2000}") int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public MlResponse predict(MlRequest request) {
        return restClient.post().uri("/predict").body(request).retrieve().body(MlResponse.class);
    }

    public record MlDishInput(Long dishId, String name, int interest) {
    }

    public record MlHistoryInput(LocalDate date, int attendance) {
    }

    public record MlRequest(Long mealId, LocalDate mealDate, MealType mealType, String dayOfWeek,
            int eligibleStudents, int confirmed, int notAttending, int uncertain, int noResponse,
            List<MlDishInput> dishes, List<MlHistoryInput> history) {
    }

    public record MlDishOutput(Long dishId, int predictedDemand) {
    }

    public record MlResponse(int predictedAttendance, int lowerBound, int upperBound, double confidence,
            List<com.preplate.common.dto.ApiDtos.ForecastFactor> factors, List<MlDishOutput> dishes) {
    }
}
