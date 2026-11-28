package com.away.core.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    @Query("select distinct u from AppUser u left join fetch u.memberships m left join fetch m.league where u.username = :username")
    Optional<AppUser> findWithMembershipsByUsername(@Param("username") String username);

    @Query("select distinct u from AppUser u left join fetch u.memberships m left join fetch m.league order by u.username")
    List<AppUser> listarConMembresias();

    boolean existsByUsernameIgnoreCase(String username);
}
