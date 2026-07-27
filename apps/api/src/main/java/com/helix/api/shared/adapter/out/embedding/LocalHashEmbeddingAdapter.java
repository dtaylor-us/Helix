package com.helix.api.shared.adapter.out.embedding;

import com.helix.api.shared.application.TextEmbeddingPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Component
public class LocalHashEmbeddingAdapter implements TextEmbeddingPort {

    private static final int VECTOR_SIZE = 48;

    @Override
    public List<Double> embed(String text) {
        var vector = new double[VECTOR_SIZE];
        var normalized = text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
        if (normalized.isEmpty()) {
            return toList(vector);
        }

        Arrays.stream(normalized.split("[^a-z0-9]+"))
            .filter(token -> !token.isBlank())
            .forEach(token -> {
                int hash = token.hashCode();
                int index = Math.floorMod(hash, VECTOR_SIZE);
                int signBit = Math.floorMod(hash / VECTOR_SIZE, 2);
                double sign = signBit == 0 ? 1d : -1d;
                vector[index] += sign;
            });

        normalize(vector);
        return toList(vector);
    }

    @Override
    public String modelName() {
        return "local-hash-v1";
    }

    private static void normalize(double[] vector) {
        double magnitudeSquared = 0d;
        for (double value : vector) {
            magnitudeSquared += value * value;
        }
        if (magnitudeSquared == 0d) {
            return;
        }

        double magnitude = Math.sqrt(magnitudeSquared);
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / magnitude;
        }
    }

    private static List<Double> toList(double[] vector) {
        var output = new ArrayList<Double>(vector.length);
        for (double value : vector) {
            output.add(value);
        }
        return output;
    }
}