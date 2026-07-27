package com.helix.api.shared;

import com.helix.api.shared.adapter.out.embedding.LocalHashEmbeddingAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalHashEmbeddingAdapterTest {

    @Test
    void embedIsDeterministicAndStable() {
        var adapter = new LocalHashEmbeddingAdapter();

        var first = adapter.embed("Consistency builds momentum");
        var second = adapter.embed("Consistency builds momentum");

        assertEquals(48, first.size());
        assertEquals(first, second);
        assertEquals("local-hash-v1", adapter.modelName());
    }
}