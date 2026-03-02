package controller;

import entities.Commande;
import entities.Produit;
import service.CommandeService;
import service.ProduitService;
import javafx.beans.property.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class HistoriqueCommandesController {

    @FXML private Accordion accordion;
    @FXML private TitledPane paneEnCours, paneValidee, paneExpediee, paneLivree, paneAnnulee;
    @FXML private VBox listEnCours, listValidee, listExpediee, listLivree, listAnnulee;
    @FXML private Label lblStatus, lblTotal, lblEnCours, lblLivrees;
    @FXML private Label badgeEnCours, badgeValidee, badgeExpediee, badgeLivree, badgeAnnulee;

    private final CommandeService commandeService = new CommandeService();
    private final ProduitService produitService = new ProduitService();

    @FXML
    public void initialize() {
        actualiser();
    }

    @FXML
    private void actualiser() {
        // Vider toutes les listes
        listEnCours.getChildren().clear();
        listValidee.getChildren().clear();
        listExpediee.getChildren().clear();
        listLivree.getChildren().clear();
        listAnnulee.getChildren().clear();

        List<Commande> dernieres = commandeService.getDernieresCommandes(20);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        int total = 0, enCours = 0, livrees = 0;

        for (Commande c : dernieres) {
            String produitNom = "Inconnu";
            Produit p = produitService.getById(c.getIdProduit());
            if (p != null) produitNom = p.getNom();

            HBox ligne = createLigne(
                    c.getDateCommande().format(fmt),
                    produitNom,
                    c.getQuantiteCommandee(),
                    c.getStatut()
            );

            getListForStatut(c.getStatut()).getChildren().add(ligne);
            total++;
            if ("EN_COURS".equals(c.getStatut())) enCours++;
            if ("LIVREE".equals(c.getStatut())) livrees++;
        }

        // Badges
        long bEC = dernieres.stream().filter(c -> "EN_COURS".equals(c.getStatut())).count();
        long bV  = dernieres.stream().filter(c -> "VALIDEE".equals(c.getStatut())).count();
        long bEx = dernieres.stream().filter(c -> "EXPEDIEE".equals(c.getStatut())).count();
        long bL  = dernieres.stream().filter(c -> "LIVREE".equals(c.getStatut())).count();
        long bA  = dernieres.stream().filter(c -> "ANNULEE".equals(c.getStatut())).count();

        // Remplace les lignes badgeXxx par ceci dans actualiser() :
        if (paneEnCours  != null) paneEnCours.setText("⏳  EN COURS  — " + bEC + " commande(s)");
        if (paneValidee  != null) paneValidee.setText("✅  VALIDÉE  — " + bV  + " commande(s)");
        if (paneExpediee != null) paneExpediee.setText("🚚  EXPÉDIÉE  — " + bEx + " commande(s)");
        if (paneLivree   != null) paneLivree.setText("📦  LIVRÉE  — " + bL  + " commande(s)");
        if (paneAnnulee  != null) paneAnnulee.setText("❌  ANNULÉE  — " + bA  + " commande(s)");

// Supprime aussi les @FXML badgeXxx du controller car ils n'existent plus dans le FXML
        // Stats header
        if (lblTotal   != null) lblTotal.setText(String.valueOf(total));
        if (lblEnCours != null) lblEnCours.setText(String.valueOf(enCours));
        if (lblLivrees != null) lblLivrees.setText(String.valueOf(livrees));
        if (lblStatus  != null) lblStatus.setText(total + " commande(s) chargée(s)");

        // Ouvrir le panneau qui a le plus de commandes
        ouvrirPanneauPrincipal(bEC, bV, bEx, bL, bA);
    }

    private HBox createLigne(String date, String produit, int quantite, String statut) {
        String couleur = getStatutColor(statut);

        HBox ligne = new HBox(0);
        ligne.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        ligne.setStyle(
                "-fx-background-color: #f9fdf5;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-radius: 8;" +
                        "-fx-border-color: #e0ead8;" +
                        "-fx-border-width: 1;" +
                        "-fx-cursor: hand;"
        );

        // Barre colorée gauche
        VBox colorBar = new VBox();
        colorBar.setPrefWidth(5);
        colorBar.setPrefHeight(55);
        colorBar.setStyle("-fx-background-color: " + couleur + "; -fx-background-radius: 8 0 0 8;");

        // Contenu
        HBox content = new HBox(20);
        content.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        content.setStyle("-fx-padding: 12 18;");
        HBox.setHgrow(content, Priority.ALWAYS);

        // Icône statut
        Label icon = new Label(getStatutEmoji(statut));
        icon.setStyle("-fx-font-size: 20px;");

        // Produit (principal)
        VBox infoProduit = new VBox(3);
        HBox.setHgrow(infoProduit, Priority.ALWAYS);
        Label lblProduit = new Label(produit);
        lblProduit.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2d2d2d;");
        Label lblDate = new Label("📅 " + date);
        lblDate.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
        infoProduit.getChildren().addAll(lblProduit, lblDate);

        // Quantité
        VBox infoQte = new VBox(3);
        infoQte.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Label lblQteTitle = new Label("Quantité");
        lblQteTitle.setStyle("-fx-font-size: 9px; -fx-text-fill: #aaa; -fx-letter-spacing: 1px;");
        Label lblQte = new Label(String.valueOf(quantite));
        lblQte.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #4a7c3a;");
        infoQte.getChildren().addAll(lblQteTitle, lblQte);

        // Badge statut
        Label badge = new Label(statut.replace("_", " "));
        badge.setStyle(
                "-fx-background-color: " + couleur + "22;" +
                        "-fx-text-fill: " + couleur + ";" +
                        "-fx-font-size: 10px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 4 10;" +
                        "-fx-background-radius: 8;" +
                        "-fx-letter-spacing: 0.5px;"
        );

        content.getChildren().addAll(icon, infoProduit, infoQte, badge);
        ligne.getChildren().addAll(colorBar, content);

        // Hover
        ligne.setOnMouseEntered(e -> ligne.setStyle(
                "-fx-background-color: #eef5e8;" +
                        "-fx-background-radius: 8; -fx-border-radius: 8;" +
                        "-fx-border-color: " + couleur + ";" +
                        "-fx-border-width: 1; -fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);"
        ));
        ligne.setOnMouseExited(e -> ligne.setStyle(
                "-fx-background-color: #f9fdf5;" +
                        "-fx-background-radius: 8; -fx-border-radius: 8;" +
                        "-fx-border-color: #e0ead8; -fx-border-width: 1; -fx-cursor: hand;"
        ));

        return ligne;
    }

    private void ouvrirPanneauPrincipal(long bEC, long bV, long bEx, long bL, long bA) {
        long max = Math.max(bEC, Math.max(bV, Math.max(bEx, Math.max(bL, bA))));
        if (max == 0) { accordion.setExpandedPane(paneEnCours); return; }
        if (bEC == max)      accordion.setExpandedPane(paneEnCours);
        else if (bV == max)  accordion.setExpandedPane(paneValidee);
        else if (bEx == max) accordion.setExpandedPane(paneExpediee);
        else if (bL == max)  accordion.setExpandedPane(paneLivree);
        else                 accordion.setExpandedPane(paneAnnulee);
    }

    private VBox getListForStatut(String statut) {
        if (statut == null) return listEnCours;
        return switch (statut) {
            case "EN_COURS"  -> listEnCours;
            case "VALIDEE"   -> listValidee;
            case "EXPEDIEE"  -> listExpediee;
            case "LIVREE"    -> listLivree;
            case "ANNULEE"   -> listAnnulee;
            case "PAYEE"     -> listValidee;
            default          -> listEnCours;
        };
    }

    private String getStatutColor(String statut) {
        if (statut == null) return "#5a8c4a";
        return switch (statut) {
            case "EN_COURS"  -> "#5bc0de";
            case "VALIDEE"   -> "#5a8c4a";
            case "EXPEDIEE"  -> "#7b68ee";
            case "LIVREE"    -> "#f0ad4e";
            case "ANNULEE"   -> "#d9534f";
            default          -> "#5a8c4a";
        };
    }

    private String getStatutEmoji(String statut) {
        if (statut == null) return "📋";
        return switch (statut) {
            case "EN_COURS"  -> "⏳";
            case "VALIDEE"   -> "✅";
            case "EXPEDIEE"  -> "🚚";
            case "LIVREE"    -> "📦";
            case "ANNULEE"   -> "❌";
            default          -> "📋";
        };
    }

    @FXML
    private void retourAccueil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/home.fxml"));
            Parent root = loader.load();
            Stage homeStage = new Stage();
            homeStage.setScene(new Scene(root, 1920, 1080));
            homeStage.setTitle("Accueil - AgriConnect");
            homeStage.setMaximized(true);
            Stage currentStage = (Stage) accordion.getScene().getWindow();
            currentStage.close();
            homeStage.show();
        } catch (IOException e) {
            showErrorAlert("Erreur de navigation", e.getMessage());
        }
    }

    private void showInfoAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Information"); a.setHeaderText(title); a.setContentText(content); a.showAndWait();
    }

    private void showErrorAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur"); a.setHeaderText(title); a.setContentText(content); a.showAndWait();
    }

    public static class CommandeMini {
        public final IntegerProperty id = new SimpleIntegerProperty();
        public final StringProperty date = new SimpleStringProperty();
        public final StringProperty produit = new SimpleStringProperty();
        public final IntegerProperty quantite = new SimpleIntegerProperty();
        public final StringProperty statut = new SimpleStringProperty();

        public CommandeMini(int id, String date, String produit, int qte, String statut) {
            this.id.set(id); this.date.set(date); this.produit.set(produit);
            this.quantite.set(qte); this.statut.set(statut);
        }
    }
}