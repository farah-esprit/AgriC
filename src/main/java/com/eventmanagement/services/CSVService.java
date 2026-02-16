package com.eventmanagement.services;

import com.eventmanagement.models.Evenement;
import com.eventmanagement.models.Reclamation;
import com.eventmanagement.models.Utilisateur;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public class CSVService {
    
    public static void exportEvenementsToCSV(List<Evenement> evenements, String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("ID,Titre,Description,Date Début,Date Fin,Lieu,Capacité Max,Statut,Raison Rejet,Organisateur ID,Date Création\n");
            
            for (Evenement e : evenements) {
                writer.write(String.format("%d,\"%s\",\"%s\",%s,%s,\"%s\",%d,\"%s\",\"%s\",%d,%s\n",
                        e.getIdEvenement(),
                        escapeCSV(e.getTitre()),
                        escapeCSV(e.getDescription() != null ? e.getDescription() : ""),
                        e.getDateDebut(),
                        e.getDateFin(),
                        escapeCSV(e.getLieu()),
                        e.getCapaciteMax(),
                        e.getStatut(),
                        escapeCSV(e.getRaisonRejet() != null ? e.getRaisonRejet() : ""),
                        e.getOrganisateurId(),
                        e.getCreatedAt() != null ? e.getCreatedAt().toString() : ""
                ));
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la génération du CSV: " + e.getMessage());
        }
    }
    
    public static void exportReclamationsToCSV(List<Reclamation> reclamations, String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("ID,Objet,Description,Date Création,Statut,Priorité,Type,Réponse Admin,Date Réponse,Utilisateur ID\n");
            
            for (Reclamation r : reclamations) {
                writer.write(String.format("%d,\"%s\",\"%s\",%s,\"%s\",\"%s\",\"%s\",\"%s\",%s,%d\n",
                        r.getIdReclamation(),
                        escapeCSV(r.getObjet()),
                        escapeCSV(r.getDescription()),
                        r.getDateCreation(),
                        r.getStatut(),
                        r.getPriorite(),
                        escapeCSV(r.getType()),
                        escapeCSV(r.getReponseAdmin() != null ? r.getReponseAdmin() : ""),
                        r.getDateReponse() != null ? r.getDateReponse().toString() : "",
                        r.getIdUtilisateur()
                ));
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la génération du CSV: " + e.getMessage());
        }
    }
    
    public static void exportUtilisateursToCSV(List<Utilisateur> utilisateurs, String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("ID,Nom,Email,Rôle,Statut,Téléphone,Adresse,Date Création\n");
            
            for (Utilisateur u : utilisateurs) {
                writer.write(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%s\n",
                        u.getId(),
                        escapeCSV(u.getNom()),
                        escapeCSV(u.getEmail()),
                        u.getRole(),
                        u.getStatut(),
                        escapeCSV(u.getTelephone() != null ? u.getTelephone() : ""),
                        escapeCSV(u.getAdresse() != null ? u.getAdresse() : ""),
                        u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""
                ));
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la génération du CSV: " + e.getMessage());
        }
    }
    
    private static String escapeCSV(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}

