package controller;

import entities.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class DashboardFournisseurController {

    @FXML private Label welcomeLabel;
    @FXML private AnchorPane contentPane;

    private User currentUser;

    // ================= INITIALISATION =================
    @FXML
    public void initialize() {
        System.out.println("✅ Dashboard Fournisseur initialisé");
    }

    // ================= DÉFINIR L'UTILISATEUR =================
    public void setUser(User user) {
        this.currentUser = user;
        if (welcomeLabel != null) {
            welcomeLabel.setText("Bienvenue, " + user.getNom() + " !");
        }
        System.out.println("✅ Utilisateur défini : " + user.getNom() + " (Fournisseur)");
    }

    // ================= NAVIGATION VERS PROFIL =================
    @FXML
    private void handleGoToProfil(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profil.fxml"));
            Parent root = loader.load();

            ProfilController controller = loader.getController();
            controller.setUser(currentUser);

            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Mon Profil");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement du profil");
            e.printStackTrace();
        }
    }

    // ================= DÉCONNEXION =================
    @FXML
    private void handleLogout(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Login");

            System.out.println("✅ Déconnexion réussie");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la déconnexion");
            e.printStackTrace();
        }
    }

    // ================= MÉTHODE UTILITAIRE =================
    /**
     * Récupère l'utilisateur connecté
     * Vos collègues peuvent utiliser cette méthode dans leurs fonctionnalités
     * @return User connecté
     */
    public User getCurrentUser() {
        return currentUser;
    }
}