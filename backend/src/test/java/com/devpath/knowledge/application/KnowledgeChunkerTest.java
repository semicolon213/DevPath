package com.devpath.knowledge.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class KnowledgeChunkerTest {
    private final KnowledgeChunker chunker = new KnowledgeChunker();

    @Test void createsDeterministicBoundedChunksWithHeadingContext() {
        String content="# Architecture\n\n"+"a".repeat(1900)+"\n\n## Tests\n\nJUnit coverage";
        var first=chunker.chunk(content); var repeated=chunker.chunk(content);
        assertThat(first).hasSizeBetween(3,4).isEqualTo(repeated);
        assertThat(first).allSatisfy(chunk -> { assertThat(chunk.content()).hasSizeLessThanOrEqualTo(1800); assertThat(chunk.contentHash()).hasSize(64); });
        assertThat(first.getLast().heading()).isEqualTo("Tests");
    }

    @Test
    void startsANewChunkBeforeAssigningTheNextSectionHeading() {
        var chunks = new KnowledgeChunker().chunk("# First\n\nfirst body\n\n# Second\n\nsecond body");

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).heading()).isEqualTo("First");
        assertThat(chunks.get(0).content()).contains("first body").doesNotContain("Second");
        assertThat(chunks.get(1).heading()).isEqualTo("Second");
        assertThat(chunks.get(1).content()).contains("second body");
    }

    @Test void rejectsEmptyAndOversizedContent() {
        assertThatThrownBy(() -> chunker.chunk("  ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> chunker.chunk("a".repeat(200_001))).isInstanceOf(IllegalArgumentException.class);
    }
}
