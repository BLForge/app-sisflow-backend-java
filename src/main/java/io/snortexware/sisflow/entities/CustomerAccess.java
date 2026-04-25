package io.snortexware.sisflow.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customer_accesses")
public class CustomerAccess {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "label", nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AccessType type;

    @Column(name = "value", columnDefinition = "TEXT", nullable = false)
    private String value;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    public enum AccessType {
        vpn, link, ip, credential, other
    }
}
