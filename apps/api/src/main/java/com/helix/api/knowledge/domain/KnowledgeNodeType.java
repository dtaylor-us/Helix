package com.helix.api.knowledge.domain;

/**
 * Phase 11B (ADR-020). {@code Value} and {@code Growth Dimension} are deliberately excluded --
 * neither concept exists anywhere in Helix's domain model or product docs today
 * (see docs/product/knowledge-graph-scoping.md Section 3).
 */
public enum KnowledgeNodeType {
    TRANSFORMATION,
    BELIEF,
    EXPERIMENT,
    EVIDENCE,
    REFLECTION,
    WISDOM,
    MEMORY
}
