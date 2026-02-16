package com.eventmanagement.services;

import com.eventmanagement.models.Evenement;
import com.eventmanagement.models.Reclamation;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PDFService {
    
    public static void exportEvenementsToPDF(List<Evenement> evenements, String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("=".repeat(80) + "\n");
            writer.write("              RAPPORT DES ÉVÉNEMENTS\n");
            writer.write("=".repeat(80) + "\n\n");
            writer.write("Date de génération: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "\n");
            writer.write("Nombre total d'événements: " + evenements.size() + "\n\n");
            writer.write("-".repeat(80) + "\n\n");
            
            for (Evenement e : evenements) {
                writer.write("ID: " + e.getIdEvenement() + "\n");
                writer.write("Titre: " + e.getTitre() + "\n");
                writer.write("Description: " + e.getDescription() + "\n");
                writer.write("Date début: " + e.getDateDebut() + "\n");
                writer.write("Date fin: " + e.getDateFin() + "\n");
                writer.write("Lieu: " + e.getLieu() + "\n");
                writer.write("Capacité max: " + e.getCapaciteMax() + "\n");
                writer.write("Organisateur ID: " + e.getOrganisateurId() + "\n");
                writer.write("\n" + "-".repeat(80) + "\n\n");
            }
            
            writer.write("\nFin du rapport\n");
            writer.write("=".repeat(80) + "\n");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la génération du PDF: " + e.getMessage());
        }
    }
    
    public static void exportReclamationsToPDF(List<Reclamation> reclamations, String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("=".repeat(80) + "\n");
            writer.write("              RAPPORT DES RÉCLAMATIONS\n");
            writer.write("=".repeat(80) + "\n\n");
            writer.write("Date de génération: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "\n");
            writer.write("Nombre total de réclamations: " + reclamations.size() + "\n\n");
            writer.write("-".repeat(80) + "\n\n");
            
            for (Reclamation r : reclamations) {
                writer.write("ID: " + r.getIdReclamation() + "\n");
                writer.write("Objet: " + r.getObjet() + "\n");
                writer.write("Description: " + r.getDescription() + "\n");
                writer.write("Date création: " + r.getDateCreation() + "\n");
                writer.write("Statut: " + r.getStatut() + "\n");
                writer.write("Priorité: " + r.getPriorite() + "\n");
                writer.write("Type: " + r.getType() + "\n");
                writer.write("Utilisateur ID: " + r.getIdUtilisateur() + "\n");
                writer.write("\n" + "-".repeat(80) + "\n\n");
            }
            
            writer.write("\nFin du rapport\n");
            writer.write("=".repeat(80) + "\n");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la génération du PDF: " + e.getMessage());
        }
    }
}
