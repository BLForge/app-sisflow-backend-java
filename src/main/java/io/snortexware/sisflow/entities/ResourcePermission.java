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
@Table(name = "resource_permissions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"resource_id", "user_id", "action", "resource_type"})
})
public class ResourcePermission {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserProfile user;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "granted_by")
    private String grantedBy;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public enum ResourceType {
        TICKET("TICKET"),
        PROJECT("PROJECT"),
        SYSTEM("SYSTEM");

        private final String value;

        ResourceType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum Action {
        READ("READ"),
        UPDATE("UPDATE"),
        DELETE("DELETE"),
        MODERATE("MODERATE");

        private final String value;

        Action(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public boolean isExpired() {
        if (expiresAt == null) {
            return false;
        }
        return OffsetDateTime.now().isAfter(expiresAt);
    }
}
