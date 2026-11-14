package com.preplate.intelligence.support;

import com.preplate.intelligence.service.MlForecastClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class MockMlForecastConfiguration {
    @Bean
    MockMlForecastService mockMlForecastService() {
        return new MockMlForecastService();
    }

    @Bean
    @Primary
    MlForecastClient mockMlForecastClient(MockMlForecastService service,
            @Value("${preplate.forecast.base-url:http://127.0.0.1:8000}") String baseUrl) {
        return new MlForecastClient(baseUrl, service.builder());
    }
}
