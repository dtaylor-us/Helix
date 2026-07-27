package com.helix.api.wisdom.adapter.out.persistence;

import com.helix.api.wisdom.domain.WisdomEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WisdomEntryRepository extends JpaRepository<WisdomEntryEntity, UUID> {
    List<WisdomEntryEntity> findAllByOrderByRevisedAtDesc();
    List<WisdomEntryEntity> findTop20ByStatementContainingIgnoreCaseOrderByRevisedAtDesc(String query);
}
