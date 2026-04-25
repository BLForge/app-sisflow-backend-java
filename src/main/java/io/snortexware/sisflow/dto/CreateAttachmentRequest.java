package io.snortexware.sisflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateAttachmentRequest {
    @NotBlank
    private String fileName;
    @NotBlank
    private String fileUrl;
    private Long fileSize;
    private String mimeType;
}
