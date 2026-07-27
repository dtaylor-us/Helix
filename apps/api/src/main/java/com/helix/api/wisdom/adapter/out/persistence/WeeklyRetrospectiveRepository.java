package com.helix.api.wisdom.adapter.out.persistence;

import com.helix.api.wisdom.domain.WeeklyRetrospectiveEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WeeklyRetrospectiveRepository extends JpaRepository<WeeklyRetrospectiveEntity, UUID> {
    List<WeeklyRetrospectiveEntity> findTop20BySummaryContainingIgnoreCaseOrderByCreatedAtDesc(String query);
    List<WeeklyRetrospectiveEntity> findTop10ByOrderByCreatedAtDesc();
}
