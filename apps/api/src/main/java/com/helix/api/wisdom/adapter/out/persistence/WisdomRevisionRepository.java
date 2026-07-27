package com.helix.api.wisdom.adapter.out.persistence;

import com.helix.api.wisdom.domain.WisdomRevisionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WisdomRevisionRepository extends JpaRepository<WisdomRevisionEntity, UUID> {
    List<WisdomRevisionEntity> findByWisdomIdOrderByCreatedAtDesc(UUID wisdomId);
}
