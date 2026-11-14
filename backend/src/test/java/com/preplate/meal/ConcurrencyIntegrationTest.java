package com.preplate.meal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.preplate.auth.TokenStore;
import com.preplate.common.enums.AttendanceStatus;
import com.preplate.meal.repository.MealResponseRepository;
import com.preplate.support.TestClockConfiguration;
import com.preplate.support.TestDataFactory;
import com.preplate.support.TestDataFactory.Fixture;
import com.preplate.common.enums.Role;
import com.preplate.user.entity.User;
import com.preplate.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
class ConcurrencyIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private TestDataFactory dataFactory;
    @Autowired private TokenStore tokens;
    @Autowired private MealResponseRepository responses;
    @Autowired private UserRepository users;
    private Fixture fixture;
    private String token;

    @BeforeEach
    void setUp() {
        fixture = dataFactory.resetAndCreate();
        token = tokens.create(fixture.student().getId());
    }

    @Test
    void shouldKeepOneResponseWhenSameStudentRespondsConcurrently() throws Exception {
        int requestCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        try {
            for (int index = 0; index < requestCount; index++) {
                final int responseIndex = index;
                results.add(executor.submit(() -> {
                    start.await();
                    String status = responseIndex % 2 == 0 ? "ATTENDING" : "NOT_SURE";
                    return mvc.perform(put("/api/meals/{mealId}/response", fixture.dinner().getId())
                                    .header("Authorization", "Bearer " + token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"status\":\"" + status + "\"}"))
                            .andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            for (Future<Integer> result : results) {
                assertEquals(200, result.get());
            }
        } finally {
            executor.shutdownNow();
        }
        assertEquals(1, responses.findByMealId(fixture.dinner().getId()).size());
    }

    @Test
    void shouldPersistConcurrentResponsesFromFiftyDistinctStudents() throws Exception {
        List<User> students = new ArrayList<>();
        students.add(fixture.student());
        for (int index = 2; index <= 50; index++) {
            students.add(new User("CONCURRENT" + index, "Concurrent Student " + index, "unused",
                    Role.STUDENT, "CSE", "Kaveri", String.valueOf(100 + index)));
        }
        users.saveAllAndFlush(students.subList(1, students.size()));

        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        try {
            for (User student : students) {
                String studentToken = tokens.create(student.getId());
                results.add(executor.submit(() -> {
                    start.await();
                    return mvc.perform(put("/api/meals/{mealId}/response", fixture.dinner().getId())
                                    .header("Authorization", "Bearer " + studentToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"status\":\"ATTENDING\"}"))
                            .andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            for (Future<Integer> result : results) {
                assertEquals(200, result.get());
            }
        } finally {
            executor.shutdownNow();
        }
        assertEquals(50, responses.findByMealId(fixture.dinner().getId()).size());
    }
}
