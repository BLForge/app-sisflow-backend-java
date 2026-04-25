package io.snortexware.sisflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateKnowledgeBaseRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    private UUID categoryId;

    private Boolean isPublished;
}
