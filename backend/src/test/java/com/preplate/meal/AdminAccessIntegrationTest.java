package com.preplate.meal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.preplate.auth.TokenStore;
import com.preplate.support.TestClockConfiguration;
import com.preplate.support.TestDataFactory;
import com.preplate.support.TestDataFactory.Fixture;
import java.time.LocalDate;
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
class AdminAccessIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private TestDataFactory dataFactory;
    @Autowired private TokenStore tokens;
    private Fixture fixture;
    private String studentToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        fixture = dataFactory.resetAndCreate();
        studentToken = tokens.create(fixture.student().getId());
        adminToken = tokens.create(fixture.admin().getId());
    }

    @Test
    void shouldDenyStudentAndAllowAdminWhenAccessingAdminRoutes() throws Exception {
        mvc.perform(get("/api/admin/dishes").header("Authorization", bearer(studentToken)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/dishes").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8));
    }

    @Test
    void shouldCreateUpdateAndDeleteMealWhenAdminUsesValidDishIds() throws Exception {
        Long firstDish = fixture.dishes().get(0).getId();
        Long secondDish = fixture.dishes().get(1).getId();
        LocalDate date = LocalDate.of(2026, 11, 16);
        String request = "{\"mealDate\":\"" + date + "\",\"mealType\":\"DINNER\",\"dishIds\":[" + firstDish + "]}";
        String created = mvc.perform(post("/api/admin/meals").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mealType").value("DINNER"))
                .andReturn().getResponse().getContentAsString();
        Long mealId = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(created).get("mealId").asLong();

        mvc.perform(post("/api/admin/meals").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/admin/meals/{mealId}", mealId).header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dishIds\":[" + secondDish + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dishes[0].id").value(secondDish));
        mvc.perform(delete("/api/admin/meals/{mealId}", mealId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Deleted"));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
