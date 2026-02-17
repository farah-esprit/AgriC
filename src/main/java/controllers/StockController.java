package controllers;

import entities.Produit;
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
import org.example.utils.MyDatabase;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;

public class StockController {

    @FXML private ComboBox<Produit> cbProduit;
    @FXML private TextField tfQuantite, tfSeuilAlerte;
    @FXML private Button btnRetourAccueil;
    @FXML private TableView<StockData> tableStock;
    @FXML private TableColumn<StockData, Integer> colIdStock, colIdProduit, colQuantite, colSeuilAlerte;
    @FXML private TableColumn<StockData, String> colProduit;
    @FXML private Label lblStatus;

    private Connection conn;
    private StockData currentSelection = null;

    public static class StockData {
        public final IntegerProperty idStock = new SimpleIntegerProperty();
        public final IntegerProperty idProduit = new SimpleIntegerProperty();
        public final StringProperty produit = new SimpleStringProperty();
        public final IntegerProperty quantite = new SimpleIntegerProperty();
        public final IntegerProperty seuilAlerte = new SimpleIntegerProperty();

        public StockData(int idStock, int idProduit, String produit, int quantite, int seuilAlerte) {
            this.idStock.set(idStock);
            this.idProduit.set(idProduit);
            this.produit.set(produit);
            this.quantite.set(quantite);
            this.seuilAlerte.set(seuilAlerte);
        }
    }

    @FXML
    public void initialize() throws SQLException {
        conn = MyDatabase.getInstance().getConnection();

        colIdStock.setCellValueFactory(cd -> cd.getValue().idStock.asObject());
        colIdProduit.setCellValueFactory(cd -> cd.getValue().idProduit.asObject());
        colProduit.setCellValueFactory(cd -> cd.getValue().produit);
        colQuantite.setCellValueFactory(cd -> cd.getValue().quantite.asObject());
        colSeuilAlerte.setCellValueFactory(cd -> cd.getValue().seuilAlerte.asObject());

        chargerProduits();
        chargerStock();

        tableStock.getSelectionModel().selectedItemProperty().addListener((obs, old, newItem) -> {
            currentSelection = newItem;
            if (newItem != null) {
                cbProduit.setValue(getProduitById(newItem.idProduit.get()));
                tfQuantite.setText(String.valueOf(newItem.quantite.get()));
                tfSeuilAlerte.setText(String.valueOf(newItem.seuilAlerte.get()));
            }
        });
    }

    // ✅ NOUVELLE MÉTHODE : Retour à l'accueil
    @FXML
    private void handleRetourAccueil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/home.fxml"));
            Parent root = loader.load();

            Stage homeStage = new Stage();
            homeStage.setScene(new Scene(root, 1920, 1080));
            homeStage.setTitle("Accueil - AgriConnect");
            homeStage.setMaximized(true);

            Stage currentStage = (Stage) tfQuantite.getScene().getWindow();
            currentStage.close();

            homeStage.show();

