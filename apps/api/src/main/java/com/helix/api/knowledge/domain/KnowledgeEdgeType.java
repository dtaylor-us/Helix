package com.helix.api.knowledge.domain;

/**
 * The 14 deterministic edge types the first release (11B/11C) supports, per the category A/B split
 * in docs/product/knowledge-graph-scoping.md Section 4, plus BELIEF_RELATED_TO_BELIEF (Phase 11E):
 * the one edge type this graph ever creates from an AI judgment rather than an explicit/deterministic
 * domain relationship -- always origin AI_PROPOSED, status PROPOSED, never auto-confirmed (ADR-020,
 * ADR-008). Every other proposed-brief edge type (EXPERIMENT_FOLLOWED_BY_EXPERIMENT, anything
 * Value/Growth-Dimension/Insight-related, BELIEF_REVISED_FROM/WISDOM_REVISED_FROM) remains
 * deliberately deferred -- see the scoping doc for the reasoning behind each.
 */
public enum KnowledgeEdgeType {
    TRANSFORMATION_CONTAINS_BELIEF,
    TRANSFORMATION_CONTAINS_EXPERIMENT,
    TRANSFORMATION_PRODUCED_WISDOM,
    BELIEF_SUPPORTED_BY_EVIDENCE,
    BELIEF_CHALLENGED_BY_EVIDENCE,
    BELIEF_EXPLORED_BY_EXPERIMENT,
    EXPERIMENT_PRODUCED_EVIDENCE,
    EXPERIMENT_INFORMED_WISDOM,
    REFLECTION_PRODUCED_EVIDENCE,
    REFLECTION_REFERENCES_EXPERIMENT,
    REFLECTION_REFERENCES_TRANSFORMATION,
    WISDOM_SUPPORTED_BY_EVIDENCE,
    WISDOM_EMERGED_FROM_REFLECTION,
    MEMORY_DERIVED_FROM,
    BELIEF_RELATED_TO_BELIEF
}
