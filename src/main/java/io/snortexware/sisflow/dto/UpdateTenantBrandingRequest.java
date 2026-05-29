package io.snortexware.sisflow.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateTenantBrandingRequest {
    private String name;
    private String logoUrl;
    private String logoIconUrl;
    private String backgroundUrl;
}
