package io.snortexware.sisflow.dto;

import lombok.Data;

@Data
public class UpdateGithubConfigurationRequest {

    private String webhookSecret;

    private Boolean enabled;
}
