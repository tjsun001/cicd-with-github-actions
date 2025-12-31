package com.amigoscode.inference;


import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class InferenceController {

    private final InferenceClient inferenceClient;

    public InferenceController(InferenceClient inferenceClient) {
        this.inferenceClient = inferenceClient;
    }

    @GetMapping("/recommendations/{userId}")
    public Object recommendations(@PathVariable int userId) {
        return inferenceClient.predict(userId);
    }
}

