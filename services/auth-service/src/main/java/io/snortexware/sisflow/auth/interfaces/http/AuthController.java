package io.snortexware.sisflow.auth.interfaces.http;

import io.snortexware.sisflow.auth.application.AuthApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Validated
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthApplicationService authApplicationService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authApplicationService.signIn(request.email(), request.password(), httpRequest));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authApplicationService.signUp(request.email(), request.password(), request.fullName()));
    }

    @GetMapping("/confirm-email")
    public ResponseEntity<Map<String, Object>> confirmEmail(@RequestParam String token) {
        return ResponseEntity.ok(authApplicationService.confirmEmail(token));
    }

    @PostMapping("/resend-confirmation")
    public ResponseEntity<Void> resendConfirmation(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        authApplicationService.resendConfirmation(email);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> requestPasswordReset(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        authApplicationService.requestPasswordReset(email);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@RequestBody Map<String, String> body) {
        authApplicationService.resetPassword(body.get("token"), body.get("password"));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "refreshToken required"));
        }
        return ResponseEntity.ok(authApplicationService.refresh(refreshToken));
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
    public record RegisterRequest(@Email @NotBlank String email, @NotBlank String password, @NotBlank String fullName) {}
}
