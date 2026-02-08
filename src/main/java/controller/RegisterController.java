package controller;

import entities.EtatCompte;
import entities.Role;
import entities.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import service.UserService;

public class RegisterController {

    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ChoiceBox<Role> roleChoice;
    @FXML private Label errorLabel;
    @FXML private Button registerButton;

    private UserService userService;

    @FXML
    public void initialize() {

        userService = new UserService();

        roleChoice.getItems().addAll(Role.AGRICULTEUR, Role.EXPERT, Role.FOURNISSEUR);
        roleChoice.setValue(Role.AGRICULTEUR);
    }

    @FXML
    private void handleRegister() {

        String nom = nomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        Role role = roleChoice.getValue();

        // ✔ Correction ici (tu avais "confirm" au lieu de confirmPassword)
        if (nom.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Veuillez remplir tous les champs");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Mot de passe incorrect");
            return;
        }

        if (userService.emailExiste(email)) {
            showError("Email déjà utilisé !");
            return;
        }

        // ✔ Correction principale : ajouter nom
        User newUser = new User(nom, email, password, role, EtatCompte.ACTIF);

        userService.ajouter(newUser);

        showSuccess("Compte créé avec succès");

        handleBackToLogin();
    }

    @FXML
    private void handleBackToLogin() {

        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showError(String msg){
        errorLabel.setText(msg);
        errorLabel.setStyle("-fx-text-fill:red;");
    }

    private void showSuccess(String msg){
        errorLabel.setText(msg);
        errorLabel.setStyle("-fx-text-fill:green;");
    }
}
