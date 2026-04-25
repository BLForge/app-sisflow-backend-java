package io.snortexware.sisflow.dto;

import io.snortexware.sisflow.entities.TicketHomologation;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HomologationDecisionRequest {
    @NotNull
    private TicketHomologation.HomologationStatus status; // approved or rejected
    private String comment;
}
