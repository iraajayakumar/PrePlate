package com.preplate.intelligence.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@Tag("unit")
class MlForecastClientTest {
    private MockRestServiceServer server;
    private MlForecastClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new MlForecastClient("http://127.0.0.1:8000", builder);
    }

    @Test
    void shouldSendSnakeCaseRequestAndParseDetailsFixture() throws IOException {
        server.expect(requestTo("http://127.0.0.1:8000/forecast/details"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(fixture("sample-request.json")
                        .replace("\"avg_consumption_quantity\": 205,", "\"avg_consumption_quantity\": 205.0,")))
                .andRespond(withSuccess(fixture("sample-details-response.json"), MediaType.APPLICATION_JSON));

        MlForecastClient.ForecastAttempt attempt = client.forecast(sampleRequest());

        assertTrue(attempt.response().isPresent());
        MlForecastClient.MlResponse response = attempt.response().orElseThrow();
        assertEquals(142, response.forecast().mealId());
        assertEquals(3807, response.forecast().predictedAttendance());
        assertEquals(3640.0, response.attendanceInterval().lower());
        assertEquals(3974.0, response.attendanceInterval().upper());
        assertEquals(256.16, response.forecast().dishForecasts().get(0).recommendedQuantity());
        assertEquals("preplate-ml-v1-e1581e0c93", response.forecast().modelVersion());
        assertEquals("synthetic", response.dataSource());
        server.verify();
    }

    @Test
    void shouldReturnEmptyWithInvalidResponseReasonWhenDishIsMissing() throws IOException {
        MlForecastClient.MlRequest request = new MlForecastClient.MlRequest(142, "lunch", 4, 5000,
                new MlForecastClient.Responses(3100, 600, 400, 900),
                new MlForecastClient.Historical(3650, 6.2),
                List.of(new MlForecastClient.DishInput(13L, 2600, 0.82, 205, "kg")));
        server.expect(requestTo("http://127.0.0.1:8000/forecast/details"))
                .andRespond(withSuccess(fixture("sample-details-response.json"), MediaType.APPLICATION_JSON));

        MlForecastClient.ForecastAttempt attempt = client.forecast(request);

        assertTrue(attempt.response().isEmpty());
        assertEquals("invalid_response", attempt.failureReason());
        server.verify();
    }

    @Test
    void shouldReturnEmptyWithInvalidResponseReasonWhenJsonIsMalformed() {
        server.expect(requestTo("http://127.0.0.1:8000/forecast/details"))
                .andRespond(withSuccess("{invalid", MediaType.APPLICATION_JSON));

        MlForecastClient.ForecastAttempt attempt = client.forecast(sampleRequest());

        assertTrue(attempt.response().isEmpty());
        assertEquals("invalid_response", attempt.failureReason());
        server.verify();
    }

    @Test
    void shouldRejectResponseWithoutAttendanceInterval() throws IOException {
        String response = fixture("sample-details-response.json")
                .replaceFirst("(?s)\"attendance_interval\"\\s*:\\s*\\{.*?\\},\\s*", "");
        server.expect(requestTo("http://127.0.0.1:8000/forecast/details"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        MlForecastClient.ForecastAttempt attempt = client.forecast(sampleRequest());

        assertTrue(attempt.response().isEmpty());
        assertEquals("invalid_response", attempt.failureReason());
        server.verify();
    }

    private MlForecastClient.MlRequest sampleRequest() {
        return new MlForecastClient.MlRequest(142, "lunch", 4, 5000,
                new MlForecastClient.Responses(3100, 600, 400, 900),
                new MlForecastClient.Historical(3650, 6.2),
                List.of(new MlForecastClient.DishInput(12L, 2600, 0.82, 205, "kg")));
    }

    private String fixture(String name) throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/ml/" + name)) {
            if (input == null) {
                throw new IOException("Missing ML fixture: " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
