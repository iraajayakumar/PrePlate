package com.preplate.analytics;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfiguration.class)
class ConsumptionAndWasteIntegrationTest {
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
    void shouldSaveConsumptionAndCalculateWasteWhenAdminRecordsActuals() throws Exception {
        Long mealId = fixture.dinner().getId();
        mvc.perform(get("/api/admin/dashboard").param("mealDate", "2026-11-14").param("mealType", "DINNER")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk());
        Long firstDish = fixture.dishes().get(0).getId();
        Long secondDish = fixture.dishes().get(1).getId();
        String body = "{\"mealId\":" + mealId + ",\"records\":["
                + "{\"dishId\":" + firstDish + ",\"preparedQuantity\":10,\"consumedQuantity\":8},"
                + "{\"dishId\":" + secondDish + ",\"preparedQuantity\":10,\"consumedQuantity\":8}]}";
        mvc.perform(post("/api/admin/consumption").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records.length()").value(2));
        mvc.perform(get("/api/admin/analytics/waste").param("days", "14").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.prepared").value(20.0))
                .andExpect(jsonPath("$.totals.consumed").value(16.0))
                .andExpect(jsonPath("$.totals.leftover").value(4.0))
                .andExpect(jsonPath("$.totals.wastePercentage").value(20.0));

        mvc.perform(post("/api/admin/consumption").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mealId\":" + mealId + ",\"records\":[{\"dishId\":" + firstDish
                                + ",\"preparedQuantity\":5,\"consumedQuantity\":6}]}"))
                .andExpect(status().isBadRequest());
    }

    private String bearer() {
        return "Bearer " + adminToken;
    }
}