            System.out.println("🏠 Retour à l'accueil depuis Stock");

        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur de navigation : " + e.getMessage());
        }
    }

    private void chargerProduits() {
        ObservableList<Produit> produits = FXCollections.observableArrayList();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM produit WHERE actif=1")) {
            while (rs.next()) {
                produits.add(new Produit(
                        rs.getLong("id_produit"),
                        rs.getString("nom"),
                        rs.getString("description"),
                        rs.getDouble("prix"),
                        rs.getString("categorie"),
                        rs.getBoolean("actif")
                ));
            }
        } catch (SQLException e) {
            System.err.println("❌ SQL Produits: " + e.getMessage());
        }

        cbProduit.setItems(produits);
        cbProduit.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Produit item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNom());
            }
        });
        cbProduit.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Produit item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Sélectionner un produit" : item.getNom());
            }
        });
    }

    private Produit getProduitById(int id) {
        for (Produit p : cbProduit.getItems()) {
            if (p.getIdProduit() == id) return p;
        }
        return null;
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        if (cbProduit.getValue() == null)
            errors.append("• Le produit est obligatoire\n");

        if (tfQuantite.getText().trim().isEmpty()) {
            errors.append("• La quantité est obligatoire\n");
        } else {
            try {
                int q = Integer.parseInt(tfQuantite.getText().trim());
                if (q <= 0) errors.append("• La quantité doit être supérieure à 0\n");
            } catch (NumberFormatException e) {
                errors.append("• La quantité doit être un nombre entier valide\n");
            }
        }

        if (tfSeuilAlerte.getText().trim().isEmpty()) {
            errors.append("• Le seuil d'alerte est obligatoire\n");
        } else {
            try {
                int s = Integer.parseInt(tfSeuilAlerte.getText().trim());
                if (s < 0) errors.append("• Le seuil d'alerte doit être ≥ 0\n");
            } catch (NumberFormatException e) {
                errors.append("• Le seuil d'alerte doit être un nombre entier valide\n");
            }
        }

        if (errors.length() > 0) {
            showErrorAlert("Champs incomplets ou invalides", errors.toString());
            return false;
        }
        return true;
    }

    @FXML
    private void handleAjouter() {
        if (!validateInput()) return;

        try {
            Produit produit = cbProduit.getValue();
            int quantite = Integer.parseInt(tfQuantite.getText().trim());
            int seuil = Integer.parseInt(tfSeuilAlerte.getText().trim());

            String sql = "INSERT INTO stock (quantite, disponible, seuilAlert, idProduit) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pst.setInt(1, quantite);
                pst.setInt(2, quantite);
                pst.setInt(3, seuil);
                pst.setLong(4, produit.getIdProduit());
                pst.executeUpdate();

                ResultSet rs = pst.getGeneratedKeys();
                int id = rs.next() ? rs.getInt(1) : 0;
                showSuccessAlert("Stock ajouté avec succès !", "Stock ajouté (ID: " + id + ")");
                clearInputs();
                chargerStock();
            }

        } catch (Exception e) {
            showErrorAlert("Erreur lors de l'ajout", e.getMessage());
        }
    }

    @FXML
    private void handleModifier() {
        if (currentSelection == null) {
            showWarningAlert("Aucune sélection", "Veuillez sélectionner une ligne dans le tableau.");
            return;
        }

        if (!validateInput()) return;

        try {
            Produit produit = cbProduit.getValue();
            int quantite = Integer.parseInt(tfQuantite.getText().trim());
            int seuil = Integer.parseInt(tfSeuilAlerte.getText().trim());

            String sql = "UPDATE stock SET quantite=?, seuilAlert=?, idProduit=? WHERE idStock=?";
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1, quantite);
                pst.setInt(2, seuil);
                pst.setInt(3, (int) produit.getIdProduit());
                pst.setInt(4, currentSelection.idStock.get());
                pst.executeUpdate();
            }

            chargerStock();
            showSuccessAlert("Stock modifié avec succès !", "Les modifications ont été enregistrées.");

        } catch (Exception e) {
            showErrorAlert("Erreur lors de la modification", e.getMessage());
        }
    }

    @FXML
    private void handleSupprimer() {
        if (currentSelection == null) {
            showWarningAlert("Aucune sélection", "Veuillez sélectionner une ligne à supprimer.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer ce stock ?");
        confirm.setContentText(
                "Produit : " + currentSelection.produit.get() + "\n" +
                        "Quantité : " + currentSelection.quantite.get() + "\n" +
                        "Cette action est irréversible."
        );

        ButtonType btnOui = new ButtonType("Oui, supprimer", ButtonBar.ButtonData.YES);
        ButtonType btnNon = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnOui, btnNon);

        if (confirm.showAndWait().orElse(btnNon) == btnOui) {
            try {
                String sql = "DELETE FROM stock WHERE idStock=?";
                try (PreparedStatement pst = conn.prepareStatement(sql)) {
                    pst.setInt(1, currentSelection.idStock.get());
                    pst.executeUpdate();
                }

                chargerStock();
                showSuccessAlert("Supprimé avec succès", "Le stock a été supprimé de la base.");
                clearInputs();

            } catch (Exception e) {
                showErrorAlert("Erreur suppression", e.getMessage());
            }
        }
    }

    @FXML
    public void handleActualiser() {
        chargerStock();
        showInfoAlert("Liste actualisée", "La liste des stocks a été rafraîchie.\n" + tableStock.getItems().size() + " stock(s) affiché(s).");
    }

    private void chargerStock() {
        ObservableList<StockData> data = FXCollections.observableArrayList();
        String sql = """
            SELECT s.idStock, s.idProduit, s.quantite, s.seuilAlert, p.nom
            FROM stock s
            LEFT JOIN produit p ON p.id_produit = s.idProduit
            ORDER BY s.idStock DESC
            """;

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                data.add(new StockData(
                        rs.getInt("idStock"),
                        rs.getInt("idProduit"),
                        rs.getString("nom") != null ? rs.getString("nom") : "Inconnu",
                        rs.getInt("quantite"),
                        rs.getInt("seuilAlert")
                ));
            }
        } catch (SQLException e) {
            System.err.println("❌ SQL: " + e.getMessage());
        }

        tableStock.setItems(data);
        if (lblStatus != null) {
            lblStatus.setText("Liste chargée : " + data.size() + " stock(s)");
        }
    }

    private void clearInputs() {
        tfQuantite.clear();
        tfSeuilAlerte.clear();
        cbProduit.getSelectionModel().clearSelection();
        tableStock.getSelectionModel().clearSelection();
        currentSelection = null;
    }

    @FXML
    private void exporterCSV() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Exporter les stocks en CSV");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv"));
            fileChooser.setInitialFileName("stocks_" + LocalDate.now() + ".csv");
            File file = fileChooser.showSaveDialog(tableStock.getScene().getWindow());

            if (file != null) {
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write("ID Stock,ID Produit,Produit,Quantité,Seuil Alerte\n");
                    for (StockData stock : tableStock.getItems()) {
                        writer.write(String.format("%d,%d,\"%s\",%d,%d\n",
                                stock.idStock.get(),
                                stock.idProduit.get(),
                                stock.produit.get(),
                                stock.quantite.get(),
                                stock.seuilAlerte.get()));
                    }
                }
                showSuccessAlert("Export réussi", "Le fichier CSV a été sauvegardé :\n" + file.getAbsolutePath());
            }
        } catch (Exception e) {
            showErrorAlert("Erreur d'export", "Impossible de sauvegarder le fichier :\n" + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // MÉTHODES D'ALERTE HARMONISÉES
    // ═══════════════════════════════════════════════════════════

    private void showSuccessAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showWarningAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Attention");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfoAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Méthodes deprecated (pour compatibilité)
    private void showSuccess(String message) {
        showSuccessAlert("Succès", message);
    }

    private void showError(String message) {
        showErrorAlert("Erreur", message);
    }
}