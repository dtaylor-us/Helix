package com.helix.api.wisdom.adapter.out.persistence;

import com.helix.api.wisdom.domain.WisdomSourceLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WisdomSourceLinkRepository extends JpaRepository<WisdomSourceLinkEntity, UUID> {
    List<WisdomSourceLinkEntity> findByWisdomIdOrderByCreatedAtAsc(UUID wisdomId);
}
