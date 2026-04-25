package io.snortexware.sisflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateSlaRequest {

    @NotBlank
    private String name;

    @NotNull
    private Integer responseTimeHours;

    @NotNull
    private Integer resolutionTimeHours;
}
