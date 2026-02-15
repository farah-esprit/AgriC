package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import java.io.IOException;

public class HomeController {

    @FXML
    private void goToProduits() {
        openView("/views/produit.fxml", "Gestion des Produits");
    }

    @FXML
    private void goToStock() {
        openView("/views/stockview.fxml", "Gestion du Stock");
    }

    @FXML
    private void goToCommandes() {
        openView("/views/commande.fxml", "Gestion des Commandes");
    }

    @FXML
    private void voirHistorique() {
        openView("/views/historique-commandes.fxml", "Historique des Commandes");
    }

    private void openView(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 1250, 800));
            stage.setTitle(title + " - AgriConnect");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur chargement : " + fxmlPath).showAndWait();
        }
    }

}