package com.helix.api.shared;

import com.helix.api.beliefs.application.BeliefService;
import com.helix.api.beliefs.domain.BeliefEntity;
import com.helix.api.beliefs.domain.BeliefType;
import com.helix.api.evidence.application.EvidenceService;
import com.helix.api.evidence.domain.EvidenceDirection;
import com.helix.api.evidence.domain.EvidenceEntity;
import com.helix.api.evidence.domain.ProvenanceRecordType;
import com.helix.api.evidence.domain.ProvenanceSourceKind;
import com.helix.api.reflection.application.ReflectionService;
import com.helix.api.reflection.domain.ReflectionEntity;
import com.helix.api.shared.application.StructuredSearchService;
import com.helix.api.shared.application.SemanticIndexingService;
import com.helix.api.shared.application.SemanticRetrievalService;
import com.helix.api.wisdom.application.WeeklyRetrospectiveService;
import com.helix.api.wisdom.application.WisdomService;
import com.helix.api.wisdom.domain.WisdomEntryEntity;
import com.helix.api.wisdom.domain.WisdomStatus;
import com.helix.api.wisdom.domain.WeeklyRetrospectiveEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class StructuredSearchServiceTest {

    @Test
    void searchAggregatesRecordsAcrossModules() {
        var reflectionService = Mockito.mock(ReflectionService.class);
        var beliefService = Mockito.mock(BeliefService.class);
        var evidenceService = Mockito.mock(EvidenceService.class);
        var wisdomService = Mockito.mock(WisdomService.class);
        var retrospectiveService = Mockito.mock(WeeklyRetrospectiveService.class);
        var semanticIndexingService = Mockito.mock(SemanticIndexingService.class);
        var semanticRetrievalService = Mockito.mock(SemanticRetrievalService.class);

        when(semanticIndexingService.isIndexed()).thenReturn(true);
        when(reflectionService.search(anyString())).thenReturn(List.of(
            new ReflectionEntity(UUID.randomUUID(), UUID.randomUUID(), "Consistency note", OffsetDateTime.now())
        ));
        when(beliefService.search(anyString())).thenReturn(List.of(
            new BeliefEntity(UUID.randomUUID(), UUID.randomUUID(), "Consistency grows trust", BeliefType.EMPOWERING,
                OffsetDateTime.now().minusDays(1), OffsetDateTime.now())
        ));
        when(evidenceService.search(anyString())).thenReturn(List.of(
            new EvidenceEntity(UUID.randomUUID(), UUID.randomUUID(), null, null, "Observed momentum", null,
                EvidenceDirection.SUPPORTS, ProvenanceSourceKind.MANUAL_ENTRY, ProvenanceRecordType.MANUAL_ENTRY,
                null, null, OffsetDateTime.now())
        ));
        when(wisdomService.search(anyString())).thenReturn(List.of(
            new WisdomEntryEntity(UUID.randomUUID(), "Small steps win", WisdomStatus.ACCEPTED, null,
                OffsetDateTime.now().minusDays(2), OffsetDateTime.now())
        ));
        when(retrospectiveService.search(anyString())).thenReturn(List.of(
            new WeeklyRetrospectiveEntity(UUID.randomUUID(), OffsetDateTime.now().minusDays(7), OffsetDateTime.now(),
                "Weekly summary", "Assist text", OffsetDateTime.now())
        ));
        when(semanticRetrievalService.retrieve(anyString(), Mockito.anyInt())).thenReturn(List.of(
            new SemanticRetrievalService.SemanticMatch(
                "WISDOM",
                UUID.randomUUID(),
                "Consistency compounds",
                OffsetDateTime.now().toString(),
                0.72
            )
        ));

        var service = new StructuredSearchService(
            reflectionService,
            beliefService,
            evidenceService,
            wisdomService,
            retrospectiveService,
            semanticIndexingService,
            semanticRetrievalService
        );

        var results = service.search("consistency");

        assertTrue(results.size() >= 5);
        assertTrue(results.stream().anyMatch(result -> "SEMANTIC".equals(result.matchType()) || "HYBRID".equals(result.matchType())));
    }
}
