package com.helix.api.onboarding.domain;

/**
 * Server-persisted onboarding progress (Phase 7), replacing the purely client-derived
 * {@code transformations.length === 0} welcome-state check. Mirrors the two existing gates
 * Today's UI already had (no transformations yet -> welcome screen; a transformation but no
 * experiment yet -> "add an experiment" prompt) so the transition points are exactly the moments
 * that already mattered to the UI, not a new invented concept.
 */
public enum OnboardingStatus {
    /** No transformation has ever been created. */
    NOT_STARTED,
    /** At least one transformation exists, but the guided first-experiment step isn't done yet. */
    FIRST_TRANSFORMATION_CREATED,
    /** At least one experiment has been created — the guided setup loop has been completed once. */
    COMPLETE
}
