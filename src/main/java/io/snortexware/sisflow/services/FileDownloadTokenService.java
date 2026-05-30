package io.snortexware.sisflow.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.snortexware.sisflow.dto.CreateDownloadTokenRequest;
import io.snortexware.sisflow.dto.DownloadTokenResponse;
import io.snortexware.sisflow.entities.TicketAttachment;
import io.snortexware.sisflow.repositories.CustomerRepository;
import io.snortexware.sisflow.repositories.TenantRepository;
import io.snortexware.sisflow.repositories.TicketAttachmentRepository;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import io.snortexware.sisflow.security.TenantContext;
import io.snortexware.sisflow.security.exceptions.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileDownloadTokenService {

    private static final String TOKEN_PREFIX = "file-download:token:";
    private static final String ISSUE_RATE_LIMIT_PREFIX = "file-download:issue:";
    private static final String RESOLVE_RATE_LIMIT_PREFIX = "file-download:resolve:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final TenantContext tenantContext;
    private final AuthorizationService authorizationService;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final TenantRepository tenantRepository;
    private final CustomerRepository customerRepository;
    private final UserProfileRepository userProfileRepository;
    private final RateLimitService rateLimitService;

    @Value("${app.base.url}")
    private String baseUrl;

    @Value("${file.download.token.ttl-seconds:60}")
    private long tokenTtlSeconds;

    @Value("${file.download.token.issue-limit-per-minute:60}")
    private long issueLimitPerMinute;

    @Value("${file.download.token.resolve-limit-per-minute:180}")
    private long resolveLimitPerMinute;

    @Value("${file.accel.redirect.prefix:/_protected_files}")
    private String accelRedirectPrefix;

    public DownloadTokenResponse issueToken(UUID callerId, CreateDownloadTokenRequest request) {
        if (callerId == null) throw AppException.unauthorized();

        var rateLimit = rateLimitService.consume(
                ISSUE_RATE_LIMIT_PREFIX + callerId,
                issueLimitPerMinute,
                Duration.ofMinutes(1)
        );
        if (!rateLimit.allowed()) {
            throw new AppException(io.snortexware.sisflow.security.ErrorCode.RATE_LIMIT_EXCEEDED, HttpStatus.TOO_MANY_REQUESTS);
        }

        String normalizedFileUrl = normalizeFileUrl(request.getFileUrl());
        FilePath path = parseFileUrl(normalizedFileUrl);
        authorizeAccess(callerId, normalizedFileUrl, path);

        String token = UUID.randomUUID().toString();
        DownloadTokenPayload payload = new DownloadTokenPayload(
                normalizedFileUrl,
                path.bucket(),
                path.filename(),
                sanitizeDownloadName(request.getFileName(), path.filename()),
                request.isAttachment()
        );

        try {
            stringRedisTemplate.opsForValue().set(
                    TOKEN_PREFIX + token,
                    objectMapper.writeValueAsString(payload),
                    Duration.ofSeconds(tokenTtlSeconds)
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize download token payload", e);
        }

        return new DownloadTokenResponse(
                baseUrl + "/files/download/" + token + "/" + payload.downloadName(),
                tokenTtlSeconds
        );
    }

    public ResponseEntity<Void> resolveToken(String token, String requesterKey) {
        var rateLimit = rateLimitService.consume(
                RESOLVE_RATE_LIMIT_PREFIX + requesterKey,
                resolveLimitPerMinute,
                Duration.ofMinutes(1)
        );
        if (!rateLimit.allowed()) {
            throw new AppException(io.snortexware.sisflow.security.ErrorCode.RATE_LIMIT_EXCEEDED, HttpStatus.TOO_MANY_REQUESTS);
        }

        String rawPayload = stringRedisTemplate.opsForValue().get(TOKEN_PREFIX + token);
        if (rawPayload == null || rawPayload.isBlank()) {
            throw AppException.notFound();
        }

        try {
            DownloadTokenPayload payload = objectMapper.readValue(rawPayload, DownloadTokenPayload.class);
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-Accel-Redirect", buildInternalRedirect(payload.bucket(), payload.filename()));
            headers.add(HttpHeaders.CACHE_CONTROL, "private, no-store");
            String dispositionType = payload.attachment() ? "attachment" : "inline";
            headers.add(HttpHeaders.CONTENT_DISPOSITION, dispositionType + "; filename=\"" + payload.downloadName() + "\"");
            return new ResponseEntity<>(headers, HttpStatus.OK);
        } catch (JsonProcessingException e) {
            throw AppException.notFound();
        }
    }

    private void authorizeAccess(UUID callerId, String normalizedFileUrl, FilePath path) {
        UUID tenantId = tenantContext.getCurrentTenant();

        switch (path.bucket()) {
            case "attachments" -> authorizeTicketAttachment(callerId, normalizedFileUrl);
            case "logos", "backgrounds" -> authorizeBrandingFile(callerId, tenantId, normalizedFileUrl);
            case "avatars" -> authorizeAvatarFile(callerId, tenantId, normalizedFileUrl);
            default -> throw AppException.forbidden();
        }
    }

    private void authorizeTicketAttachment(UUID callerId, String normalizedFileUrl) {
        TicketAttachment attachment = ticketAttachmentRepository.findByFileUrlWithTicket(normalizedFileUrl)
                .orElseThrow(AppException::notFound);
        authorizationService.validateCanViewTicket(callerId, attachment.getTicket());
    }

    private void authorizeBrandingFile(UUID callerId, UUID tenantId, String normalizedFileUrl) {
        if (tenantId == null) throw AppException.forbidden();

        boolean tenantBrandingMatch = tenantRepository.existsTenantBrandingFile(tenantId, normalizedFileUrl);
        boolean customerBrandingMatch = customerRepository.existsByLogoUrlAndTenant_Id(normalizedFileUrl, tenantId);

        if (!tenantBrandingMatch && !customerBrandingMatch) {
            throw AppException.notFound();
        }

        if (!authorizationService.isDeveloperOrAbove(callerId)) {
            throw AppException.forbidden();
        }
    }

    private void authorizeAvatarFile(UUID callerId, UUID tenantId, String normalizedFileUrl) {
        if (tenantId == null) throw AppException.forbidden();

        boolean sameTenantAvatar = userProfileRepository.existsByAvatarUrlAndTenant_Id(normalizedFileUrl, tenantId);
        if (!sameTenantAvatar) {
            throw AppException.notFound();
        }

        if (!authorizationService.isDeveloperOrAbove(callerId) && !userProfileRepository.existsByIdAndAvatarUrl(callerId, normalizedFileUrl)) {
            throw AppException.forbidden();
        }
    }

    private String normalizeFileUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw AppException.badRequest();
        }

        if (fileUrl.startsWith("/files/")) {
            return fileUrl;
        }

        if (fileUrl.startsWith(baseUrl + "/files/")) {
            return fileUrl.substring(baseUrl.length());
        }

        throw AppException.badRequest();
    }

    private FilePath parseFileUrl(String fileUrl) {
        String[] parts = fileUrl.split("/");
        if (parts.length != 4 || !"files".equals(parts[1])) {
            throw AppException.badRequest();
        }

        String bucket = parts[2];
        String filename = parts[3];
        if (bucket.isBlank() || filename.isBlank() || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw AppException.badRequest();
        }

        return new FilePath(bucket, filename);
    }

    private String sanitizeDownloadName(String requestedName, String fallbackName) {
        String candidate = (requestedName == null || requestedName.isBlank()) ? fallbackName : requestedName;
        return Paths.get(candidate).getFileName().toString().replace("\"", "");
    }

    private String buildInternalRedirect(String bucket, String filename) {
        Path safePath = Paths.get("/", bucket, filename).normalize();
        return accelRedirectPrefix + safePath.toString();
    }

    private record FilePath(String bucket, String filename) {
    }

    private record DownloadTokenPayload(
            String fileUrl,
            String bucket,
            String filename,
            String downloadName,
            boolean attachment
    ) {
    }
}
