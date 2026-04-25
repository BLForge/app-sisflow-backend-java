package io.snortexware.sisflow.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

/**
 * Proxies file downloads from Supabase Storage.
 * Clients use /files/** — Supabase URLs are never exposed.
 * SECURITY: Requires authentication for all file access.
 */
@Slf4j
@RestController
@RequestMapping("/files")
public class FileProxyController {

    @Value("${supabase.storage.url}")
    private String storageBaseUrl;

    @Value("${supabase.anon.key}")
    private String supabaseAnonKey;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * GET /files/{bucket}/{**path}
     * Streams the file from Supabase Storage to the client.
     * SECURITY: Requires authentication - only authenticated users can access files.
     */
    @GetMapping("/{bucket}/**")
    public ResponseEntity<byte[]> proxy(
            @PathVariable String bucket,
            @AuthenticationPrincipal UUID callerId,
            jakarta.servlet.http.HttpServletRequest request) {

        // SECURITY: Require authentication for all file access
        if (callerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        // SECURITY: Validate bucket name to prevent path traversal
        if (!isValidBucket(bucket)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid bucket name");
        }

        // Extract the full path after /files/{bucket}/
        String fullPath = request.getRequestURI();
        String prefix = "/files/" + bucket + "/";
        String objectPath = fullPath.startsWith(prefix) ? fullPath.substring(prefix.length()) : "";

        // SECURITY: Validate object path to prevent path traversal
        if (!isValidObjectPath(objectPath)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file path");
        }

        String supabaseUrl = storageBaseUrl + "/object/public/" + bucket + "/" + objectPath;

        try {
            HttpRequest proxyRequest = HttpRequest.newBuilder()
                    .uri(URI.create(supabaseUrl))
                    .header("apikey", supabaseAnonKey)
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<byte[]> response = http.send(proxyRequest, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
            }
            if (response.statusCode() >= 400) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Storage error");
            }

            // Forward content-type and content-disposition
            HttpHeaders headers = new HttpHeaders();
            response.headers().firstValue("content-type")
                    .ifPresent(ct -> headers.setContentType(MediaType.parseMediaType(ct)));
            response.headers().firstValue("content-disposition")
                    .ifPresent(cd -> headers.set(HttpHeaders.CONTENT_DISPOSITION, cd));
            
            // Cache for 1 hour but require revalidation
            headers.setCacheControl(CacheControl.maxAge(Duration.ofHours(1)).mustRevalidate());

            // SECURITY: Log file access for audit purposes
            log.info("User {} accessed file: {}/{}", callerId, bucket, objectPath);

            return new ResponseEntity<>(response.body(), headers, HttpStatus.OK);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("File proxy error for {} (user: {}): {}", supabaseUrl, callerId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not fetch file");
        }
    }

    /**
     * SECURITY: Validate bucket name to prevent path traversal attacks
     */
    private boolean isValidBucket(String bucket) {
        if (bucket == null || bucket.trim().isEmpty()) {
            return false;
        }
        
        // Only allow alphanumeric characters, hyphens, and underscores
        return bucket.matches("^[a-zA-Z0-9_-]+$") && 
               !bucket.contains("..") && 
               !bucket.startsWith("/") && 
               !bucket.endsWith("/");
    }

    /**
     * SECURITY: Validate object path to prevent path traversal attacks
     */
    private boolean isValidObjectPath(String objectPath) {
        if (objectPath == null) {
            return false;
        }
        
        // Prevent path traversal attempts
        return !objectPath.contains("..") && 
               !objectPath.startsWith("/") && 
               !objectPath.contains("\\") &&
               objectPath.length() <= 1000; // Reasonable path length limit
    }
}
