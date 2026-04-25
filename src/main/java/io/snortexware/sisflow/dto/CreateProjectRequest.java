package io.snortexware.sisflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateProjectRequest {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private UUID systemId;

    private String githubRepository;

    private String githubOwner;

    private UUID pullRequestStatusId;
}
