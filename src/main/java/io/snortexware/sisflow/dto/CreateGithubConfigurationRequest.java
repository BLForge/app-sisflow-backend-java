package io.snortexware.sisflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateGithubConfigurationRequest {

    @NotNull
    private UUID projectId;

    private String webhookSecret;

    private Boolean enabled;
}
