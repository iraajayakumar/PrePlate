package com.preplate.meal;

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
class StudentMealFlowIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private TestDataFactory dataFactory;
    @Autowired private TokenStore tokens;
    private Fixture fixture;
    private String token;

    @BeforeEach
    void setUp() {
        fixture = dataFactory.resetAndCreate();
        token = tokens.create(fixture.student().getId());
    }

    @Test
    void shouldRecordMealResponseAndDishPreferencesWhenStudentConfirms() throws Exception {
        Long mealId = fixture.dinner().getId();
        mvc.perform(get("/api/meals/upcoming").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serverNow").exists())
                .andExpect(jsonPath("$.meals[?(@.mealId==" + mealId + ")]").isNotEmpty());

        mvc.perform(put("/api/meals/{mealId}/response", mealId).header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ATTENDING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATTENDING"));
        Long dishId = fixture.dishes().get(0).getId();
        mvc.perform(put("/api/meals/{mealId}/dish-preferences", mealId).header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dishIds\":[" + dishId + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dishIds[0]").value(dishId));
        mvc.perform(get("/api/meals/upcoming").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meals[?(@.mealId==" + mealId + ")].selectedDishIds[0]")
                        .value(dishId.intValue()));
    }

    @Test
    void shouldShowHistoryAndSeparateSelectedFromAttendedWhenStudentResponds() throws Exception {
        Long mealId = fixture.dinner().getId();
        mvc.perform(put("/api/meals/{mealId}/response", mealId).header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ATTENDING\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/student/history?period=TODAY").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories.length()").value(4))
                .andExpect(jsonPath("$.categories[?(@.mealType=='DINNER')].entries[0].status")
                        .value("ATTENDING"));
        mvc.perform(get("/api/student/analytics?scope=THIS_MONTH").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participation[?(@.mealType=='DINNER')].selected").value(1))
                .andExpect(jsonPath("$.participation[?(@.mealType=='DINNER')].attended").value(0));
    }

    @Test
    void shouldRejectDishPreferencesBeforeAttendanceOrForAnotherMeal() throws Exception {
        Long mealId = fixture.dinner().getId();
        Long dishFromAnotherMeal = fixture.dishes().get(2).getId();
        mvc.perform(put("/api/meals/{mealId}/dish-preferences", mealId).header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dishIds\":[" + fixture.dishes().get(0).getId() + "]}"))
                .andExpect(status().isConflict());

        mvc.perform(put("/api/meals/{mealId}/response", mealId).header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ATTENDING\"}"))
                .andExpect(status().isOk());

        mvc.perform(put("/api/meals/{mealId}/dish-preferences", mealId).header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dishIds\":[" + dishFromAnotherMeal + "]}"))
                .andExpect(status().isBadRequest());
    }

    private String bearer() {
        return "Bearer " + token;
    }
}
