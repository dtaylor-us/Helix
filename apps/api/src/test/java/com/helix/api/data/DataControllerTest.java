package com.helix.api.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helix.api.data.adapter.in.http.DataController;
import com.helix.api.data.application.DataDeletionService;
import com.helix.api.data.application.DataExportService;
import com.helix.api.onboarding.domain.OnboardingStatus;
import com.helix.api.shared.ApiExceptionHandler;
import com.helix.api.transformation.domain.TransformationEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DataControllerTest {

    private final DataExportService exportService = Mockito.mock(DataExportService.class);
    private final DataDeletionService deletionService = Mockito.mock(DataDeletionService.class);
    private final MockMvc mockMvc = MockMvcBuilders
        .standaloneSetup(new DataController(exportService, deletionService))
        .setControllerAdvice(new ApiExceptionHandler())
        .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void exportReturnsEveryModuleIncludingOnboardingStatus() throws Exception {
        var transformationId = UUID.randomUUID();
        Mockito.when(exportService.export()).thenReturn(new DataExportService.DataExportSnapshot(
            OnboardingStatus.COMPLETE,
            List.of(new TransformationEntity(transformationId, "Become more peaceful", "Practice steadiness", OffsetDateTime.now())),
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of()
        ));

        mockMvc.perform(get("/api/v1/data/export"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.onboardingStatus").value("COMPLETE"))
            .andExpect(jsonPath("$.transformations[0].title").value("Become more peaceful"));
    }

    @Test
    void deleteRejectsRequestWithoutConfirmation() throws Exception {
        mockMvc.perform(delete("/api/v1/data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DataController.DeleteAllDataRequest(false))))
            .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(deletionService);
    }

    @Test
    void deleteWithConfirmationCallsDeletionServiceAndReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DataController.DeleteAllDataRequest(true))))
            .andExpect(status().isNoContent());

        Mockito.verify(deletionService).deleteEverything();
    }
}
