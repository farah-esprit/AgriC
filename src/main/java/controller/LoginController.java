package controller;

import entities.User;
import entities.EtatCompte;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import service.UserService;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Label errorLabel;

    private UserService userService;

    @FXML
    public void initialize() {
        userService = new UserService();

        if (errorLabel != null) {
            errorLabel.setText("");
        }
    }

    @FXML
    private void handleRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/register.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Inscription");

        } catch (Exception e) {
            System.out.println("Erreur lors du chargement de l'inscription");
            e.printStackTrace();
            showError("Erreur lors du chargement de la page d'inscription");
        }
    }

    //LOGIN
    @FXML
    private void handleLogin() {

        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs");
            return;
        }

        if (!isValidEmail(email)) {
            showError("Format d'email invalide");
            return;
        }

        // Authentification
        User user = userService.authenticate(email, password);

        if (user == null) {
            showError("Email ou mot de passe incorrect");
            return;
        }

        // Vérification de l'état du compte
        if (user.getEtatCompte() == EtatCompte.BLOQUE) {
            showError("Votre compte est bloqué. Contactez l'administrateur.");
            return;
        }

        // Connexion réussie
        showSuccess("Connexion réussie ! Bienvenue " + user.getNom());

        // Redirection selon le rôle
        redirectToDashboard(user);
    }

    //MESSAGES
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
    }

    private void showSuccess(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
    }

    //VALIDATION EMAIL
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private void redirectToDashboard(User user) {
        try {
            String fxmlFile = "";

            // Choisir le dashboard selon le rôle
            switch (user.getRole()) {
                case ADMIN:
                    fxmlFile = "/adminDashboard.fxml";
                    break;
                case AGRICULTEUR:
                    fxmlFile = "/agriculteurDashboard.fxml";
                    break;
                case EXPERT:
                    fxmlFile = "/expertDashboard.fxml";
                    break;
                case FOURNISSEUR:
                    fxmlFile = "/fournisseurDashboard.fxml";
                    break;
            }

            System.out.println("📂 Chargement de : " + fxmlFile);

            // Charger le FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            // Passer l'utilisateur au controller approprié
            Object controller = loader.getController();

            if (controller instanceof DashboardAdminController) {
                ((DashboardAdminController) controller).setUser(user);
                System.out.println("✅ DashboardAdminController initialisé");
            } else if (controller instanceof DashboardAgriculteurController) {
                ((DashboardAgriculteurController) controller).setUser(user);
                System.out.println("✅ DashboardAgriculteurController initialisé");
            }
            else if (controller instanceof DashboardFournisseurController) {
                ((DashboardFournisseurController) controller).setUser(user);
                System.out.println("✅ DashboardFournisseurrController initialisé");
            }
            else if (controller instanceof DashboardExpertController) {
                ((DashboardExpertController) controller).setUser(user);
                System.out.println("✅ DashboardExpertController initialisé");
            }


            // Changer la scène
            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("AgriConnect - Dashboard " + user.getRole());

            System.out.println("✅ Redirection réussie vers " + fxmlFile);

        } catch (Exception e) {
            System.out.println("❌ ERREUR lors de la redirection :");
            e.printStackTrace();
            showError("Erreur lors de la redirection : " + e.getMessage());
        }
    }



}