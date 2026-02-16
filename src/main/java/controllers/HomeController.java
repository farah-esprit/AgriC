package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.io.IOException;

public class HomeController {

    // ═══════════════════════════════════════════════════════════
    // NAVIGATION VERS LES DIFFÉRENTES PAGES
    // ═══════════════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════════════
    // MÉTHODE POUR OUVRIR UNE NOUVELLE VUE
    // ═══════════════════════════════════════════════════════════

    private void openView(String fxmlPath, String title) {
        try {
            // Charger le fichier FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // Créer une nouvelle fenêtre
            Stage newStage = new Stage();
            newStage.setScene(new Scene(root, 1600, 900));
            newStage.setTitle(title + " - AgriConnect");

            // Fermer la fenêtre Home actuelle
            closeCurrentWindow();

            // Afficher la nouvelle fenêtre
            newStage.show();

            System.out.println("✅ Ouverture de : " + title);

        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur de chargement", "Impossible d'ouvrir : " + fxmlPath + "\n" + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // MÉTHODE POUR FERMER LA FENÊTRE ACTUELLE
    // ═══════════════════════════════════════════════════════════

    private void closeCurrentWindow() {
        for (Window window : Stage.getWindows()) {
            if (window instanceof Stage && window.isShowing()) {
                Stage stage = (Stage) window;
                if (stage.getScene() != null && stage.getScene().getRoot() != null) {
                    stage.close();
                    System.out.println("🔒 Fenêtre Home fermée");
                    break;
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // MÉTHODE POUR AFFICHER LES ERREURS
    // ═══════════════════════════════════════════════════════════

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Une erreur s'est produite");
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ═══════════════════════════════════════════════════════════
    // EFFETS HOVER SUR LES BOUTONS DE LA SIDEBAR
    // ═══════════════════════════════════════════════════════════

    /**
     * Effet au survol de la souris (mouse enter)
     * Change la couleur de fond du bouton
     */
    @FXML
    private void onMouseEntered(javafx.scene.input.MouseEvent event) {
        if (event.getSource() instanceof Button) {
            Button btn = (Button) event.getSource();

            // Ajouter un fond semi-transparent au survol
            String currentStyle = btn.getStyle();
            if (!currentStyle.contains("background-color: rgba(255,255,255,0.25)")) {
                btn.setStyle(currentStyle + "-fx-background-color: rgba(255,255,255,0.2);");
            }
        }
    }

    /**
     * Effet quand la souris quitte le bouton (mouse exit)
     * Restaure le style d'origine
     */
    @FXML
    private void onMouseExited(javafx.scene.input.MouseEvent event) {
        if (event.getSource() instanceof Button) {
            Button btn = (Button) event.getSource();

            // Enlever l'effet hover
            String currentStyle = btn.getStyle();
            String newStyle = currentStyle.replace("-fx-background-color: rgba(255,255,255,0.2);", "");
            btn.setStyle(newStyle);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // MÉTHODES SUPPLÉMENTAIRES (OPTIONNELLES)
    // ═══════════════════════════════════════════════════════════

    /**
     * Méthode de déconnexion (à implémenter)
     */
    @FXML
    private void handleDeconnexion() {
        try {
            // Fermer toutes les fenêtres
            for (Window window : Stage.getWindows()) {
                if (window instanceof Stage) {
                    ((Stage) window).close();
                }
            }

            // Rediriger vers la page de connexion (si elle existe)
            // openView("/views/login.fxml", "Connexion");

            System.out.println("👋 Déconnexion réussie");

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur de déconnexion", e.getMessage());
        }
    }

    /**
     * Afficher le profil utilisateur (à implémenter)
     */
    @FXML
    private void showProfile() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Mon Profil");
        alert.setHeaderText("Profil Fournisseur");
        alert.setContentText("Fonctionnalité en cours de développement...\n\nVous pourrez bientôt modifier vos informations personnelles.");
        alert.showAndWait();
    }
}