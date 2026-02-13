package org.example.services;

import org.example.entities.diagnostic;
import org.example.entities.diagnostic;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DiagnosticService {

    private List<diagnostic> diagnostics = new ArrayList<>();
    private int nextId = 1;

    // Ajouter un diagnostic
    public void ajouter(diagnostic diag) {
        diag.setId(nextId++);
        diagnostics.add(diag);
    }

    // Lister tous les diagnostics
    public List<diagnostic> getAll() {
        return diagnostics;
    }

    // Lister les diagnostics pour une culture
    public List<diagnostic> getByCulture(int cultureId) {
        return diagnostics.stream()
                .filter(d -> d.getCultureId() == cultureId)
                .collect(Collectors.toList());
    }
}
