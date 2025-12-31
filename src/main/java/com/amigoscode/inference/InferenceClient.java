package com.amigoscode.inference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class InferenceClient {

    private final RestClient restClient;

    public InferenceClient(@Value("${inference.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public Object predict(int userId) {
        return restClient.post()
                .uri("/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("user_id", userId))
                .retrieve()
                .body(Object.class);
    }
    public Object health() {
        return restClient.get()
                .uri("/health")
                .retrieve()
                .body(Object.class);
    }

}

