package com.helix.api.reflection.adapter.in.http;

import com.helix.api.reflection.application.ReflectionChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class ReflectionChatController {

    private final ReflectionChatService service;

    public ReflectionChatController(ReflectionChatService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/experiments/{experimentId}/reflection-chat/turn")
    public ReflectionChatTurnResponse nextTurn(
        @PathVariable UUID experimentId,
        @Valid @RequestBody ReflectionChatRequest request
    ) {
        var response = service.nextTurn(experimentId, toMessages(request.transcript()));
        return new ReflectionChatTurnResponse(
            response.text(),
            response.deterministicFallback() ? "DETERMINISTIC" : "AI",
            response.provider(),
            response.model()
        );
    }

    @PostMapping("/api/v1/experiments/{experimentId}/reflection-chat/finish")
    public ReflectionChatFinishResponse finish(
        @PathVariable UUID experimentId,
        @Valid @RequestBody ReflectionChatRequest request
    ) {
        var response = service.finish(experimentId, toMessages(request.transcript()));
        return new ReflectionChatFinishResponse(
            response.content(),
            response.attempted(),
            response.noticed(),
            response.evidenceNoted(),
            response.surprise(),
            response.deterministicFallback() ? "DETERMINISTIC" : "AI",
            response.provider(),
            response.model()
        );
    }

    private List<ReflectionChatService.ChatMessage> toMessages(List<ChatMessageDto> transcript) {
        return transcript.stream().map(message -> new ReflectionChatService.ChatMessage(message.role(), message.text())).toList();
    }

    public record ReflectionChatRequest(@NotNull @Valid List<ChatMessageDto> transcript) {}

    public record ChatMessageDto(
        @NotBlank @Pattern(regexp = "user|assistant") String role,
        @NotBlank @Size(max = 4000) String text
    ) {}

    public record ReflectionChatTurnResponse(String text, String source, String aiProvider, String aiModel) {}

    public record ReflectionChatFinishResponse(
        String content,
        Boolean attempted,
        String noticed,
        String evidenceNoted,
        String surprise,
        String source,
        String aiProvider,
        String aiModel
    ) {}
}
