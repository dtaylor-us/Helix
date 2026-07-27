package com.helix.api.shared.application;

import java.util.List;

public interface TextEmbeddingPort {
    List<Double> embed(String text);

    String modelName();
}