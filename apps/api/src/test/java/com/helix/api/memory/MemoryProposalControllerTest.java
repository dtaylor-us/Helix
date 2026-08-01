package com.helix.api.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helix.api.memory.adapter.in.http.MemoryProposalController;
import com.helix.api.memory.application.MemoryProposalService;
import com.helix.api.shared.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MemoryProposalControllerTest {

    private final MemoryProposalService service = Mockito.mock(MemoryProposalService.class);
    private final MockMvc mockMvc = MockMvcBuilders
        .standaloneSetup(new MemoryProposalController(service))
        .setControllerAdvice(new ApiExceptionHandler())
        .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createReturnsBadRequestWithActionableSourceMessage() throws Exception {
        Mockito.when(service.create(
            eq("Small actions protect consistency."),
            eq(com.helix.api.memory.domain.MemorySourceKind.AI_DERIVED),
            eq(com.helix.api.memory.domain.MemorySourceRecordType.REFLECTION),
            any(UUID.class),
            eq("Reflection excerpt")
        )).thenThrow(new IllegalArgumentException("That source record couldn't be found — check the ID/type and try again."));

        mockMvc.perform(post("/api/v1/memory/proposals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new MemoryProposalController.CreateMemoryProposalRequest(
                    "Small actions protect consistency.",
                    com.helix.api.memory.domain.MemorySourceKind.AI_DERIVED,
                    com.helix.api.memory.domain.MemorySourceRecordType.REFLECTION,
                    UUID.randomUUID(),
                    "Reflection excerpt"
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("That source record couldn't be found — check the ID/type and try again."));
    }
}
