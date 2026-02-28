package org.example.entities;

public class MLResponse {
    private String diseaseStatus;
    private double confidence;

    public MLResponse() { }

    public MLResponse(String diseaseStatus, double confidence) {
        this.diseaseStatus = diseaseStatus;
        this.confidence = confidence;
    }

    public String getDiseaseStatus() {
        return diseaseStatus;
    }

    public void setDiseaseStatus(String diseaseStatus) {
        this.diseaseStatus = diseaseStatus;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
}
