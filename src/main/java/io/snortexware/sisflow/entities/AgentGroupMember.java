package io.snortexware.sisflow.entities;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "agent_group_members")
@IdClass(AgentGroupMember.AgentGroupMemberId.class)
public class AgentGroupMember {

    @Id
    @Column(name = "group_id", columnDefinition = "uuid", nullable = false)
    private UUID groupId;

    @Id
    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentGroupMemberId implements Serializable {
        private UUID groupId;
        private UUID userId;
    }
}
