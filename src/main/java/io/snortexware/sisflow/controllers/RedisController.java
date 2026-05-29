package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.RedisAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/redis")
@RequiredArgsConstructor
public class RedisController extends BaseController {

    private final AuthorizationService authorizationService;
    private final RedisAdminService redisAdminService;

    @Override
    protected AuthorizationService authorizationService() {
        return authorizationService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health(@AuthenticationPrincipal UUID callerId) {
        requireDeveloper(callerId);
        return ResponseEntity.ok(redisAdminService.health());
    }

    @GetMapping("/keys")
    public ResponseEntity<List<String>> keys(
            @RequestParam(required = false) String pattern,
            @AuthenticationPrincipal UUID callerId) {
        requireDeveloper(callerId);
        return ResponseEntity.ok(redisAdminService.listKeys(pattern));
    }

    @GetMapping("/value")
    public ResponseEntity<Map<String, Object>> value(
            @RequestParam String key,
            @AuthenticationPrincipal UUID callerId) {
        requireDeveloper(callerId);
        if (!redisAdminService.exists(key)) {
            throw AppException.notFound();
        }
        return ResponseEntity.ok(redisAdminService.getValue(key));
    }
}
