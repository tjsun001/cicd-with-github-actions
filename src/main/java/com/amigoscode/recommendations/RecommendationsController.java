package com.amigoscode.recommendations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class RecommendationsController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String inferenceBaseUrl;

    // Demo-safe fallback list (swap these with your real popular IDs if you want)
    private static final List<Integer> FALLBACK_POPULAR = List.of(1, 2, 3, 101, 102);

    public RecommendationsController(@Value("${INFERENCE_BASE_URL}") String inferenceBaseUrl) {
        this.inferenceBaseUrl = inferenceBaseUrl;
    }

    @GetMapping(value = "/recommendations/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> recommendations(@PathVariable long id) {

        // Option B payload (keeps inference schema happy)
        Map<String, Object> payload = Map.of(
                "user_id", id,
                "product_id", id
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    inferenceBaseUrl + "/recommendations",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            // Copy body to a mutable map so we can annotate/fallback
            Map<String, Object> body = resp.getBody() != null ? new HashMap<>(resp.getBody()) : new HashMap<>();

            // Normalize recommendations field
            Object recObj = body.get("recommendations");
            List<?> recs = (recObj instanceof List) ? (List<?>) recObj : List.of();

            if (recs.isEmpty()) {
                // ✅ Backend fallback (Option 1)
                body.put("recommendations", FALLBACK_POPULAR);
                body.put("source", "fallback_popular");
                body.put("reason", "empty_recs_or_cold_start");
            } else {
                body.put("source", "ml");
            }

            // Always include id for clarity (helps demo.sh + debugging)
            body.putIfAbsent("user_id", id);

            return ResponseEntity
                    .status(resp.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);

        } catch (RestClientException ex) {
            // ✅ Fallback when inference is down / times out / returns non-parseable response
            Map<String, Object> fallbackBody = new HashMap<>();
            fallbackBody.put("user_id", id);
            fallbackBody.put("recommendations", FALLBACK_POPULAR);
            fallbackBody.put("source", "fallback_popular");
            fallbackBody.put("reason", "inference_error");
            fallbackBody.put("detail", ex.getMessage());

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(fallbackBody);
        }
    }
}
