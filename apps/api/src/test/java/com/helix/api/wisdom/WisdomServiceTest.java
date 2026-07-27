package com.helix.api.wisdom;

import com.helix.api.evidence.application.EvidenceService;
import com.helix.api.reflection.application.ReflectionService;
import com.helix.api.reflection.domain.ReflectionEntity;
import com.helix.api.wisdom.adapter.out.persistence.WisdomEntryRepository;
import com.helix.api.wisdom.adapter.out.persistence.WisdomRevisionRepository;
import com.helix.api.wisdom.adapter.out.persistence.WisdomSourceLinkRepository;
import com.helix.api.wisdom.application.WeeklyRetrospectiveService;
import com.helix.api.wisdom.application.WisdomService;
import com.helix.api.wisdom.domain.WisdomSourceType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class WisdomServiceTest {

    @Test
    void createAndReviseWisdomPreservesRevisionHistory() {
        var repository = Mockito.mock(WisdomEntryRepository.class);
        var revisionRepository = Mockito.mock(WisdomRevisionRepository.class);
        var sourceRepository = Mockito.mock(WisdomSourceLinkRepository.class);
        var retrospectiveService = Mockito.mock(WeeklyRetrospectiveService.class);
        var reflectionService = Mockito.mock(ReflectionService.class);
        var evidenceService = Mockito.mock(EvidenceService.class);

        var reflectionId = UUID.randomUUID();

        when(reflectionService.get(reflectionId)).thenReturn(new ReflectionEntity(
            reflectionId,
            UUID.randomUUID(),
            "Reflection source",
            OffsetDateTime.now().minusDays(1)
        ));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(revisionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var service = new WisdomService(repository, revisionRepository, sourceRepository, retrospectiveService, reflectionService, evidenceService);

        var entry = service.create(
            "Small steps protect consistency.",
            null,
            List.of(new WisdomService.WisdomSourceInput(WisdomSourceType.REFLECTION, reflectionId, "Weekly theme"))
        );
        when(repository.findById(entry.getId())).thenReturn(java.util.Optional.of(entry));

        var revision = service.revise(entry.getId(), "Small steps create compounding progress.", "Week over week evidence");

        assertEquals("Small steps protect consistency.", revision.getPreviousStatement());
        assertEquals("Small steps create compounding progress.", revision.getNewStatement());
    }
}
