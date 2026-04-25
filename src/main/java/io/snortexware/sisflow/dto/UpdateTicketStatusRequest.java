package io.snortexware.sisflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateTicketStatusRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String color;

    private boolean isDefault;

    private boolean isClosed;

    private int sortOrder;
}
