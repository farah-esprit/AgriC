package service;

import entities.Evenement;
import entities.Reclamation;
import entities.User;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AssistantService {

    public String summarizeUserPendingEvents(List<Evenement> events) {
        List<Evenement> pending = events.stream()
            .filter(e -> "EN_ATTENTE".equals(e.getStatut()))
            .sorted(Comparator.comparing(Evenement::getDateDebut))
            .toList();
        if (pending.isEmpty()) {
            return "Tu n'as aucun evenement en attente.";
        }
        String lines = pending.stream()
            .limit(8)
            .map(e -> "- #" + e.getIdEvenement() + " " + safe(e.getTitre()) + " (" + e.getDateDebut() + ")")
            .collect(Collectors.joining("\n"));
        return "Evenements en attente (" + pending.size() + "):\n" + lines;
    }

    public String explainRejectedEvents(List<Evenement> events) {
        List<Evenement> rejected = events.stream()
            .filter(e -> "REJETE".equals(e.getStatut()))
            .sorted(Comparator.comparing(Evenement::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
        if (rejected.isEmpty()) {
            return "Aucun evenement rejete pour le moment.";
        }
        String lines = rejected.stream()
            .limit(8)
            .map(e -> "- #" + e.getIdEvenement() + " " + safe(e.getTitre()) + " | raison: " + safe(e.getRaisonRejet()))
            .collect(Collectors.joining("\n"));
        return "Raisons de rejet:\n" + lines;
    }

    public String adminSummary(List<Evenement> events, List<Reclamation> claims, List<User> users) {
        long approved = events.stream().filter(e -> "APPROUVE".equals(e.getStatut())).count();
        long pending = events.stream().filter(e -> "EN_ATTENTE".equals(e.getStatut())).count();
        long rejected = events.stream().filter(e -> "REJETE".equals(e.getStatut())).count();

        long openClaims = claims.stream().filter(c -> !"TRAITEE".equals(c.getStatut()) && !"CLOTUREE".equals(c.getStatut())).count();
        long resolvedClaims = claims.size() - openClaims;


        return "Resume admin:\n"
            + "- Evenements: " + events.size() + " (APPROUVE=" + approved + ", EN_ATTENTE=" + pending + ", REJETE=" + rejected + ")\n"
            + "- Reclamations: " + claims.size() + " (ouvertes=" + openClaims + ", traitees/cloturees=" + resolvedClaims + ")\n"
             ;
    }

    public String prioritizeClaims(List<Reclamation> claims) {
        if (claims.isEmpty()) {
            return "Aucune reclamation disponible.";
        }
        List<Reclamation> ordered = claims.stream()
            .sorted(Comparator.comparingInt(this::priorityScore).reversed())
            .limit(10)
            .toList();

        String lines = ordered.stream()
            .map(c -> "- #" + c.getIdReclamation()
                + " [" + label(priorityScore(c)) + "] "
                + safe(c.getObjet())
                + " | statut=" + safe(c.getStatut())
                + " | priorite=" + safe(c.getPriorite()))
            .collect(Collectors.joining("\n"));
        return "Priorisation reclamations (urgent -> normal):\n" + lines;
    }

    public String suggestAdminReply(Reclamation claim) {
        if (claim == null) {
            return "Selectionne une reclamation pour proposer une reponse.";
        }
        String type = safe(claim.getType()).toLowerCase();
        String opening = "Bonjour, nous avons bien recu votre reclamation.";
        String middle;
        if (type.contains("tech")) {
            middle = "Notre equipe technique analyse le dysfonctionnement et applique un correctif prioritaire.";
        } else if (type.contains("fin")) {
            middle = "Notre equipe finance verifie le dossier de paiement/remboursement et vous recontacte rapidement.";
        } else {
            middle = "Votre demande est en cours de traitement par le service concerne.";
        }
        String closing = "Nous vous remercions pour votre patience. Cordialement, l'equipe support.";
        return opening + "\n" + middle + "\n" + closing;
    }

    public String buildAdminReportText(List<Evenement> events, List<Reclamation> claims, List<User> users, Map<String, Integer> statsByStatus, Map<String, Integer> statsByType) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== RAPPORT ASSISTANT ADMIN ===\n");
        sb.append("Date: ").append(LocalDate.now()).append("\n\n");
        sb.append(adminSummary(events, claims, users)).append("\n\n");

        sb.append("Reclamations par statut:\n");
        statsByStatus.forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append("\n"));
        sb.append("\nReclamations par type:\n");
        statsByType.forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append("\n"));
        sb.append("\n");
        sb.append(prioritizeClaims(claims));
        return sb.toString();
    }

    private int priorityScore(Reclamation claim) {
        int score = 0;
        String priorite = safe(claim.getPriorite()).toUpperCase();
        if ("HAUTE".equals(priorite)) {
            score += 60;
        } else if ("MOYENNE".equals(priorite)) {
            score += 35;
        } else {
            score += 15;
        }

        String statut = safe(claim.getStatut()).toUpperCase();
        if ("EN_ATTENTE".equals(statut)) {
            score += 25;
        } else if ("EN_COURS".equals(statut)) {
            score += 15;
        }

        if (claim.getDateCreation() != null) {
            long days = ChronoUnit.DAYS.between(claim.getDateCreation(), LocalDate.now());
            score += (int) Math.min(20, Math.max(0, days));
        }

        String type = safe(claim.getType()).toLowerCase();
        if (type.contains("tech") || type.contains("fin")) {
            score += 10;
        }

        return score;
    }

    private String label(int score) {
        return score >= 75 ? "URGENT" : "NORMAL";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
