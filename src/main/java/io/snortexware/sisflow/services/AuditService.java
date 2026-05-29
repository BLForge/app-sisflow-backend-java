package io.snortexware.sisflow.services;

import io.snortexware.sisflow.entities.AuditEvent;
import io.snortexware.sisflow.entities.Ticket;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.repositories.AuditEventRepository;
import io.snortexware.sisflow.repositories.TicketRepository;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    public static final String TICKET_CREATED      = "TICKET_CREATED";
    public static final String STATUS_CHANGED      = "STATUS_CHANGED";
    public static final String PRIORITY_CHANGED    = "PRIORITY_CHANGED";
    public static final String TYPE_CHANGED        = "TYPE_CHANGED";
    public static final String ASSIGNED_TO_CHANGED = "ASSIGNED_TO_CHANGED";
    public static final String GROUP_CHANGED       = "GROUP_CHANGED";
    public static final String TRANSFERRED         = "TRANSFERRED";
    public static final String TIME_LOGGED         = "TIME_LOGGED";
    public static final String SLA_CHANGED         = "SLA_CHANGED";
    public static final String ROLE_ASSIGNED       = "ROLE_ASSIGNED";
    public static final String ROLE_REVOKED        = "ROLE_REVOKED";
    public static final String PERMISSION_GRANTED  = "PERMISSION_GRANTED";
    public static final String PERMISSION_REVOKED  = "PERMISSION_REVOKED";

    private final AuditEventRepository auditEventRepository;
    private final TicketRepository ticketRepository;
    private final UserProfileRepository userProfileRepository;

    public void record(UUID ticketId, UUID actorId, String action, String oldValue, String newValue) {
        try {
            Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
            if (ticketOpt.isEmpty()) {
                log.warn("AuditService.record: ticket not found for id={}", ticketId);
                return;
            }

            Optional<UserProfile> actorOpt = userProfileRepository.findById(actorId);
            if (actorOpt.isEmpty()) {
                log.warn("AuditService.record: actor not found for id={}", actorId);
                return;
            }

            AuditEvent event = AuditEvent.builder()
                    .ticket(ticketOpt.get())
                    .actor(actorOpt.get())
                    .action(action)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .createdAt(OffsetDateTime.now())
                    .build();

            auditEventRepository.save(event);
        } catch (Exception e) {
            log.warn("AuditService.record: failed to record audit event for ticketId={}, action={}: {}",
                    ticketId, action, e.getMessage());
        }
    }

    public void logRoleAssignment(UUID userId, UUID roleId, UUID actorId) {
        try {
            log.info("Logging role assignment: user={}, role={}, actor={}", userId, roleId, actorId);
        } catch (Exception e) {
            log.warn("Failed to log role assignment: {}", e.getMessage());
        }
    }

    public void logRoleRevocation(UUID userId, UUID roleId, UUID actorId) {
        try {
            log.info("Logging role revocation: user={}, role={}, actor={}", userId, roleId, actorId);
        } catch (Exception e) {
            log.warn("Failed to log role revocation: {}", e.getMessage());
        }
    }

    public void logPermissionChange(UUID roleId, UUID permissionId, String action) {
        try {
            log.info("Logging permission change: role={}, permission={}, action={}", roleId, permissionId, action);
        } catch (Exception e) {
            log.warn("Failed to log permission change: {}", e.getMessage());
        }
    }
}
