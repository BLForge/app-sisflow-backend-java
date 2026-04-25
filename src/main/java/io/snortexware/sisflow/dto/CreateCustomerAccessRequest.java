package io.snortexware.sisflow.dto;

import io.snortexware.sisflow.entities.CustomerAccess;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCustomerAccessRequest {

    @NotBlank
    private String label;

    @NotNull
    private CustomerAccess.AccessType type;

    @NotBlank
    private String value;

    private String username;
    private String password;
    private String notes;
}
