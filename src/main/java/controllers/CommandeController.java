package controllers;

import entities.Commande;
import entities.Produit;
import services.CommandeService;
import services.ProduitService;
import services.StockService;
import services.StripePayment;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CommandeController {

    @FXML private Label lblStatus;
    @FXML private ComboBox<Produit> cbProduit;
    @FXML private TextField tfQuantite;
    @FXML private ComboBox<String> cbStatut;
    @FXML private Button btnAjouter, btnModifier, btnSupprimer, btnActualiser;
    @FXML private TableView<CommandeData> tableCommande;
    @FXML private TableColumn<CommandeData, Integer> colId, colQuantite;
    @FXML private TableColumn<CommandeData, String> colDate, colStatut, colProduit, colClient;

    private final CommandeService commandeService = new CommandeService();
    private final ProduitService produitService = new ProduitService();
    private final StockService stockService = new StockService();
    private CommandeData currentSelection = null;

    public static class CommandeData {
        public final IntegerProperty id = new SimpleIntegerProperty();
        public final StringProperty date = new SimpleStringProperty();
        public final StringProperty statut = new SimpleStringProperty();
        public final IntegerProperty quantite = new SimpleIntegerProperty();
        public final StringProperty produitNom = new SimpleStringProperty();
        public final StringProperty client = new SimpleStringProperty();

        public CommandeData(int id, String date, String statut, int quantite,
                            String produitNom, String client) {
            this.id.set(id);
            this.date.set(date);
            this.statut.set(statut);
            this.quantite.set(quantite);
            this.produitNom.set(produitNom);
            this.client.set(client);
        }
    }

    @FXML
    public void initialize() {
        cbStatut.setItems(FXCollections.observableArrayList(
                "EN_COURS", "VALIDEE", "EXPEDIEE", "LIVREE", "ANNULEE"
        ));

        List<Produit> produits = produitService.getAllProduits();
        cbProduit.setItems(FXCollections.observableArrayList(produits));
        cbProduit.setCellFactory(param -> new ListCell<Produit>() {
            @Override
            protected void updateItem(Produit item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNom());
            }
        });
        cbProduit.setButtonCell(new ListCell<Produit>() {
            @Override
            protected void updateItem(Produit item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Sélectionnez un produit" : item.getNom());
            }
        });

        colId.setCellValueFactory(cd -> cd.getValue().id.asObject());
        colDate.setCellValueFactory(cd -> cd.getValue().date);
        colStatut.setCellValueFactory(cd -> cd.getValue().statut);
        colQuantite.setCellValueFactory(cd -> cd.getValue().quantite.asObject());
        colProduit.setCellValueFactory(cd -> cd.getValue().produitNom);
        colClient.setCellValueFactory(cd -> cd.getValue().client);

        tableCommande.getSelectionModel().selectedItemProperty().addListener((obs, old, newSel) -> {
            currentSelection = newSel;
            if (newSel != null) {
                tfQuantite.setText(String.valueOf(newSel.quantite.get()));
                cbStatut.setValue(newSel.statut.get());
                cbProduit.getSelectionModel().select(
                        cbProduit.getItems().stream()
                                .filter(p -> p.getNom().equals(newSel.produitNom.get()))
                                .findFirst().orElse(null)
                );
            } else {
                clearInputs();
            }
        });

        loadCommandes();
    }

    // ✅ NOUVELLE MÉTHODE : Retour à l'accueil
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

            System.out.println("🏠 Retour à l'accueil depuis Commandes");

        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Erreur de navigation", "Impossible de retourner à l'accueil : " + e.getMessage());
        }
    }

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
            showWarningAlert("Aucune sélection", "Veuillez sélectionner une commande.");
            return;
        }

        if (!verifierSaisie()) return;

        try {
            int qte = Integer.parseInt(tfQuantite.getText().trim());
            String statut = cbStatut.getValue();

            commandeService.modifierStatut(currentSelection.id.get(), statut);
            currentSelection.quantite.set(qte);
            currentSelection.statut.set(statut);

            showSuccessAlert("Commande modifiée", "La commande a été mise à jour avec succès.");
        } catch (Exception e) {
            showErrorAlert("Erreur modification", e.getMessage());
        }
    }

    @FXML
    private void handleSupprimer() {
        if (currentSelection == null) {
            showWarningAlert("Aucune sélection", "Veuillez sélectionner une commande à supprimer.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer la commande #" + currentSelection.id.get() + " ?");
        confirm.setContentText(
                "Produit : " + currentSelection.produitNom.get() + "\n" +
                        "Quantité : " + currentSelection.quantite.get() + "\n" +
                        "Cette action est irréversible."
        );

        DialogPane pane = confirm.getDialogPane();
        pane.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        pane.getStyleClass().add("delete-confirmation-alert");

        ButtonType oui = new ButtonType("Oui, supprimer", ButtonBar.ButtonData.YES);
        ButtonType non = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(oui, non);

        if (confirm.showAndWait().orElse(non) == oui) {
            try {
                commandeService.supprimer(currentSelection.id.get());
                showSuccessAlert("Supprimée", "La commande a été supprimée avec succès.");
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
        showInfoAlert(
                "Liste actualisée",
                "La liste des commandes a été rafraîchie.\n" +
                        tableCommande.getItems().size() + " commande(s) affichée(s)."
        );
    }

    @FXML
    private void handlePayerStripe() {
        if (!verifierSaisie()) return;

        try {
            int qte = Integer.parseInt(tfQuantite.getText().trim());
            Produit p = cbProduit.getValue();
            long montantCentimes = Math.round(p.getPrix() * qte * 100);

            String successUrl = "http://localhost/success.html";
            String cancelUrl = "http://localhost/cancel.html";

            String checkoutUrl = StripePayment.createCheckoutSession(
                    montantCentimes,
                    p.getNom() + " × " + qte,
                    successUrl,
                    cancelUrl
            );

            Desktop.getDesktop().browse(new URI(checkoutUrl));

            showInfoAlert("Paiement démarré",
                    "Veuillez compléter le paiement dans la fenêtre du navigateur qui s'est ouverte.\n" +
                            "Une fois terminé, revenez ici pour confirmer la commande.");

            Commande c = new Commande();
            c.setQuantiteCommandee(qte);
            c.setIdProduit(p.getIdProduit());
            c.setStatut("EN_ATTENTE_PAIEMENT");
            commandeService.ajouter(c);

            clearInputs();
            loadCommandes();

        } catch (Exception e) {
            showErrorAlert("Erreur paiement Stripe", e.getMessage());
        }
    }

    @FXML
    private void exporterCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter les commandes en CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers CSV (*.csv)", "*.csv"));
        fileChooser.setInitialFileName("commandes_" + LocalDate.now() + ".csv");

        File file = fileChooser.showSaveDialog(tableCommande.getScene().getWindow());

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.append("ID,Date,Statut,Quantité,Produit,Client\n");
                for (CommandeData c : tableCommande.getItems()) {
                    writer.append(String.format("%d,\"%s\",\"%s\",%d,\"%s\",\"%s\"\n",
                            c.id.get(),
                            escapeCsv(c.date.get()),
                            escapeCsv(c.statut.get()),
                            c.quantite.get(),
                            escapeCsv(c.produitNom.get()),
                            escapeCsv(c.client.get())));
                }
                showSuccessAlert("Export réussi", "Le fichier CSV a été sauvegardé :\n" + file.getAbsolutePath());
            } catch (IOException e) {
                showErrorAlert("Erreur d'export", "Impossible de sauvegarder le fichier :\n" + e.getMessage());
            }
        }
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        s = s.replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\n") || s.contains("\"")) {
            return "\"" + s + "\"";
        }
        return s;
    }

    private boolean verifierSaisie() {
        Produit produit = cbProduit.getValue();
        String qteTexte = tfQuantite.getText().trim();

        if (produit == null) {
            showErrorAlert("Produit requis", "Veuillez sélectionner un produit.");
            return false;
        }

        if (qteTexte.isEmpty()) {
            showErrorAlert("Quantité requise", "Veuillez entrer une quantité.");
            return false;
        }

        int quantite;
        try {
            quantite = Integer.parseInt(qteTexte);
        } catch (NumberFormatException e) {
            showErrorAlert("Format invalide", "La quantité doit être un nombre entier.");
            return false;
        }

        if (quantite <= 0) {
            showErrorAlert("Quantité invalide", "La quantité doit être supérieure à 0.");
            return false;
        }

        int stockDisponible = stockService.getQuantiteDisponible((int) produit.getIdProduit());
        if (quantite > stockDisponible) {
            showErrorAlert(
                    "Stock insuffisant",
                    "Produit : " + produit.getNom() + "\nDisponible : " + stockDisponible + "\nDemandé : " + quantite
            );
            return false;
        }

        return true;
    }

    private void loadCommandes() {
        List<Commande> commandes = commandeService.getAllCommandes();
        ObservableList<CommandeData> data = FXCollections.observableArrayList();
        for (Commande c : commandes) {
            String produitNom = "Produit inconnu";
            Produit p = produitService.getById(c.getIdProduit());
            if (p != null) produitNom = p.getNom();
            String client = "—";
            data.add(new CommandeData(
                    c.getIdCommande(),
                    c.getDateCommande().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    c.getStatut(),
                    c.getQuantiteCommandee(),
                    produitNom,
                    client
            ));
        }
        tableCommande.setItems(data);
        lblStatus.setText("Chargement terminé : " + data.size() + " commande(s)");
    }

    private void clearInputs() {
        tfQuantite.clear();
        cbProduit.getSelectionModel().clearSelection();
        cbStatut.getSelectionModel().clearSelection();
        tableCommande.getSelectionModel().clearSelection();
        currentSelection = null;
    }

    private void showSuccessAlert(String header, String content) {
        showAlert(Alert.AlertType.INFORMATION, "Succès", header, content, "success-alert");
    }

    private void showErrorAlert(String header, String content) {
        showAlert(Alert.AlertType.ERROR, "Erreur", header, content, "error-alert");
    }

    private void showWarningAlert(String header, String content) {
        showAlert(Alert.AlertType.WARNING, "Attention", header, content, "warning-alert");
    }

    private void showInfoAlert(String header, String content) {
        showAlert(Alert.AlertType.INFORMATION, "Information", header, content, "info-alert");
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content, String styleClass) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        DialogPane pane = alert.getDialogPane();
        pane.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        pane.getStyleClass().add(styleClass);
        alert.showAndWait();
    }
}