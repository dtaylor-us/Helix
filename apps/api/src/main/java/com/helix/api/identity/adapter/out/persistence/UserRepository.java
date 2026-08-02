package com.helix.api.identity.adapter.out.persistence;

import com.helix.api.identity.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByGoogleSub(String googleSub);
    Optional<UserEntity> findByEmail(String email);
}
