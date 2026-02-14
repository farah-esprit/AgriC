package org.example.services;

import org.example.entities.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DiagnosticService {

    private final List<Diagnostic> diagnostics = new ArrayList<>();

    public void ajouter(Diagnostic diag) {
        diag.setId(diagnostics.size() + 1); // simple id auto-increment
        diagnostics.add(diag);
    }

    public List<Diagnostic> getByCultureId(int cultureId) {
        return diagnostics.stream()
                .filter(d -> d.getCultureId() == cultureId)
                .collect(Collectors.toList());
    }
}
