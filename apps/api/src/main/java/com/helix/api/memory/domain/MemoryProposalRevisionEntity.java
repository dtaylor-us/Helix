package com.helix.api.memory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "memory_proposal_revisions")
public class MemoryProposalRevisionEntity {

    @Id
    private UUID id;

    @Column(name = "memory_proposal_id", nullable = false)
    private UUID memoryProposalId;

    @Column(name = "previous_statement", nullable = false, columnDefinition = "text")
    private String previousStatement;

    @Column(name = "new_statement", nullable = false, columnDefinition = "text")
    private String newStatement;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false, length = 32)
    private MemoryProposalStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 32)
    private MemoryProposalStatus newStatus;

    @Column(nullable = false, columnDefinition = "text")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected MemoryProposalRevisionEntity() {
    }

    public MemoryProposalRevisionEntity(UUID id, UUID memoryProposalId, String previousStatement, String newStatement,
                                        MemoryProposalStatus previousStatus, MemoryProposalStatus newStatus,
                                        String reason, OffsetDateTime createdAt) {
        this.id = id;
        this.memoryProposalId = memoryProposalId;
        this.previousStatement = previousStatement;
        this.newStatement = newStatement;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMemoryProposalId() {
        return memoryProposalId;
    }

    public String getPreviousStatement() {
        return previousStatement;
    }

    public String getNewStatement() {
        return newStatement;
    }

    public MemoryProposalStatus getPreviousStatus() {
        return previousStatus;
    }

    public MemoryProposalStatus getNewStatus() {
        return newStatus;
    }

    public String getReason() {
        return reason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}