package com.helix.api.today;

import com.helix.api.experiments.domain.ExperimentEntity;
import com.helix.api.experiments.domain.ExperimentStatus;
import com.helix.api.onboarding.domain.OnboardingStatus;
import com.helix.api.today.adapter.in.http.CurrentFocusController;
import com.helix.api.today.application.CurrentFocusService;
import com.helix.api.today.application.TodayService;
import com.helix.api.transformation.domain.TransformationEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CurrentFocusControllerTest {

    private final CurrentFocusService service = Mockito.mock(CurrentFocusService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CurrentFocusController(service)).build();

    @Test
    void returnsNotStartedOnboardingWithNoActiveExperiment() throws Exception {
        Mockito.when(service.snapshot()).thenReturn(
            new CurrentFocusService.CurrentFocusSnapshot(OnboardingStatus.NOT_STARTED, List.of(), null)
        );

        mockMvc.perform(get("/api/v1/current-focus"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.onboardingStatus").value("NOT_STARTED"))
            .andExpect(jsonPath("$.transformations").isEmpty())
            .andExpect(jsonPath("$.hasActiveExperiment").value(false))
            .andExpect(jsonPath("$.activeExperiment").doesNotExist());
    }

    @Test
    void returnsTransformationsAndActiveExperimentWhenPresent() throws Exception {
        var transformationId = UUID.randomUUID();
        var transformation = new TransformationEntity(
            transformationId, "Become more peaceful", "Practice steadiness", OffsetDateTime.now()
        );
        var experimentId = UUID.randomUUID();
        var experiment = new ExperimentEntity(
            experimentId, transformationId, "Pause before responding", "Pausing helps",
            "Breathe once", ExperimentStatus.ACTIVE, OffsetDateTime.now()
        );
        Mockito.when(service.snapshot()).thenReturn(new CurrentFocusService.CurrentFocusSnapshot(
            OnboardingStatus.COMPLETE,
            List.of(transformation),
            new TodayService.TodaySnapshot(experiment, List.of(), List.of())
        ));

        mockMvc.perform(get("/api/v1/current-focus"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.onboardingStatus").value("COMPLETE"))
            .andExpect(jsonPath("$.transformations[0].title").value("Become more peaceful"))
            .andExpect(jsonPath("$.hasActiveExperiment").value(true))
            .andExpect(jsonPath("$.activeExperiment.title").value("Pause before responding"));
    }
}
