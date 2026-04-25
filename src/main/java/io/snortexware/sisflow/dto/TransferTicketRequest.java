package io.snortexware.sisflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class TransferTicketRequest {

    @NotNull
    private UUID targetAgentId;

    private String reason;
}
