package com.preplate.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestClockConfiguration {
    @Bean
    @Primary
    public Clock testClock() {
        return Clock.fixed(Instant.parse("2026-11-14T08:10:00Z"), ZoneId.of("Asia/Kolkata"));
    }
}
