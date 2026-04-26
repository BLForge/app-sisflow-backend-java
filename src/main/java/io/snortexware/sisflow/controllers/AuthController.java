package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.LoginRequest;
import io.snortexware.sisflow.dto.RegisterRequest;
import io.snortexware.sisflow.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.signIn(req.getEmail(), req.getPassword()));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.signUp(req.getEmail(), req.getPassword(), req.getFullName()));
    }

    @GetMapping("/confirm-email")
    public ResponseEntity<Map<String, Object>> confirmEmail(@RequestParam String token) {
        return ResponseEntity.ok(authService.confirmEmail(token));
    }

    @PostMapping("/resend-confirmation")
    public ResponseEntity<Void> resendConfirmation(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) return ResponseEntity.badRequest().build();
        authService.resendConfirmation(email);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> requestPasswordReset(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) return ResponseEntity.badRequest().build();
        authService.requestPasswordReset(email);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@RequestBody Map<String, String> body) {
        authService.resetPassword(body.get("token"), body.get("password"));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "refreshToken required"));
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }
}
