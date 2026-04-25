package io.snortexware.sisflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTicketTypeRequest {

    @NotBlank
    private String name;

    private String icon;

    private String description;
}
