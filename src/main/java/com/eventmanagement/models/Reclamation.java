package com.eventmanagement.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Reclamation {
    private int idReclamation;
    private String objet;
    private String description;
    private LocalDate dateCreation;
    private String statut; // EN_ATTENTE, EN_COURS, TRAITEE, CLOTUREE
    private String priorite;
    private String type;
    private int idUtilisateur;
    private String reponseAdmin;
    private LocalDate dateReponse;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public Reclamation() {}
    
    public Reclamation(int idReclamation, String objet, String description, LocalDate dateCreation,
                       String statut, String priorite, String type, int idUtilisateur) {
        this.idReclamation = idReclamation;
        this.objet = objet;
        this.description = description;
        this.dateCreation = dateCreation;
        this.statut = statut;
        this.priorite = priorite;
        this.type = type;
        this.idUtilisateur = idUtilisateur;
    }

    public Reclamation(String objet, String description, LocalDate dateCreation,
                       String statut, String priorite, String type, int idUtilisateur) {
        this.objet = objet;
        this.description = description;
        this.dateCreation = dateCreation;
        this.statut = statut != null ? statut : "EN_ATTENTE";
        this.priorite = priorite;
        this.type = type;
        this.idUtilisateur = idUtilisateur;
    }
    
    public int getIdReclamation() {
        return idReclamation;
    }
    
    public void setIdReclamation(int idReclamation) {
        this.idReclamation = idReclamation;
    }
    
    public String getObjet() {
        return objet;
    }
    
    public void setObjet(String objet) {
        this.objet = objet;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDate getDateCreation() {
        return dateCreation;
    }
    
    public void setDateCreation(LocalDate dateCreation) {
        this.dateCreation = dateCreation;
    }
    
    public String getStatut() {
        return statut;
    }
    
    public void setStatut(String statut) {
        this.statut = statut;
    }
    
    public String getPriorite() {
        return priorite;
    }
    
    public void setPriorite(String priorite) {
        this.priorite = priorite;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public int getIdUtilisateur() {
        return idUtilisateur;
    }
    
    public void setIdUtilisateur(int idUtilisateur) {
        this.idUtilisateur = idUtilisateur;
    }

    public String getReponseAdmin() {
        return reponseAdmin;
    }

    public void setReponseAdmin(String reponseAdmin) {
        this.reponseAdmin = reponseAdmin;
    }

    public LocalDate getDateReponse() {
        return dateReponse;
    }

    public void setDateReponse(LocalDate dateReponse) {
        this.dateReponse = dateReponse;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
