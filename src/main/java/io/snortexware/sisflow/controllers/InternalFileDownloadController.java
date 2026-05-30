package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.services.FileDownloadTokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/files")
@RequiredArgsConstructor
public class InternalFileDownloadController {

    private final FileDownloadTokenService fileDownloadTokenService;

    @GetMapping("/download/{token}")
    public ResponseEntity<Void> download(@PathVariable String token, HttpServletRequest request) {
        String requesterKey = request.getHeader("X-Real-IP");
        if (requesterKey == null || requesterKey.isBlank()) {
            requesterKey = request.getRemoteAddr();
        }
        return fileDownloadTokenService.resolveToken(token, requesterKey);
    }
}
