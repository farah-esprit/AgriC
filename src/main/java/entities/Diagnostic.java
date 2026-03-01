package entities;

import java.time.LocalDate;

public class Diagnostic {

    private int idDiagnostic;
    private int idCulture;
    private int idUser;

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    private String photoPath;

    private LocalDate dateDiagnostic;
    private String etat;
    private String symptomes;
    private String informationsComplementaires;
    private String recommandations;

    private String diseaseDetected;
    private double confidence;
    private String rapportIa;

    public Diagnostic() {}

    // ================= GETTERS & SETTERS =================

    public int getIdDiagnostic() {
        return idDiagnostic;
    }

    public void setIdDiagnostic(int idDiagnostic) {
        this.idDiagnostic = idDiagnostic;
    }

    public int getIdCulture() {
        return idCulture;
    }

    public void setIdCulture(int idCulture) {
        this.idCulture = idCulture;
    }

    public int getIdUser() {
        return idUser;
    }


    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public LocalDate getDateDiagnostic() {
        return dateDiagnostic;
    }

    public void setDateDiagnostic(LocalDate dateDiagnostic) {
        this.dateDiagnostic = dateDiagnostic;
    }

    public String getEtat() {
        return etat;
    }

    public void setEtat(String etat) {
        this.etat = etat;
    }

    public String getSymptomes() {
        return symptomes;
    }

    public void setSymptomes(String symptomes) {
        this.symptomes = symptomes;
    }

    public String getInformationsComplementaires() {
        return informationsComplementaires;
    }

    public void setInformationsComplementaires(String informationsComplementaires) {
        this.informationsComplementaires = informationsComplementaires;
    }

    public String getRecommandations() {
        return recommandations;
    }

    public void setRecommandations(String recommandations) {
        this.recommandations = recommandations;
    }

    public String getDiseaseDetected() {
        return diseaseDetected;
    }

    public void setDiseaseDetected(String diseaseDetected) {
        this.diseaseDetected = diseaseDetected;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getRapportIa() {
        return rapportIa;
    }

    public void setRapportIa(String rapportIa) {
        this.rapportIa = rapportIa;
    }
}
