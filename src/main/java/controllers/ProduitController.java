package controllers;

import entities.Produit;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import services.ProduitService;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;


public class ProduitController {

    @FXML private TextField tfNom, tfDescription, tfPrix;
    @FXML private ComboBox<String> cbCategorie;
    @FXML private Button btnAjouter, btnModifier, btnSupprimer, btnActualiser;
    @FXML private TableView<ProduitData> tableProduit;
    @FXML private TableColumn<ProduitData, Long> colId;
    @FXML private TableColumn<ProduitData, String> colNom, colDescription, colCategorie;
    @FXML private TableColumn<ProduitData, Double> colPrix;
    @FXML private Label lblStatus;


    private ProduitService produitService = new ProduitService();
    private ProduitData currentSelection = null;

    public static class ProduitData {
        public final LongProperty id = new SimpleLongProperty();
        public final StringProperty nom = new SimpleStringProperty();
        public final StringProperty description = new SimpleStringProperty();
        public final DoubleProperty prix = new SimpleDoubleProperty();
        public final StringProperty categorie = new SimpleStringProperty();

        public ProduitData(long id, String nom, String description, double prix, String categorie) {
            this.id.set(id);
            this.nom.set(nom);
            this.description.set(description);
            this.prix.set(prix);
            this.categorie.set(categorie);
        }
    }

    @FXML
    public void initialize() {
        cbCategorie.setItems(FXCollections.observableArrayList(
                "Légumes", "Fruits", "Céréales", "Huiles", "Épices",
                "Produits laitiers", "Miel", "Viandes", "Autres"
        ));

        colId.setCellValueFactory(cd -> cd.getValue().id.asObject());
        colNom.setCellValueFactory(cd -> cd.getValue().nom);
        colDescription.setCellValueFactory(cd -> cd.getValue().description);
        colPrix.setCellValueFactory(cd -> cd.getValue().prix.asObject());
        colCategorie.setCellValueFactory(cd -> cd.getValue().categorie);

        tableProduit.getSelectionModel().selectedItemProperty().addListener((obs, old, newSel) -> {
            currentSelection = newSel;
            if (newSel != null) {
                tfNom.setText(newSel.nom.get());
                tfDescription.setText(newSel.description.get());
                tfPrix.setText(String.format(java.util.Locale.US, "%.2f", newSel.prix.get()));
                cbCategorie.setValue(newSel.categorie.get());
            } else {
                clearInputs();
            }
        });

        chargerProduits();
    }

    @FXML
    private void handleAjouter() {
        if (!validateInput()) return;
        try {
            Produit p = new Produit(
                    tfNom.getText().trim(),
                    tfDescription.getText().trim(),
                    Double.parseDouble(tfPrix.getText().trim()),
                    cbCategorie.getValue()
            );
            produitService.ajouter(p);
            showSuccessAlert("Produit ajouté avec succès !", "Le produit a bien été enregistré.");
            clearInputs();
            chargerProduits();
        } catch (Exception e) {
            showErrorAlert("Erreur lors de l'ajout", e.getMessage());
        }
    }

    @FXML
    private void handleModifier() {
        if (currentSelection == null) {
            showWarningAlert("Aucune sélection", "Veuillez sélectionner un produit dans le tableau.");
            return;
        }
        if (!validateInput()) return;
        try {
            currentSelection.nom.set(tfNom.getText().trim());
            currentSelection.description.set(tfDescription.getText().trim());
            currentSelection.prix.set(Double.parseDouble(tfPrix.getText().trim()));
            currentSelection.categorie.set(cbCategorie.getValue());
            Produit p = new Produit(
                    currentSelection.id.get(),
                    currentSelection.nom.get(),
                    currentSelection.description.get(),
                    currentSelection.prix.get(),
                    currentSelection.categorie.get(),
                    true
            );
            produitService.modifier(p);
            showSuccessAlert("Produit modifié avec succès !", "Les modifications ont été enregistrées.");
            chargerProduits();
        } catch (Exception e) {
            showErrorAlert("Erreur lors de la modification", e.getMessage());
        }
    }

