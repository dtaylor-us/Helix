package com.helix.api.transformation.adapter.in.http;

import com.helix.api.transformation.application.TransformationService;
import com.helix.api.transformation.domain.TransformationEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transformations")
public class TransformationController {

    private final TransformationService service;

    public TransformationController(TransformationService service) {
        this.service = service;
    }

    @PostMapping
    public TransformationDto create(@Valid @RequestBody CreateTransformationRequest request) {
        return toDto(service.create(request.title(), request.purpose(), request.desiredIdentity(), request.obstacle()));
    }

    @GetMapping
    public List<TransformationDto> list() {
        return service.list().stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    public TransformationDto get(@PathVariable UUID id) {
        return toDto(service.get(id));
    }

    private TransformationDto toDto(TransformationEntity entity) {
        return new TransformationDto(
            entity.getId(),
            entity.getTitle(),
            entity.getPurpose(),
            entity.getDesiredIdentity(),
            entity.getObstacle(),
            entity.getCreatedAt().toString()
        );
    }

    public record CreateTransformationRequest(
        @NotBlank @Size(max = 140) String title,
        @Size(max = 2000) String purpose,
        @Size(max = 2000) String desiredIdentity,
        @Size(max = 2000) String obstacle
    ) {}

    public record TransformationDto(
        UUID id, String title, String purpose, String desiredIdentity, String obstacle, String createdAt
    ) {}
}
