package io.snortexware.sisflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateInteractionRequest {

    @NotBlank
    private String message;

    private boolean isInternal;
}
