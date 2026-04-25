package io.snortexware.sisflow.dto;

import io.snortexware.sisflow.entities.Project;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateProjectRequest {

    @NotBlank
    private String name;

    private String description;

    private String githubRepository;

    private String githubOwner;

    private UUID pullRequestStatusId;

    private Project.Status status;
}
