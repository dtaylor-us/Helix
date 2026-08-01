package com.helix.api.today.application;

import com.helix.api.onboarding.application.OnboardingService;
import com.helix.api.onboarding.domain.OnboardingStatus;
import com.helix.api.transformation.application.TransformationService;
import com.helix.api.transformation.domain.TransformationEntity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Phase 7: a single projection combining everything Today needs, so the client no longer has to
 * make two calls ({@code /today} + {@code /transformations}) and re-derive a welcome/first-use
 * state client-side from {@code transformations.length === 0}. {@code /today} and
 * {@code /transformations} are both left unchanged for any other caller.
 */
@Service
public class CurrentFocusService {

    private final TodayService todayService;
    private final TransformationService transformationService;
    private final OnboardingService onboardingService;

    public CurrentFocusService(
        TodayService todayService, TransformationService transformationService, OnboardingService onboardingService
    ) {
        this.todayService = todayService;
        this.transformationService = transformationService;
        this.onboardingService = onboardingService;
    }

    public CurrentFocusSnapshot snapshot() {
        OnboardingStatus onboardingStatus = onboardingService.get().getStatus();
        List<TransformationEntity> transformations = transformationService.list();
        var todaySnapshot = todayService.snapshot();
        return new CurrentFocusSnapshot(onboardingStatus, transformations, todaySnapshot.orElse(null));
    }

    public record CurrentFocusSnapshot(
        OnboardingStatus onboardingStatus,
        List<TransformationEntity> transformations,
        TodayService.TodaySnapshot today
    ) {}
}
