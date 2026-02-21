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
import javafx.scene.layout.*;
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
    @FXML private FlowPane cardsContainer;
    @FXML private Label lblStatus;

    private Connection conn;
    private StockData currentSelection = null;
    private VBox selectedCard = null;
    private ObservableList<StockData> stockList = FXCollections.observableArrayList();

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
        chargerProduits();
        chargerStock();
    }

    // ✅ Crée une card pour un stock
    private VBox createCard(StockData s) {
        VBox card = new VBox(10);
        card.setPrefWidth(210);
        card.setMaxWidth(210);

        // Couleur selon niveau stock
        int qte = s.quantite.get();
        int seuil = s.seuilAlerte.get();
        String statut, couleurBadge, emoji;
        if (qte <= 0) {
            statut = "Rupture";
            couleurBadge = "#d9534f";
            emoji = "🔴";
        } else if (qte <= seuil) {
            statut = "Stock bas";
            couleurBadge = "#f0ad4e";
            emoji = "🟠";
        } else {
            statut = "En stock";
            couleurBadge = "#5a8c4a";
            emoji = "🟢";
        }

        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-color: " + couleurBadge + ";" +
                        "-fx-border-width: 2;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 10, 0, 0, 3);" +
                        "-fx-padding: 15;" +
                        "-fx-cursor: hand;"
        );

        // Emoji statut grand
        Label lblEmoji = new Label(emoji);
        lblEmoji.setStyle("-fx-font-size: 32px;");

        HBox emojiBox = new HBox(lblEmoji);
        emojiBox.setStyle("-fx-alignment: center;");

        // Nom produit
        Label lblNom = new Label(s.produit.get());
        lblNom.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2d2d2d; -fx-wrap-text: true;");
        lblNom.setMaxWidth(185);
        lblNom.setWrapText(true);

        // Badge statut
        Label lblStatut = new Label(statut);
        lblStatut.setStyle(
                "-fx-background-color: " + couleurBadge + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 3 10;" +
                        "-fx-background-radius: 10;"
        );

        Separator sep = new Separator();

        // Quantité
        HBox qteBox = new HBox(8);
        Label qteLabel = new Label("Quantité :");
        qteLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        Label qteVal = new Label(String.valueOf(qte));
        qteVal.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + couleurBadge + ";");
        qteBox.getChildren().addAll(qteLabel, qteVal);

        // Seuil
        HBox seuilBox = new HBox(8);
        Label seuilLabel = new Label("Seuil alerte :");
        seuilLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        Label seuilVal = new Label(String.valueOf(seuil));
        seuilVal.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #888;");
        seuilBox.getChildren().addAll(seuilLabel, seuilVal);

        card.getChildren().addAll(emojiBox, lblNom, lblStatut, sep, qteBox, seuilBox);

        // Clic
        card.setOnMouseClicked(e -> selectCard(s, card, couleurBadge));

        // Hover
        card.setOnMouseEntered(e -> {
            if (currentSelection == null || currentSelection.idStock.get() != s.idStock.get()) {
                card.setStyle(
                        "-fx-background-color: #f9fdf9;" +
                                "-fx-background-radius: 12;" +
                                "-fx-border-radius: 12;" +
                                "-fx-border-color: " + couleurBadge + ";" +
                                "-fx-border-width: 2.5;" +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 14, 0, 0, 5);" +
                                "-fx-padding: 15;" +
                                "-fx-cursor: hand;"
                );
            }
        });
        card.setOnMouseExited(e -> {
            if (currentSelection == null || currentSelection.idStock.get() != s.idStock.get()) {
                card.setStyle(
                        "-fx-background-color: white;" +
                                "-fx-background-radius: 12;" +
                                "-fx-border-radius: 12;" +
                                "-fx-border-color: " + couleurBadge + ";" +
                                "-fx-border-width: 2;" +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 10, 0, 0, 3);" +
                                "-fx-padding: 15;" +
                                "-fx-cursor: hand;"
                );
            }
        });

        return card;
    }

    private void selectCard(StockData s, VBox card, String couleurBadge) {
        // Désélectionner ancienne
        if (selectedCard != null) {
            String oldCouleur = currentSelection != null ?
                    (currentSelection.quantite.get() <= 0 ? "#d9534f" :
                            currentSelection.quantite.get() <= currentSelection.seuilAlerte.get() ? "#f0ad4e" : "#5a8c4a")
                    : "#5a8c4a";
            selectedCard.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-background-radius: 12;" +
                            "-fx-border-radius: 12;" +
                            "-fx-border-color: " + oldCouleur + ";" +
                            "-fx-border-width: 2;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 10, 0, 0, 3);" +
                            "-fx-padding: 15;" +
                            "-fx-cursor: hand;"
            );
        }

        // Sélectionner nouvelle
        card.setStyle(
                "-fx-background-color: #e8f5e9;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-color: " + couleurBadge + ";" +
                        "-fx-border-width: 3;" +
                        "-fx-effect: dropshadow(gaussian, rgba(74,124,58,0.35), 14, 0, 0, 5);" +
                        "-fx-padding: 15;" +
                        "-fx-cursor: hand;"
        );
        selectedCard = card;
        currentSelection = s;

        // Remplir formulaire
        cbProduit.setValue(getProduitById(s.idProduit.get()));
        tfQuantite.setText(String.valueOf(s.quantite.get()));
        tfSeuilAlerte.setText(String.valueOf(s.seuilAlerte.get()));
    }

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
        } catch (IOException e) {
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
        if (cbProduit.getValue() == null) errors.append("• Le produit est obligatoire\n");
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
                showSuccessAlert("Stock ajouté !", "Stock ajouté avec succès (ID: " + id + ")");
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
            showWarningAlert("Aucune sélection", "Veuillez sélectionner une card.");
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
            showSuccessAlert("Stock modifié !", "Les modifications ont été enregistrées.");
            chargerStock();
        } catch (Exception e) {
            showErrorAlert("Erreur lors de la modification", e.getMessage());
        }
    }

    @FXML
    private void handleSupprimer() {
        if (currentSelection == null) {
            showWarningAlert("Aucune sélection", "Veuillez sélectionner une card.");
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
                try (PreparedStatement pst = conn.prepareStatement("DELETE FROM stock WHERE idStock=?")) {
                    pst.setInt(1, currentSelection.idStock.get());
                    pst.executeUpdate();
                }
                showSuccessAlert("Supprimé !", "Le stock a été supprimé.");
                clearInputs();
                chargerStock();
            } catch (Exception e) {
                showErrorAlert("Erreur suppression", e.getMessage());
            }
        }
    }

    @FXML
    public void handleActualiser() {
        chargerStock();
        showInfoAlert("Liste actualisée", stockList.size() + " stock(s) affiché(s).");
    }

    private void chargerStock() {
        stockList.clear();
        cardsContainer.getChildren().clear();

        String sql = """
            SELECT s.idStock, s.idProduit, s.quantite, s.seuilAlert, p.nom
            FROM stock s
            LEFT JOIN produit p ON p.id_produit = s.idProduit
            ORDER BY s.idStock DESC
            """;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                StockData data = new StockData(
                        rs.getInt("idStock"),
                        rs.getInt("idProduit"),
                        rs.getString("nom") != null ? rs.getString("nom") : "Inconnu",
                        rs.getInt("quantite"),
                        rs.getInt("seuilAlert")
                );
                stockList.add(data);
                cardsContainer.getChildren().add(createCard(data));
            }
        } catch (SQLException e) {
            System.err.println("❌ SQL: " + e.getMessage());
        }

        if (lblStatus != null)
            lblStatus.setText("Liste chargée : " + stockList.size() + " stock(s)");
    }

    private void clearInputs() {
        tfQuantite.clear();
        tfSeuilAlerte.clear();
        cbProduit.getSelectionModel().clearSelection();
        currentSelection = null;
        selectedCard = null;
    }

    @FXML
    private void exporterCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter les stocks en CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fileChooser.setInitialFileName("stocks_" + LocalDate.now() + ".csv");
        File file = fileChooser.showSaveDialog(cardsContainer.getScene().getWindow());
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("ID Stock,ID Produit,Produit,Quantité,Seuil Alerte\n");
                for (StockData s : stockList) {
                    writer.write(String.format("%d,%d,\"%s\",%d,%d\n",
                            s.idStock.get(), s.idProduit.get(),
                            s.produit.get(), s.quantite.get(), s.seuilAlerte.get()));
                }
                showSuccessAlert("Export réussi", "Fichier sauvegardé :\n" + file.getAbsolutePath());
            } catch (IOException e) {
                showErrorAlert("Erreur d'export", e.getMessage());
            }
        }
    }

    private void showSuccessAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Succès"); a.setHeaderText(title); a.setContentText(content); a.showAndWait();
    }
    private void showErrorAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur"); a.setHeaderText(title); a.setContentText(content); a.showAndWait();
    }
    private void showWarningAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Attention"); a.setHeaderText(title); a.setContentText(content); a.showAndWait();
    }
    private void showInfoAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Information"); a.setHeaderText(title); a.setContentText(content); a.showAndWait();
    }
    private void showSuccess(String msg) { showSuccessAlert("Succès", msg); }
    private void showError(String msg) { showErrorAlert("Erreur", msg); }
}