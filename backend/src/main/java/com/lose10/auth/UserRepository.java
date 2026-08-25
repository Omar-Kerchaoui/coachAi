package com.lose10.auth;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("select u from User u where :q is null or :q = '' "
            + "or u.email like concat('%', :q, '%') or u.phoneE164 like concat('%', :q, '%')")
    Page<User> search(@Param("q") String query, Pageable pageable);
}
