package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.entities.StoredFile;
import io.snortexware.sisflow.repositories.StoredFileRepository;
import io.snortexware.sisflow.security.exceptions.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final StoredFileRepository storedFileRepository;

    @Value("${app.base.url}")
    private String baseUrl;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bucket", defaultValue = "avatars") String bucket) throws IOException {

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/"))
            throw AppException.fileTypeNotAllowed();

        String ext = contentType.substring(contentType.lastIndexOf('/') + 1);
        String filename = UUID.randomUUID() + "." + ext;

        storedFileRepository.save(StoredFile.builder()
                .bucket(bucket)
                .filename(filename)
                .contentType(contentType)
                .data(file.getBytes())
                .createdAt(OffsetDateTime.now())
                .build());

        return ResponseEntity.ok(Map.of("url", baseUrl + "/files/" + bucket + "/" + filename));
    }

    @GetMapping("/{bucket}/{filename}")
    public ResponseEntity<byte[]> serve(
            @PathVariable String bucket,
            @PathVariable String filename) {

        StoredFile f = storedFileRepository.findByBucketAndFilename(bucket, filename)
                .orElseThrow(AppException::notFound);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(f.getContentType()))
                .body(f.getData());
    }
}
