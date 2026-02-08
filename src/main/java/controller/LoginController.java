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

    // Initialisation automatique appelée après le chargement du FXML
    @FXML
    public void initialize() {
        userService = new UserService();

        if (errorLabel != null) {
            errorLabel.setText("");
        }
    }

    // ================= REGISTER =================
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
        }
    }

    // ================= LOGIN =================
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

        User user = userService.authenticate(email, password);

        if (user == null) {
            showError("Email ou mot de passe incorrect");
            return;
        }

        if (user.getEtatCompte() == EtatCompte.BLOQUE) {
            showError("Votre compte est bloqué");
            return;
        }

        showSuccess("Connexion réussie ! Bienvenue " + user.getNom());

        redirectToDashboard(user);
    }

    // ================= MESSAGES =================
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
    }

    private void showSuccess(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
    }

    // ================= VALIDATION EMAIL =================
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    // ================= REDIRECTION =================
    private void redirectToDashboard(User user) {

        System.out.println("Utilisateur : " + user.getNom());
        System.out.println("Rôle : " + user.getRole());

        switch (user.getRole()) {

            case ADMIN:
                System.out.println("Dashboard Admin");
                break;

            case AGRICULTEUR:
                System.out.println("Dashboard Agriculteur");
                break;

            case EXPERT:
                System.out.println("Dashboard Expert");
                break;

            case FOURNISSEUR:
                System.out.println("Dashboard Fournisseur");
                break;
        }
    }
}
