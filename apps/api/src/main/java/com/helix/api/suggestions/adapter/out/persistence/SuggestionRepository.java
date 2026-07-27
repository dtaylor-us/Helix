package com.helix.api.suggestions.adapter.out.persistence;

import com.helix.api.suggestions.domain.SuggestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SuggestionRepository extends JpaRepository<SuggestionEntity, UUID> {
    List<SuggestionEntity> findByExperimentIdOrderByCreatedAtDesc(UUID experimentId);
}
