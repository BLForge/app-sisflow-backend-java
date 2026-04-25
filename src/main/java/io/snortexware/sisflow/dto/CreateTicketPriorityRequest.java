package io.snortexware.sisflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateTicketPriorityRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String color;

    private BigDecimal slaMultiplier;

    private int sortOrder;
}
