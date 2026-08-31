package com.devpath.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devpath.ai.config.AiGenerationProperties;
import com.devpath.ai.domain.GenerationJob;
import com.devpath.prompt.application.PromptPackage;
import com.devpath.prompt.application.PromptTemplateVersion;
import com.devpath.prompt.application.SkillExplanationPromptBuilder;
import com.devpath.rule.application.SkillMatrixApplicationService;
import com.devpath.rule.application.SkillMatrixView;
import com.devpath.shared.application.ObjectContentPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiGenerationApplicationServiceTest {
    static final Instant NOW = Instant.parse("2026-08-31T00:00:00Z");
    @Mock GenerationPersistencePort persistence;
    @Mock SkillMatrixApplicationService matrices;
    @Mock SkillExplanationPromptBuilder prompts;
    @Mock SkillExplanationValidator validator;
    @Mock ObjectContentPort objects;
    @Mock AiAuditPort audit;
    @Mock AiGenerationTransactionService transactions;
    AiGenerationApplicationService service;

    @BeforeEach
    void setUp() {
        service = new AiGenerationApplicationService(persistence, matrices, prompts, validator, objects,
            new AiGenerationProperties("http://localhost", "qwen-test", Duration.ofSeconds(1),
                Duration.ofSeconds(2), 2), audit, transactions, new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsOneLockedContextFromAnOwnerScopedSkillMatrix() {
        UUID owner = UUID.randomUUID(), matrixId = UUID.randomUUID(), templateId = UUID.randomUUID();
        SkillMatrixView matrix = org.mockito.Mockito.mock(SkillMatrixView.class);
        when(persistence.findByOwnerAndIdempotencyKey(owner, "key")).thenReturn(Optional.empty());
        when(matrices.get(owner, matrixId)).thenReturn(matrix);
        when(persistence.loadActiveTemplate(AiGenerationApplicationService.TASK_TYPE)).thenReturn(
            new PromptTemplateVersion(templateId, AiGenerationApplicationService.TASK_TYPE, "v1",
                "Never calculate scores.", "Return JSON."));
        when(prompts.build(eq(matrix), any())).thenReturn(new PromptPackage("{}", "prompt", "a".repeat(64), 4096));
        when(persistence.saveJob(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(persistence.findArtifactIdByPromptContext(any())).thenReturn(Optional.empty());

        GenerationJobView result = service.request(owner, AiGenerationApplicationService.TASK_TYPE,
            List.of(matrixId), AiGenerationApplicationService.OUTPUT_TYPE, "key");

        ArgumentCaptor<StoredPromptContext> context = ArgumentCaptor.forClass(StoredPromptContext.class);
        verify(persistence).saveContext(context.capture());
        assertThat(context.getValue().userId()).isEqualTo(owner);
        assertThat(context.getValue().skillMatrixId()).isEqualTo(matrixId);
        assertThat(context.getValue().templateVersionId()).isEqualTo(templateId);
        assertThat(result.status()).isEqualTo("QUEUED");
    }

    @Test
    void persistsAnArtifactOnlyAfterValidationPasses() {
        UUID owner = UUID.randomUUID(), context = UUID.randomUUID(), matrix = UUID.randomUUID(), execution = UUID.randomUUID();
        GenerationJob running = GenerationJob.queue(owner, context, "key", AiGenerationApplicationService.TASK_TYPE,
            NOW.minusSeconds(1)).start(NOW.minusMillis(500));
        GenerationWorkItem item = new GenerationWorkItem(running, matrix, "prompt");
        SkillMatrixView matrixView = org.mockito.Mockito.mock(SkillMatrixView.class);
        var content = new SkillExplanationContent("grounded", List.of(), List.of());
        when(transactions.providerCompleted(item, execution,
            new GenerationProviderResult("raw", 10, 5), 12, NOW))
            .thenReturn(Optional.of(running));
        when(matrices.get(owner, matrix)).thenReturn(matrixView);
        when(validator.validate("raw", matrixView)).thenReturn(new SkillExplanationValidation(content, List.of()));
        when(objects.put(eq(owner), any(), eq(running.id()), eq("skill-explanation.json"), any()))
            .thenReturn("object://owner/artifact");
        when(transactions.validated(eq(item), eq(execution), any(), eq("object://owner/artifact"), any(), eq(NOW)))
            .thenReturn(true);

        service.providerSucceeded(item, execution,
            new GenerationProviderResult("raw", 10, 5), 12);

        verify(transactions).validated(eq(item), eq(execution), any(), eq("object://owner/artifact"), any(), eq(NOW));
        verify(persistence, never()).recordRejectedResponse(any(), any(), any(), any(), any());
    }
}
