package io.snortexware.sisflow.dto;

import io.snortexware.sisflow.entities.Ticket;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateTicketRequest {

    @NotBlank
    private String title;

    private Long code; // optional override — validated for uniqueness in service

    private String description;

    private String privateNotes;

    @NotNull
    private Ticket.Priority priority;

    @NotNull
    private Ticket.TicketType type;

    @NotNull
    private UUID statusId;

    @NotNull
    private UUID customerId;

    @NotNull
    private UUID slaId;
}
