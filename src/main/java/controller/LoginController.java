package controller;

import entities.User;
import entities.EtatCompte;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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
        // Cacher le message d'erreur au départ
        if (errorLabel != null) {
            errorLabel.setText("");
        }
    }

    @FXML
    private void handleLogin() {
        // Récupérer les valeurs des champs
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        // Validation des champs vides
        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs");
            return;
        }

        // Validation du format email
        if (!isValidEmail(email)) {
            showError("Format d'email invalide");
            return;
        }

        // Tentative d'authentification
        User user = userService.authenticate(email, password);

        if (user == null) {
            showError("Email ou mot de passe incorrect");
            return;
        }

        // Vérifier si le compte est actif
        if (user.getEtatCompte() == EtatCompte.BLOQUE) {
            showError("Votre compte est bloqué. Contactez l'administrateur.");
            return;
        }

        // Connexion réussie !
        showSuccess("Connexion réussie ! Bienvenue " + user.getNom());

        // TODO: Rediriger vers la page principale selon le rôle
        redirectToDashboard(user);
    }

    // Afficher un message d'erreur
    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        }
    }

    // Afficher un message de succès
    private void showSuccess(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        }
    }

    // Validation simple du format email
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    // Redirection vers le dashboard selon le rôle
    private void redirectToDashboard(User user) {
        System.out.println("Redirection pour l'utilisateur : " + user.getNom());
        System.out.println("Rôle : " + user.getRole());

        // TODO: Implémenter la navigation vers les différents dashboards
        switch (user.getRole()) {
            case ADMIN:
                // Charger dashboard admin
                System.out.println("→ Dashboard Admin");
                break;
            case AGRICULTEUR:
                // Charger dashboard agriculteur
                System.out.println("→ Dashboard Agriculteur");
                break;
            case EXPERT:
                // Charger dashboard expert
                System.out.println("→ Dashboard Expert");
                break;
            case FOURNISSEUR:
                // Charger dashboard fournisseur
                System.out.println("→ Dashboard Fournisseur");
                break;
        }
    }
}