package com.amigoscode.inference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class InferenceClient {

    private final RestClient restClient;

    public InferenceClient(
            RestClient.Builder builder,
            @Value("${inference.base-url}") String baseUrl
    ) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .build();
    }

    public Object predict(int userId) {
        return restClient.post()
                .uri("/recommendations")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("user_id", userId))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        (request, response) ->
                                new RuntimeException(
                                        "Inference call failed with status " + response.getStatusCode()
                                )
                )
                .body(Object.class);
    }

    public Object health() {
        return restClient.get()
                .uri("/health")
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        (request, response) ->
                                new RuntimeException(
                                        "Inference health check failed with status " + response.getStatusCode()
                                )
                )
                .body(Object.class);
    }
}
