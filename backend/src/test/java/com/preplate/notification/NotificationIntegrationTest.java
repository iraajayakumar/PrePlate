package com.preplate.notification;

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
class NotificationIntegrationTest {
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
    void shouldDedupeDinnerReminderAndMarkNotificationsReadWhenRequested() throws Exception {
        mvc.perform(get("/api/notifications").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications[0].type").value("REMINDER"))
                .andExpect(jsonPath("$.notifications[0].message").value(
                        org.hamcrest.Matchers.containsString("2:00 PM (in 20 minutes)")));
        mvc.perform(get("/api/notifications").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications.length()").value(1));

        mvc.perform(put("/api/meals/{mealId}/response", fixture.dinner().getId())
                        .header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ATTENDING\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/notifications").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications[?(@.type=='CONFIRMATION')]").isNotEmpty())
                .andExpect(jsonPath("$.notifications[?(@.type=='OPTIONAL_PREFERENCE')]").isNotEmpty());
        mvc.perform(post("/api/notifications/read-all").header("Authorization", bearer()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/notifications").header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").value(0));
    }

    private String bearer() {
        return "Bearer " + token;
    }
}
