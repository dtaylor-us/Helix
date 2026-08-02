package com.helix.api.shared;

import com.helix.api.identity.application.CurrentUserProvider;
import com.helix.api.reflection.application.ReflectionService;
import com.helix.api.reflection.domain.ReflectionEntity;
import com.helix.api.shared.adapter.out.embedding.LocalHashEmbeddingAdapter;
import com.helix.api.shared.adapter.out.persistence.SemanticSearchDocumentRepository;
import com.helix.api.shared.application.SemanticIndexingService;
import com.helix.api.wisdom.application.WisdomService;
import com.helix.api.wisdom.domain.WisdomEntryEntity;
import com.helix.api.wisdom.domain.WisdomStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class SemanticIndexingServiceTest {

    @Test
    void rebuildIndexesReflectionsAndWisdom() {
        var reflectionService = Mockito.mock(ReflectionService.class);
        var wisdomService = Mockito.mock(WisdomService.class);
        var repository = Mockito.mock(SemanticSearchDocumentRepository.class);

        when(reflectionService.listForRetrieval()).thenReturn(List.of(
            new ReflectionEntity(UUID.randomUUID(), UUID.randomUUID(), "Daily consistency note", OffsetDateTime.now())
        ));
        when(wisdomService.list()).thenReturn(List.of(
            new WisdomEntryEntity(UUID.randomUUID(), "Small steps compound", WisdomStatus.ACCEPTED, null,
                OffsetDateTime.now().minusDays(1), OffsetDateTime.now())
        ));

        var currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        when(currentUserProvider.currentUserId()).thenReturn(UUID.randomUUID());
        var service = new SemanticIndexingService(
            reflectionService,
            wisdomService,
            repository,
            new LocalHashEmbeddingAdapter(),
            currentUserProvider
        );

        var result = service.rebuild();

        var savedCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(repository).saveAll(savedCaptor.capture());
        assertEquals(2, savedCaptor.getValue().size());
        assertEquals(2, result.indexedCount());
        assertEquals("local-hash-v1", result.embeddingModel());
        assertTrue(result.indexedAt() != null && !result.indexedAt().isBlank());
    }
}