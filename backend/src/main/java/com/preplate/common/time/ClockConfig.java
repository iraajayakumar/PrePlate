package com.preplate.common.time;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {
    @Bean
    public Clock clock(
            @Value("${preplate.timezone:Asia/Kolkata}") String timezone,
            @Value("${preplate.dev.simulated-now:}") String simulatedNow) {
        ZoneId zone = ZoneId.of(timezone);
        Clock systemClock = Clock.system(zone);
        if (simulatedNow == null || simulatedNow.isBlank()) {
            return systemClock;
        }
        Instant target = LocalDateTime.parse(simulatedNow).atZone(zone).toInstant();
        return Clock.offset(systemClock, Duration.between(Instant.now(), target));
    }
}
