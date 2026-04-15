package com.rawr.subscription;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> subscribe(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || !email.matches("^[^@]+@[^@]+\\.[^@]+$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid email"));
        }
        subscriptionService.subscribe(email);
        return ResponseEntity.ok(Map.of("message", "Subscribed successfully"));
    }

    @GetMapping("/unsubscribe")
    public ResponseEntity<Map<String, String>> unsubscribe(@RequestParam String token) {
        subscriptionService.unsubscribe(token);
        return ResponseEntity.ok(Map.of("message", "Unsubscribed successfully"));
    }
}
