package io.snortexware.sisflow.services;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Proxies auth operations to Supabase Auth REST API.
 * The frontend never needs to know about Supabase.
 */
@Slf4j
@Service
public class SupabaseAuthService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.anon.key}")
    private String supabaseAnonKey;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** Sign in with email + password. Returns the full Supabase auth response JSON. */
    public JSONObject signIn(String email, String password) {
        JSONObject body = new JSONObject();
        body.put("email", email);
        body.put("password", password);
        return post("/auth/v1/token?grant_type=password", body);
    }

    /** Sign up with email + password + full name. */
    public JSONObject signUp(String email, String password, String fullName, String emailRedirectTo) {
        JSONObject body = new JSONObject();
        body.put("email", email);
        body.put("password", password);
        JSONObject data = new JSONObject();
        data.put("full_name", fullName);
        body.put("data", data);
        if (emailRedirectTo != null) {
            body.put("email_redirect_to", emailRedirectTo);
        }
        return post("/auth/v1/signup", body);
    }

    /** Send password reset email. */
    public void resetPassword(String email, String redirectTo) {
        JSONObject body = new JSONObject();
        body.put("email", email);
        if (redirectTo != null) {
            body.put("redirect_to", redirectTo);
        }
        post("/auth/v1/recover", body);
    }

    /** Refresh an access token using a refresh token. */
    public JSONObject refreshToken(String refreshToken) {
        JSONObject body = new JSONObject();
        body.put("refresh_token", refreshToken);
        return post("/auth/v1/token?grant_type=refresh_token", body);
    }

    private JSONObject post(String path, JSONObject body) {
        if (supabaseAnonKey == null || supabaseAnonKey.isBlank()) {
            log.error("SUPABASE_ANON_KEY is not configured");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Auth service not configured. Contact the administrator.");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(supabaseUrl + path))
                    .header("Content-Type", "application/json")
                    .header("apikey", supabaseAnonKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            // Try to parse response body
            JSONObject json;
            try {
                json = new JSONObject(response.body());
            } catch (Exception parseEx) {
                log.warn("Could not parse Supabase response: {}", response.body());
                json = new JSONObject();
            }

            if (response.statusCode() >= 400) {
                String msg = json.optString("error_description",
                        json.optString("msg",
                        json.optString("message",
                        json.optString("error", "Credenciais inválidas"))));
                log.warn("Supabase auth error {}: {}", response.statusCode(), msg);
                // Always return 400 so apiFetch doesn't treat it as session expiry (401 → redirect)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
            }

            return json;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Supabase auth request failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Auth service unavailable");
        }
    }
}
