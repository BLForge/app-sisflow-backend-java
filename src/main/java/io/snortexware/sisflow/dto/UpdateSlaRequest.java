package io.snortexware.sisflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class UpdateSlaRequest {

    @NotBlank
    private String name;

    @NotNull
    @Positive
    private Integer responseTimeHours;

    @NotNull
    @Positive
    private Integer resolutionTimeHours;
}
