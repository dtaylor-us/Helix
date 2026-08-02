package com.helix.api.identity.adapter.out.persistence;

import com.helix.api.identity.domain.AuthorizedUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorizedUserRepository extends JpaRepository<AuthorizedUserEntity, String> {
}
