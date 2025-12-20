package com.amigoscode.pongping;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/pong")
public class PongPingController {
    @GetMapping
    public String pongPing() {
        return "ping";
    }
}
