package com.preplate.intelligence;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.preplate.auth.TokenStore;
import com.preplate.support.TestClockConfiguration;
import com.preplate.support.TestDataFactory;
import com.preplate.support.TestDataFactory.Fixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfiguration.class)
@TestPropertySource(properties = {
        "preplate.forecast.enabled=true",
        "preplate.forecast.base-url=http://127.0.0.1:1",
        "preplate.forecast.timeout-ms=100"
})
class MlForecastFallbackIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private TestDataFactory dataFactory;
    @Autowired private TokenStore tokens;
    private Fixture fixture;
    private String adminToken;

    @BeforeEach
    void setUp() {
        fixture = dataFactory.resetAndCreate();
        adminToken = tokens.create(fixture.admin().getId());
    }

    @Test
    void shouldUseHeuristicWhenMlForecastServiceIsUnreachable() throws Exception {
        mvc.perform(post("/api/admin/forecasts/{mealId}/refresh", fixture.dinner().getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forecast.source").value("HEURISTIC"))
                .andExpect(jsonPath("$.forecast.predictedAttendance").isNumber());
    }
}
