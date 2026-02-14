package org.example.entities;

import java.time.LocalDate;

public class Diagnostic {

    private int id;
    private int cultureId;
    private LocalDate dateDiagnostic;
    private String etat; // État de la culture
    private String symptomes; // Symptômes observés
    private String informationsComplementaires; // Infos complémentaires
    private String recommandations; // Conseils
    private String photo; // chemin image/photo

    public Diagnostic() {}

    public Diagnostic(int id, int cultureId, LocalDate dateDiagnostic, String etat,
                      String symptomes, String informationsComplementaires,
                      String recommandations, String photo) {
        this.id = id;
        this.cultureId = cultureId;
        this.dateDiagnostic = dateDiagnostic;
        this.etat = etat;
        this.symptomes = symptomes;
        this.informationsComplementaires = informationsComplementaires;
        this.recommandations = recommandations;
        this.photo = photo;
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

    public String getSymptomes() { return symptomes; }
    public void setSymptomes(String symptomes) { this.symptomes = symptomes; }

    public String getInformationsComplementaires() { return informationsComplementaires; }
    public void setInformationsComplementaires(String informationsComplementaires) { this.informationsComplementaires = informationsComplementaires; }

    public String getRecommandations() { return recommandations; }
    public void setRecommandations(String recommandations) { this.recommandations = recommandations; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }
}
