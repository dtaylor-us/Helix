package com.helix.api.transformation.application;

import com.helix.api.transformation.adapter.out.persistence.TransformationRepository;
import com.helix.api.transformation.domain.TransformationEntity;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class TransformationService {

    private final TransformationRepository repository;

    public TransformationService(TransformationRepository repository) {
        this.repository = repository;
    }

    public TransformationEntity create(String title, String purpose) {
        var entity = new TransformationEntity(UUID.randomUUID(), title.trim(), purpose, OffsetDateTime.now());
        return repository.save(entity);
    }

    public List<TransformationEntity> list() {
        return repository.findAll();
    }

    public TransformationEntity get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("Transformation not found"));
    }
}
