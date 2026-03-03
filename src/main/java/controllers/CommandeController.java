package controllers;

import entities.Commande;
import entities.Produit;
import services.BrevoEmailService;
import services.CommandeService;
import services.ProduitService;
import services.StockService;
import services.StripePayment;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CommandeController {

    @FXML private Label lblStatus, lblTotal, lblEnCours, lblAnnulees;
    @FXML private Label badgeEnCours, badgeValidee, badgeExpediee, badgeLivree, badgeAnnulee;
    @FXML private VBox colEnCours, colValidee, colExpediee, colLivree, colAnnulee;
    @FXML private ComboBox<Produit> cbProduit;
    @FXML private TextField tfQuantite;
    @FXML private ComboBox<String> cbStatut;

    private final CommandeService commandeService = new CommandeService();
    private final ProduitService produitService = new ProduitService();
    private final StockService stockService = new StockService();
    private CommandeData currentSelection = null;
    private VBox selectedCard = null;
    private final List<CommandeData> allCommandes = new ArrayList<>();

    public static class CommandeData {
        public final IntegerProperty id = new SimpleIntegerProperty();
        public final StringProperty date = new SimpleStringProperty();
        public final StringProperty statut = new SimpleStringProperty();
        public final IntegerProperty quantite = new SimpleIntegerProperty();
        public final StringProperty produitNom = new SimpleStringProperty();
        public final StringProperty client = new SimpleStringProperty();

        public CommandeData(int id, String date, String statut, int quantite,
                            String produitNom, String client) {
            this.id.set(id); this.date.set(date); this.statut.set(statut);
            this.quantite.set(quantite); this.produitNom.set(produitNom);
            this.client.set(client);
        }
    }

    @FXML
    public void initialize() {
        cbStatut.setItems(FXCollections.observableArrayList(
                "EN_COURS", "VALIDEE", "EXPEDIEE", "LIVREE", "ANNULEE", "PAYEE"
        ));
        List<Produit> produits = produitService.getAllProduits();
        cbProduit.setItems(FXCollections.observableArrayList(produits));
        cbProduit.setCellFactory(p -> new ListCell<>() {
            @Override protected void updateItem(Produit item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNom());
            }
        });
        cbProduit.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Produit item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Sélectionnez un produit" : item.getNom());
            }
        });
        loadCommandes();
    }

    // ═══════════════════════════════════════════════════════
    // KANBAN CARDS
    // ═══════════════════════════════════════════════════════

    private VBox createKanbanCard(CommandeData c) {
        String couleur = getStatutColor(c.statut.get());

        VBox card = new VBox(8);
        card.setStyle(
                "-fx-background-color: #f9fdf5;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-color: " + couleur + "55;" +
                        "-fx-border-width: 1;" +
                        "-fx-padding: 12 14;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2);"
        );

        // Header : ID + date
        HBox topRow = new HBox();
        topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label lblId = new Label("#" + c.id.get());
        lblId.setStyle("-fx-text-fill: " + couleur + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        String dateStr = c.date.get();
        Label lblDate = new Label(dateStr.length() > 10 ? dateStr.substring(0, 10) : dateStr);
        lblDate.setStyle("-fx-text-fill: #999; -fx-font-size: 9px;");
        topRow.getChildren().addAll(lblId, sp, lblDate);

        // Nom produit
        Label lblProduit = new Label(c.produitNom.get());
        lblProduit.setStyle("-fx-text-fill: #2d2d2d; -fx-font-size: 13px; -fx-font-weight: bold; -fx-wrap-text: true;");
        lblProduit.setMaxWidth(210);
        lblProduit.setWrapText(true);

        // Quantité
        HBox qteRow = new HBox(5);
        qteRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label qteIcon = new Label("📦");
        qteIcon.setStyle("-fx-font-size: 10px;");
        Label qteVal = new Label(c.quantite.get() + " unités");
        qteVal.setStyle("-fx-text-fill: #4a7c3a; -fx-font-size: 11px; -fx-font-weight: bold;");
        qteRow.getChildren().addAll(qteIcon, qteVal);

        // Badge statut
        Label badge = new Label(getStatutEmoji(c.statut.get()) + " " + c.statut.get());
        badge.setStyle(
                "-fx-background-color: " + couleur + "22;" +
                        "-fx-text-fill: " + couleur + ";" +
                        "-fx-font-size: 9px; -fx-font-weight: bold;" +
                        "-fx-padding: 3 8; -fx-background-radius: 8;"
        );

        card.getChildren().addAll(topRow, lblProduit, qteRow, badge);

        // Hover
        card.setOnMouseEntered(e -> {
            if (currentSelection == null || currentSelection.id.get() != c.id.get()) {
                card.setStyle(
                        "-fx-background-color: #eef5e8;" +
                                "-fx-background-radius: 10; -fx-border-radius: 10;" +
                                "-fx-border-color: " + couleur + "; -fx-border-width: 1.5;" +
                                "-fx-padding: 12 14; -fx-cursor: hand;" +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 10, 0, 0, 3);"
                );
            }
        });
        card.setOnMouseExited(e -> {
            if (currentSelection == null || currentSelection.id.get() != c.id.get()) {
                card.setStyle(
                        "-fx-background-color: #f9fdf5;" +
                                "-fx-background-radius: 10; -fx-border-radius: 10;" +
                                "-fx-border-color: " + couleur + "55; -fx-border-width: 1;" +
                                "-fx-padding: 12 14; -fx-cursor: hand;" +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2);"
                );
            }
        });

        card.setOnMouseClicked(e -> selectKanbanCard(c, card, couleur));
        return card;
    }

    private void selectKanbanCard(CommandeData c, VBox card, String couleur) {
        if (selectedCard != null && currentSelection != null) {
            String oldColor = getStatutColor(currentSelection.statut.get());
            selectedCard.setStyle(
                    "-fx-background-color: #f9fdf5;" +
                            "-fx-background-radius: 10; -fx-border-radius: 10;" +
                            "-fx-border-color: " + oldColor + "55; -fx-border-width: 1;" +
                            "-fx-padding: 12 14; -fx-cursor: hand;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2);"
            );
        }
        card.setStyle(
                "-fx-background-color: #e8f5e9;" +
                        "-fx-background-radius: 10; -fx-border-radius: 10;" +
                        "-fx-border-color: #4a7c3a; -fx-border-width: 2.5;" +
                        "-fx-padding: 12 14; -fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(74,124,58,0.3), 12, 0, 0, 4);"
        );
        selectedCard = card;
        currentSelection = c;
        cbProduit.getItems().stream()
                .filter(p -> p.getNom().equals(c.produitNom.get()))
                .findFirst().ifPresent(cbProduit::setValue);
        tfQuantite.setText(String.valueOf(c.quantite.get()));
        cbStatut.setValue(c.statut.get());
    }

    private String getStatutColor(String statut) {
        if (statut == null) return "#5a8c4a";
        return switch (statut) {
            case "EN_COURS"             -> "#5bc0de";
            case "VALIDEE"              -> "#5a8c4a";
            case "EXPEDIEE"             -> "#7b68ee";
            case "LIVREE"               -> "#f0ad4e";
            case "ANNULEE"              -> "#d9534f";
            case "PAYEE"                -> "#4caf50";
            case "EN_ATTENTE_PAIEMENT"  -> "#ff9800";
            default                     -> "#5a8c4a";
        };
    }

    private String getStatutEmoji(String statut) {
        if (statut == null) return "📋";
        return switch (statut) {
            case "EN_COURS"             -> "⏳";
            case "VALIDEE"              -> "✅";
            case "EXPEDIEE"             -> "🚚";
            case "LIVREE"               -> "📦";
            case "ANNULEE"              -> "❌";
            case "PAYEE"                -> "💳";
            case "EN_ATTENTE_PAIEMENT"  -> "⌛";
            default                     -> "📋";
        };
    }

    private VBox getColForStatut(String statut) {
        if (statut == null) return colEnCours;
        return switch (statut) {
            case "EN_COURS"             -> colEnCours;
            case "VALIDEE"              -> colValidee;
            case "EXPEDIEE"             -> colExpediee;
            case "LIVREE"               -> colLivree;
            case "ANNULEE"              -> colAnnulee;
            case "PAYEE"                -> colValidee;
            case "EN_ATTENTE_PAIEMENT"  -> colEnCours;
            default                     -> colEnCours;
        };
    }

    // ═══════════════════════════════════════════════════════
    // NAVIGATION
    // ═══════════════════════════════════════════════════════

    @FXML
    private void retourAccueil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/home.fxml"));
            Parent root = loader.load();
            Stage homeStage = new Stage();
            homeStage.setScene(new Scene(root, 1400, 850));
            homeStage.setTitle("Accueil - AgriConnect");
            Stage currentStage = (Stage) tfQuantite.getScene().getWindow();
            currentStage.close();
            homeStage.show();
        } catch (IOException e) {
            showErrorAlert("Erreur de navigation", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════
    // CRUD
    // ═══════════════════════════════════════════════════════

    @FXML
    private void handleAjouter() {
        if (!verifierSaisie()) return;
        try {
            int qte = Integer.parseInt(tfQuantite.getText().trim());
            Produit p = cbProduit.getValue();
            Commande c = new Commande();
            c.setQuantiteCommandee(qte);
            c.setIdProduit(p.getIdProduit());
            c.setStatut(cbStatut.getValue() != null ? cbStatut.getValue() : "EN_COURS");
            commandeService.ajouter(c);

            // ✅ Email confirmation
            BrevoEmailService.envoyerConfirmationCommande(
                    "ton_email@gmail.com",
                    "Client AgriConnect",
                    p.getNom(),
                    qte,
                    p.getPrix()
            );

            showSuccessAlert("Commande ajoutée", "Commande enregistrée avec succès.");
            clearInputs();
            loadCommandes();
        } catch (Exception e) {
            showErrorAlert("Erreur ajout", e.getMessage());
        }
    }

    @FXML
    private void handleModifier() {
        if (currentSelection == null) {
            showWarningAlert("Aucune sélection", "Cliquez sur une card d'abord.");
            return;
        }
        if (cbStatut.getValue() == null) {
            showErrorAlert("Statut requis", "Veuillez sélectionner un statut.");
            return;
        }
        try {
            commandeService.modifierStatut(currentSelection.id.get(), cbStatut.getValue());
            showSuccessAlert("Modifiée", "Statut mis à jour.");
            clearInputs();
            loadCommandes();
        } catch (Exception e) {
            showErrorAlert("Erreur modification", e.getMessage());
        }
    }

    @FXML
    private void handleSupprimer() {
        if (currentSelection == null) {
            showWarningAlert("Aucune sélection", "Cliquez sur une card d'abord.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer la commande #" + currentSelection.id.get() + " ?");
        confirm.setContentText("Produit : " + currentSelection.produitNom.get() +
                "\nCette action est irréversible.");
        ButtonType oui = new ButtonType("Oui, supprimer", ButtonBar.ButtonData.YES);
        ButtonType non = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(oui, non);
        if (confirm.showAndWait().orElse(non) == oui) {
            try {
                commandeService.supprimer(currentSelection.id.get());
                showSuccessAlert("Supprimée", "Commande supprimée.");
                clearInputs();
                loadCommandes();
            } catch (Exception e) {
                showErrorAlert("Erreur suppression", e.getMessage());
            }
        }
    }

    @FXML
    private void handleActualiser() {
        loadCommandes();
    }

    @FXML
    private void handlePayerStripe() {
        if (!verifierSaisie()) return;
        try {
            int qte = Integer.parseInt(tfQuantite.getText().trim());
            Produit p = cbProduit.getValue();
            long montantCentimes = Math.round(p.getPrix() * qte * 100);
            String checkoutUrl = StripePayment.createCheckoutSession(
                    montantCentimes, p.getNom() + " × " + qte,
                    "http://localhost/success.html",
                    "http://localhost/cancel.html");
            Desktop.getDesktop().browse(new URI(checkoutUrl));

            // ✅ Statut PAYEE
            Commande c = new Commande();
            c.setQuantiteCommandee(qte);
            c.setIdProduit(p.getIdProduit());
            c.setStatut("PAYEE");
            commandeService.ajouter(c);

            // ✅ Email confirmation paiement
            BrevoEmailService.envoyerConfirmationCommande(
                    "bahaeddine.cherif@isimg.tn",
                    "Client AgriConnect",
                    p.getNom(),
                    qte,
                    p.getPrix()
            );

            showSuccessAlert("Paiement lancé !", "Commande enregistrée comme PAYÉE ✅");
            clearInputs();
            loadCommandes();
        } catch (Exception e) {
            showErrorAlert("Erreur Stripe", e.getMessage());
        }
    }

    @FXML
    private void exporterCSV() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fc.setInitialFileName("commandes_" + LocalDate.now() + ".csv");
        File file = fc.showSaveDialog(colEnCours.getScene().getWindow());
        if (file != null) {
            try (FileWriter w = new FileWriter(file)) {
                w.append("Date,Statut,Quantité,Produit\n");
                for (CommandeData c : allCommandes)
                    w.append(String.format("\"%s\",\"%s\",%d,\"%s\"\n",
                            c.date.get(), c.statut.get(),
                            c.quantite.get(), c.produitNom.get()));
                showSuccessAlert("Export réussi", file.getAbsolutePath());
            } catch (IOException e) {
                showErrorAlert("Erreur export", e.getMessage());
            }
        }
    }



    private boolean verifierSaisie() {
        Produit produit = cbProduit.getValue();
        if (produit == null) {
            showErrorAlert("Produit requis", "Veuillez sélectionner un produit.");
            return false;
        }
        String qteTexte = tfQuantite.getText().trim();
        if (qteTexte.isEmpty()) {
            showErrorAlert("Quantité requise", "Veuillez entrer une quantité.");
            return false;
        }
        int quantite;
        try { quantite = Integer.parseInt(qteTexte); }
        catch (NumberFormatException e) {
            showErrorAlert("Format invalide", "Quantité doit être un entier.");
            return false;
        }
        if (quantite <= 0) {
            showErrorAlert("Quantité invalide", "Quantité doit être > 0.");
            return false;
        }
        int stock = stockService.getQuantiteDisponible((int) produit.getIdProduit());
        if (quantite > stock) {
            showErrorAlert("Stock insuffisant",
                    "Disponible : " + stock + " | Demandé : " + quantite);
            return false;
        }
        return true;
    }

    // ═══════════════════════════════════════════════════════
    // CHARGEMENT
    // ═══════════════════════════════════════════════════════

    private void loadCommandes() {
        colEnCours.getChildren().clear();
        colValidee.getChildren().clear();
        colExpediee.getChildren().clear();
        colLivree.getChildren().clear();
        colAnnulee.getChildren().clear();
        allCommandes.clear();

        List<Commande> commandes = commandeService.getAllCommandes();
        for (Commande c : commandes) {
            String produitNom = "Produit inconnu";
            Produit p = produitService.getById(c.getIdProduit());
            if (p != null) produitNom = p.getNom();
            CommandeData data = new CommandeData(
                    c.getIdCommande(),
                    c.getDateCommande().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    c.getStatut(), c.getQuantiteCommandee(), produitNom, "—"
            );
            allCommandes.add(data);
            getColForStatut(c.getStatut()).getChildren().add(createKanbanCard(data));
        }

        // Badges
        long bEC = allCommandes.stream().filter(d -> "EN_COURS".equals(d.statut.get()) || "EN_ATTENTE_PAIEMENT".equals(d.statut.get())).count();
        long bV  = allCommandes.stream().filter(d -> "VALIDEE".equals(d.statut.get()) || "PAYEE".equals(d.statut.get())).count();
        long bEx = allCommandes.stream().filter(d -> "EXPEDIEE".equals(d.statut.get())).count();
        long bL  = allCommandes.stream().filter(d -> "LIVREE".equals(d.statut.get())).count();
        long bA  = allCommandes.stream().filter(d -> "ANNULEE".equals(d.statut.get())).count();

        if (badgeEnCours  != null) badgeEnCours.setText(String.valueOf(bEC));
        if (badgeValidee  != null) badgeValidee.setText(String.valueOf(bV));
        if (badgeExpediee != null) badgeExpediee.setText(String.valueOf(bEx));
        if (badgeLivree   != null) badgeLivree.setText(String.valueOf(bL));
        if (badgeAnnulee  != null) badgeAnnulee.setText(String.valueOf(bA));

        long totalEC = allCommandes.stream().filter(d -> "EN_COURS".equals(d.statut.get())).count();
        long totalA  = allCommandes.stream().filter(d -> "ANNULEE".equals(d.statut.get())).count();

        if (lblTotal    != null) lblTotal.setText(String.valueOf(allCommandes.size()));
        if (lblEnCours  != null) lblEnCours.setText(String.valueOf(totalEC));
        if (lblAnnulees != null) lblAnnulees.setText(String.valueOf(totalA));
        if (lblStatus   != null) lblStatus.setText("Chargé : " + allCommandes.size() + " commande(s)");
    }



    private void clearInputs() {
        tfQuantite.clear();
        cbProduit.getSelectionModel().clearSelection();
        cbStatut.getSelectionModel().clearSelection();
        currentSelection = null;
        selectedCard = null;
    }

    private void showSuccessAlert(String h, String c) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Succès"); a.setHeaderText(h); a.setContentText(c); a.showAndWait();
    }

    private void showErrorAlert(String h, String c) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur"); a.setHeaderText(h); a.setContentText(c); a.showAndWait();
    }

    private void showWarningAlert(String h, String c) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Attention"); a.setHeaderText(h); a.setContentText(c); a.showAndWait();
    }

    private void showInfoAlert(String h, String c) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Info"); a.setHeaderText(h); a.setContentText(c); a.showAndWait();
    }
}