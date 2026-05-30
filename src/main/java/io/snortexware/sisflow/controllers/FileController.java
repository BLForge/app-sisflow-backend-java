package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateDownloadTokenRequest;
import io.snortexware.sisflow.dto.DownloadTokenResponse;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.FileDownloadTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileDownloadTokenService fileDownloadTokenService;

    @Value("${app.base.url}")
    private String baseUrl;

    @Value("${file.upload.base-dir:./uploads}")
    private String uploadBaseDir;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_BUCKETS = Set.of("avatars", "attachments", "logos", "backgrounds");
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    
    private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
        "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
        "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
        "image/gif", new byte[]{0x47, 0x49, 0x46, 0x38},
        "image/webp", new byte[]{0x52, 0x49, 0x46, 0x46}
    );

    private boolean isValidImageFile(MultipartFile file, String contentType) throws IOException {
        byte[] magic = MAGIC_BYTES.get(contentType);
        if (magic == null) return false;
        
        byte[] header = new byte[magic.length];
        try (var is = file.getInputStream()) {
            if (is.read(header) != magic.length) return false;
        }
        
        for (int i = 0; i < magic.length; i++) {
            if (header[i] != magic[i]) return false;
        }
        return true;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bucket", defaultValue = "avatars") String bucket,
            @AuthenticationPrincipal UUID callerId) throws IOException {

        if (callerId == null) throw AppException.unauthorized();
        if (file.isEmpty() || file.getSize() > MAX_FILE_SIZE) throw AppException.badRequest();
        if (!ALLOWED_BUCKETS.contains(bucket)) throw AppException.badRequest();

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType))
            throw AppException.fileTypeNotAllowed();
        
        if (!isValidImageFile(file, contentType))
            throw AppException.fileTypeNotAllowed();

        String filename = UUID.randomUUID() + ".webp";

        Path bucketPath = Paths.get(uploadBaseDir, bucket).toAbsolutePath().normalize();
        Files.createDirectories(bucketPath);
        
        Path filePath = bucketPath.resolve(filename);
        if (!filePath.startsWith(bucketPath)) throw AppException.badRequest();
        
        int maxWidth = bucket.equals("backgrounds") ? 1920 : 800;
        Thumbnails.of(file.getInputStream())
                .size(maxWidth, maxWidth)
                .outputFormat("webp")
                .outputQuality(0.95)
                .toFile(filePath.toFile());

        return ResponseEntity.ok(Map.of("url", baseUrl + "/files/" + bucket + "/" + filename));
    }

    @PostMapping("/download-token")
    public ResponseEntity<DownloadTokenResponse> createDownloadToken(
            @Valid @RequestBody CreateDownloadTokenRequest request,
            @AuthenticationPrincipal UUID callerId) {
        return ResponseEntity.ok(fileDownloadTokenService.issueToken(callerId, request));
    }
}
