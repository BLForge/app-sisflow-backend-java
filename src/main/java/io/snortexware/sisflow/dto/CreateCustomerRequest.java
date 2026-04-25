package io.snortexware.sisflow.dto;

import io.snortexware.sisflow.validation.Cnpj;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCustomerRequest {

    @NotBlank
    private String name;

    private String tradeName;

    @NotBlank
    @Cnpj
    private String document;

    @Email
    private String email;

    private String phone;

    private String address;

    private String city;

    @Size(min = 2, max = 2)
    private String state;

    private String logoUrl;

    private String notes;
}
