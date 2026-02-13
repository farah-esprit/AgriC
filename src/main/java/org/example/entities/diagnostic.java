package org.example.entities;

import java.time.LocalDate;

public class diagnostic {
    private int id;
    private int cultureId; // Référence à la culture
    private LocalDate dateDiagnostic;
    private String etat; // Ex: "Sain", "Maladie détectée"
    private String recommandations; // Conseils pour la culture
    private String details; // Infos complémentaires

    public diagnostic() {}

    public diagnostic(int id, int cultureId, LocalDate dateDiagnostic, String etat,
                      String recommandations, String details) {
        this.id = id;
        this.cultureId = cultureId;
        this.dateDiagnostic = dateDiagnostic;
        this.etat = etat;
        this.recommandations = recommandations;
        this.details = details;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCultureId() { return cultureId; }
    public void setCultureId(int cultureId) { this.cultureId = cultureId; }
    public LocalDate getDateDiagnostic() { return dateDiagnostic; }
    public void setDateDiagnostic(LocalDate dateDiagnostic) { this.dateDiagnostic = dateDiagnostic; }
    public String getEtat() { return etat; }
    public void setEtat(String etat) { this.etat = etat; }
    public String getRecommandations() { return recommandations; }
    public void setRecommandations(String recommandations) { this.recommandations = recommandations; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
