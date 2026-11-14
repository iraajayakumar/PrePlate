package com.preplate.intelligence.support;

import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

public class MockMlForecastService {
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    public RestClient.Builder builder() {
        return builder;
    }

    public MockRestServiceServer server() {
        return server;
    }
}
