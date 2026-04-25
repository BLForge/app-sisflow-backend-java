package io.snortexware.sisflow.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ticket_priorities")
public class TicketPriorityConfig {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "color", length = 7, nullable = false)
    private String color;

    @Column(name = "sla_multiplier", nullable = false, precision = 4, scale = 2)
    private BigDecimal slaMultiplier;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
