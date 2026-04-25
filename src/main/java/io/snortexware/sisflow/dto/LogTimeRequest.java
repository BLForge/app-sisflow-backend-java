package io.snortexware.sisflow.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LogTimeRequest {

    @NotNull
    @Positive
    @DecimalMax("24")
    private BigDecimal hours;

    private String description;
}
