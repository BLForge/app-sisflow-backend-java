package io.snortexware.sisflow.auth.infrastructure.persistence;

import io.snortexware.sisflow.auth.domain.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface SpringDataUserAccountRepository extends JpaRepository<UserAccount, UUID> {

    @Query("select u from UserAccount u left join fetch u.tenant where u.id = :id")
    Optional<UserAccount> findByIdWithTenant(@Param("id") UUID id);

    @Query("select u from UserAccount u left join fetch u.tenant where u.email = :email")
    Optional<UserAccount> findByEmailWithTenant(@Param("email") String email);

    Optional<UserAccount> findByEmail(String email);
}
