package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.LoginRequest;
import io.snortexware.sisflow.dto.RegisterRequest;
import io.snortexware.sisflow.dto.ResetPasswordRequest;
import io.snortexware.sisflow.services.SupabaseAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SupabaseAuthService authService;

    @Value("${app.base.url:https://ticket.lucasmoreira.cc}")
    private String appBaseUrl;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        JSONObject result = authService.signIn(req.getEmail(), req.getPassword());
        return ResponseEntity.ok(Map.of(
            "accessToken", result.optString("access_token"),
            "refreshToken", result.optString("refresh_token"),
            "expiresIn", result.optInt("expires_in", 3600)
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        String redirectTo = appBaseUrl + "/email-confirmed";
        JSONObject result = authService.signUp(req.getEmail(), req.getPassword(), req.getFullName(), redirectTo);

        if (result.has("access_token")) {
            return ResponseEntity.ok(Map.of(
                "status", "confirmed",
                "accessToken", result.optString("access_token"),
                "refreshToken", result.optString("refresh_token")
            ));
        }
        return ResponseEntity.ok(Map.of("status", "confirmation_required"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        String redirectTo = req.getRedirectTo() != null ? req.getRedirectTo() : appBaseUrl + "/reset-password";
        authService.resetPassword(req.getEmail(), redirectTo);
        return ResponseEntity.ok(Map.of("status", "email_sent"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "refreshToken required"));
        }
        JSONObject result = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(Map.of(
            "accessToken", result.optString("access_token"),
            "refreshToken", result.optString("refresh_token"),
            "expiresIn", result.optInt("expires_in", 3600)
        ));
    }
}
