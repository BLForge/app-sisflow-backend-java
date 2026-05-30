package io.snortexware.sisflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateDownloadTokenRequest {
    @NotBlank
    private String fileUrl;

    private String fileName;

    private boolean attachment;
}
