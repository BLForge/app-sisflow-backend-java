package io.snortexware.sisflow.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ticket_statuses")
public class TicketStatusConfig {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "color", length = 7, nullable = false)
    private String color;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "is_closed", nullable = false)
    private boolean isClosed;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