    @FXML
    private void handleSupprimer() {
        if (currentSelection == null) {
            showWarningAlert("Aucune sélection", "Veuillez sélectionner un produit dans le tableau.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer ce produit ?");
        confirm.setContentText(
                "Produit : " + currentSelection.nom.get() + "\n" +
                        "Catégorie : " + currentSelection.categorie.get() + "\n" +
                        "Cette action est irréversible."
        );

        ButtonType btnOui = new ButtonType("Oui, supprimer", ButtonBar.ButtonData.YES);
        ButtonType btnNon = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnOui, btnNon);

        DialogPane dialogPane = confirm.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialogPane.getStyleClass().add("delete-confirmation-alert");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == btnOui) {
            try {
                produitService.supprimer(currentSelection.id.get());
                tableProduit.getItems().remove(currentSelection);
                showSuccessAlert("Supprimé avec succès", "Le produit a été supprimé de la base.");
                clearInputs();
                lblStatus.setText("Produit supprimé");
            } catch (Exception e) {
                showErrorAlert("Erreur suppression", e.getMessage());
            }
        }
    }

    @FXML
    private void handleActualiser() {
        clearInputs();
        chargerProduits();
        showInfoAlert(
                "Liste actualisée",
                "La liste des produits a été rafraîchie.\n" +
                        tableProduit.getItems().size() + " produit(s) trouvé(s)."
        );
        lblStatus.setText("Actualisé : " + tableProduit.getItems().size() + " produits affichés");
    }

    @FXML
    private void exporterCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter les produits en CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers CSV (.csv)", ".csv"));
        fileChooser.setInitialFileName("produits_" + LocalDate.now() + ".csv");

        File file = fileChooser.showSaveDialog(tableProduit.getScene().getWindow());

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.append("ID,Nom,Description,Prix (€),Catégorie,Actif\n");
                for (ProduitData p : tableProduit.getItems()) {
                    writer.append(String.format("%d,\"%s\",\"%s\",%.2f,\"%s\",%b\n",
                            p.id.get(),
                            escapeCsv(p.nom.get()),
                            escapeCsv(p.description.get()),
                            p.prix.get(),
                            escapeCsv(p.categorie.get()),
                            true));
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

    // ─────────────── Contrôle de saisie amélioré ───────────────

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        // Nom
        String nom = tfNom.getText().trim();
        if (nom.isEmpty()) errors.append("• Le nom est obligatoire.\n");
        else if (nom.length() < 3 || nom.length() > 50) errors.append("• Le nom doit contenir entre 3 et 50 caractères.\n");

        // Description
        String desc = tfDescription.getText().trim();
        if (desc.length() > 250) errors.append("• La description ne peut pas dépasser 250 caractères.\n");

        // Prix
        String prixText = tfPrix.getText().trim();
        if (prixText.isEmpty()) errors.append("• Le prix est obligatoire.\n");
        else {
            try {
                double prix = Double.parseDouble(prixText);
                if (prix <= 0) errors.append("• Le prix doit être supérieur à 0.\n");
            } catch (NumberFormatException e) {
                errors.append("• Le prix doit être un nombre valide (ex: 28.50).\n");
            }
        }

        // Catégorie
        if (cbCategorie.getValue() == null) errors.append("• La catégorie est obligatoire.\n");

        if (errors.length() > 0) {
            showErrorAlert("Champs incomplets ou invalides", errors.toString());
            return false;
        }
        return true;
    }

    private void clearInputs() {
        tfNom.clear();
        tfDescription.clear();
        tfPrix.clear();
        cbCategorie.setValue(null);
        tableProduit.getSelectionModel().clearSelection();
        currentSelection = null;
    }

    private void chargerProduits() {
        ObservableList<ProduitData> data = FXCollections.observableArrayList();
        for (Produit p : produitService.getAllProduits()) {
            data.add(new ProduitData(
                    p.getIdProduit(),
                    p.getNom(),
                    p.getDescription(),
                    p.getPrix(),
                    p.getCategorie()
            ));
        }
        tableProduit.setItems(data);
        lblStatus.setText("Liste chargée : " + data.size() + " produits");
    }

    private void showSuccessAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(title);
        alert.setContentText(content);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialogPane.getStyleClass().add("success-alert");
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(title);
        alert.setContentText(content);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialogPane.getStyleClass().add("error-alert");
        alert.showAndWait();
    }

    private void showWarningAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Attention");
        alert.setHeaderText(title);
        alert.setContentText(content);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialogPane.getStyleClass().add("warning-alert");
        alert.showAndWait();
    }

    private void showInfoAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }




}
