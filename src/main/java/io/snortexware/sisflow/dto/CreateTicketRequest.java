package io.snortexware.sisflow.dto;

import io.snortexware.sisflow.entities.Ticket;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateTicketRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Ticket.Priority priority;

    @NotNull
    private Ticket.TicketType type;

    @NotNull
    private UUID customerId;

    @NotNull
    private UUID slaId;
}
