package controllers;

import entities.Commande;
import entities.Produit;
import services.CommandeService;
import services.ProduitService;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class HistoriqueCommandesController {

    @FXML private TableView<CommandeMini> tableHistorique;
    @FXML private TableColumn<CommandeMini, Integer> colId;
    @FXML private TableColumn<CommandeMini, String> colDate;
    @FXML private TableColumn<CommandeMini, String> colProduit;
    @FXML private TableColumn<CommandeMini, Integer> colQuantite;
    @FXML private TableColumn<CommandeMini, String> colStatut;
    @FXML private Label lblStatus;

    private final CommandeService commandeService = new CommandeService();
    private final ProduitService produitService = new ProduitService();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(cd -> cd.getValue().id.asObject());
        colDate.setCellValueFactory(cd -> cd.getValue().date);
        colProduit.setCellValueFactory(cd -> cd.getValue().produit);
        colQuantite.setCellValueFactory(cd -> cd.getValue().quantite.asObject());
        colStatut.setCellValueFactory(cd -> cd.getValue().statut);

        actualiser();
    }

    @FXML
    private void actualiser() {
        List<Commande> dernieres = commandeService.getDernieresCommandes(20);

        ObservableList<CommandeMini> data = FXCollections.observableArrayList();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (Commande c : dernieres) {
            String produitNom = "Inconnu";
            Produit p = produitService.getById(c.getIdProduit());
            if (p != null) produitNom = p.getNom();

            data.add(new CommandeMini(
                    c.getIdCommande(),
                    c.getDateCommande().format(fmt),
                    produitNom,
                    c.getQuantiteCommandee(),
                    c.getStatut()
            ));
        }

        tableHistorique.setItems(data);

        // Mise à jour du status
        if (lblStatus != null) {
            lblStatus.setText(data.size() + " commande(s) chargée(s)");
        }

        showInfoAlert("Liste actualisée", "Historique rafraîchi : " + data.size() + " commande(s) affichée(s).");
    }

    @FXML
    private void retourAccueil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/home.fxml"));
            Parent root = loader.load();

            Stage homeStage = new Stage();
            homeStage.setScene(new Scene(root, 1920, 1080));
            homeStage.setTitle("Accueil - AgriConnect");
            homeStage.setMaximized(true);

            Stage currentStage = (Stage) tableHistorique.getScene().getWindow();
            currentStage.close();

            homeStage.show();

            System.out.println("🏠 Retour à l'accueil depuis Historique");

        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Erreur de navigation", "Impossible de retourner à l'accueil : " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // MÉTHODES D'ALERTE
    // ═══════════════════════════════════════════════════════════

    private void showInfoAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
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

    // ═══════════════════════════════════════════════════════════
    // MINI MODEL FOR DISPLAY
    // ═══════════════════════════════════════════════════════════

    public static class CommandeMini {
        public final IntegerProperty id = new SimpleIntegerProperty();
        public final StringProperty date = new SimpleStringProperty();
        public final StringProperty produit = new SimpleStringProperty();
        public final IntegerProperty quantite = new SimpleIntegerProperty();
        public final StringProperty statut = new SimpleStringProperty();

        public CommandeMini(int id, String date, String produit, int qte, String statut) {
            this.id.set(id);
            this.date.set(date);
            this.produit.set(produit);
            this.quantite.set(qte);
            this.statut.set(statut);
        }
    }
}