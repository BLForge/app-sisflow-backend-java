package io.snortexware.sisflow.repositories;

import io.snortexware.sisflow.entities.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.project.id = :projectId")
    List<ProjectMember> findByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.user.id = :userId")
    List<ProjectMember> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.project.id = :projectId AND pm.user.id = :userId")
    Optional<ProjectMember> findByProjectIdAndUserId(@Param("projectId") UUID projectId, @Param("userId") UUID userId);

    @Query("SELECT COUNT(pm) > 0 FROM ProjectMember pm WHERE pm.project.id = :projectId AND pm.user.id = :userId")
    boolean isProjectMember(@Param("projectId") UUID projectId, @Param("userId") UUID userId);

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.user.id = :userId AND pm.roleInProject = :role")
    List<ProjectMember> findByUserIdAndRole(@Param("userId") UUID userId, @Param("role") String role);

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.project.id = :projectId AND pm.roleInProject = :role")
    List<ProjectMember> findByProjectIdAndRole(@Param("projectId") UUID projectId, @Param("role") String role);
}
