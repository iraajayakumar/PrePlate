package com.preplate.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import tools.jackson.databind.ObjectMapper;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfiguration.class)
class AuthIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private TestDataFactory dataFactory;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TokenStore tokens;
    private Fixture fixture;

    @BeforeEach
    void setUp() {
        fixture = dataFactory.resetAndCreate();
    }

    @Test
    void shouldRegisterAndReadCurrentUserWhenTokenIsValid() throws Exception {
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"registrationNumber":"NEW001","fullName":"New Student","department":"CSE",
                                 "hostelName":"Kaveri","hostelRoom":"202","password":"student123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.registrationNumber").value("NEW001"))
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(response).get("token").asString();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("New Student"));
    }

    @Test
    void shouldRejectInvalidLoginAndDuplicateRegistrationWhenCredentialsAreInvalid() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"registrationNumber\":\"TEST001\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid registration number or password"));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"registrationNumber":"TEST001","fullName":"Duplicate","department":"CSE",
                                 "hostelName":"Kaveri","hostelRoom":"101","password":"student123"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRequireTokenWhenAccessingPrivateApi() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Please sign in again."));
        mvc.perform(get("/api/admin/dishes").header("Authorization", "Bearer "
                        + tokens.create(fixture.student().getId())))
                .andExpect(status().isForbidden());
    }
}
