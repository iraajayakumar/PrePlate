package com.preplate.intelligence;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.preplate.auth.TokenStore;
import com.preplate.meal.entity.MealResponse;
import com.preplate.meal.repository.MealResponseRepository;
import com.preplate.support.TestClockConfiguration;
import com.preplate.support.TestDataFactory;
import com.preplate.support.TestDataFactory.Fixture;
import com.preplate.common.enums.AttendanceStatus;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfiguration.class)
class DashboardAndForecastIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private TestDataFactory dataFactory;
    @Autowired private TokenStore tokens;
    @Autowired private MealResponseRepository responses;
    private Fixture fixture;
    private String adminToken;

    @BeforeEach
    void setUp() {
        fixture = dataFactory.resetAndCreate();
        adminToken = tokens.create(fixture.admin().getId());
    }

    @Test
    void shouldGenerateHeuristicForecastAndRefreshCountsWhenResponsesChange() throws Exception {
        Long mealId = fixture.dinner().getId();
        mvc.perform(get("/api/admin/dashboard").param("mealDate", "2026-11-14").param("mealType", "DINNER")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mealExists").value(true))
                .andExpect(jsonPath("$.eligibleStudents").value(1))
                .andExpect(jsonPath("$.noResponse").value(1))
                .andExpect(jsonPath("$.forecast.source").value("HEURISTIC"))
                .andExpect(jsonPath("$.dishes.length()").value(2));
        responses.saveAndFlush(new MealResponse(fixture.student().getId(), mealId,
                AttendanceStatus.ATTENDING, Instant.parse("2026-11-14T08:40:00Z")));
        mvc.perform(post("/api/admin/forecasts/{mealId}/refresh", mealId).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedAttendance").value(1))
                .andExpect(jsonPath("$.noResponse").value(0))
                .andExpect(jsonPath("$.forecast.predictedAttendance").value(1));
    }

    @Test
    void shouldReturnEmptyDashboardWhenMealDoesNotExist() throws Exception {
        mvc.perform(get("/api/admin/dashboard").param("mealDate", "2026-11-14").param("mealType", "LUNCH")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mealExists").value(false))
                .andExpect(jsonPath("$.eligibleStudents").value(0))
                .andExpect(jsonPath("$.forecast").doesNotExist());
    }

    private String bearer() {
        return "Bearer " + adminToken;
    }
}
