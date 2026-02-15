package controllers;

import entities.Produit;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.example.utils.MyDatabase;

import java.io.File;
import java.io.FileWriter;
import java.sql.*;

public class StockController {

    @FXML private ComboBox<Produit> cbProduit;
    @FXML private TextField tfQuantite, tfSeuilAlerte;
    @FXML private TableView<StockData> tableStock;
    @FXML private TableColumn<StockData, Integer> colIdStock, colIdProduit, colQuantite, colSeuilAlerte;
    @FXML private TableColumn<StockData, String> colProduit;

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

    // ───────────────────────────────
    // Validation des champs
    // ───────────────────────────────
    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        if (cbProduit.getValue() == null)
            errors.append("• Le produit est obligatoire\n");

        // Quantité
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

        // Seuil alerte
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
            showError(errors.toString());
            return false;
        }
        return true;
    }

    // ───────────────────────────────
    // Ajouter / Modifier avec validation
    // ───────────────────────────────
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
                showSuccess("Stock ajouté (ID: " + id + ")");
                clearInputs();
                chargerStock();
            }

        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleModifier() {
        if (currentSelection == null) {
            showError("Sélectionnez une ligne !");
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
            showSuccess("Stock modifié");

        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleSupprimer() {
        if (currentSelection == null) {
            showError("Sélectionnez une ligne !");
            return;
        }

        try {
            String sql = "DELETE FROM stock WHERE idStock=?";
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1, currentSelection.idStock.get());
                pst.executeUpdate();
            }

            chargerStock();
            showSuccess("Stock supprimé");

        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void handleActualiser() {
        chargerStock();
        showSuccess("Liste de stock actualisée !");
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
    }

    private void clearInputs() {
        tfQuantite.clear();
        tfSeuilAlerte.clear();
        cbProduit.getSelectionModel().clearSelection();
        tableStock.getSelectionModel().clearSelection();
        currentSelection = null;
    }

    private void showSuccess(String message) {
        new Alert(Alert.AlertType.INFORMATION, message).showAndWait();
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }

    @FXML
    private void exporterCSV() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le fichier CSV");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            File file = fileChooser.showSaveDialog(tableStock.getScene().getWindow());

            if (file != null) {
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write("ID Stock,ID Produit,Produit,Quantité,Seuil Alerte\n");
                    for (StockData stock : tableStock.getItems()) {
                        writer.write(String.format("%d,%d,%s,%d,%d\n",
                                stock.idStock.get(),
                                stock.idProduit.get(),
                                stock.produit.get(),
                                stock.quantite.get(),
                                stock.seuilAlerte.get()));
                    }
                }
                showSuccess("Export CSV terminé !");
            }
        } catch (Exception e) {
            showError("Erreur lors de l'export CSV : " + e.getMessage());
        }
    }
}
