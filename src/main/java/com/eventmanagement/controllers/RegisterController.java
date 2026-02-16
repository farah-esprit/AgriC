package com.eventmanagement.controllers;

import com.eventmanagement.dao.UtilisateurDAO;
import com.eventmanagement.models.Utilisateur;
import com.eventmanagement.utils.PasswordHasher;
import com.eventmanagement.utils.ValidationUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegisterController {

    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField passwordVisibleField;
    @FXML private TextField confirmPasswordVisibleField;
    @FXML private Button togglePasswordBtn;
    @FXML private Button toggleConfirmPasswordBtn;
    @FXML private Label errorLabel;

    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private boolean passwordVisible = false;
    private boolean confirmPasswordVisible = false;

    @FXML
    public void initialize() {
        passwordVisibleField.setVisible(false);
        passwordVisibleField.setManaged(false);
        confirmPasswordVisibleField.setVisible(false);
        confirmPasswordVisibleField.setManaged(false);

        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());
        confirmPasswordVisibleField.textProperty().bindBidirectional(confirmPasswordField.textProperty());
    }

    @FXML
    private void handleRegister() {
        String nom = nomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        String nomError = ValidationUtils.validateUsername(nom);
        if (nomError != null) {
            showError(nomError);
            return;
        }

        String emailError = ValidationUtils.validateEmail(email);
        if (emailError != null) {
            showError(emailError);
            return;
        }

        String passwordError = ValidationUtils.validatePassword(password);
        if (passwordError != null) {
            showError(passwordError);
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Les mots de passe ne correspondent pas");
            return;
        }

        if (utilisateurDAO.emailExists(email)) {
            showError("Cet email est deja utilise");
            return;
        }

        Utilisateur user = new Utilisateur();
        user.setNom(nom);
        user.setEmail(email);
        user.setMotDePasse(PasswordHasher.hashPassword(password));
        user.setRole("USER");
        user.setStatut("ACTIF");

        if (utilisateurDAO.create(user)) {
            showSuccess("Inscription reussie. Connectez-vous.");
            handleBackToLogin();
        } else {
            showError("Erreur lors de l'inscription");
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            showError("Impossible d'ouvrir la page login: " + e.getMessage());
        }
    }

    @FXML
    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true);
            togglePasswordBtn.setText("Hide");
        } else {
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            togglePasswordBtn.setText("Show");
        }
    }

    @FXML
    private void toggleConfirmPasswordVisibility() {
        confirmPasswordVisible = !confirmPasswordVisible;
        if (confirmPasswordVisible) {
            confirmPasswordField.setVisible(false);
            confirmPasswordField.setManaged(false);
            confirmPasswordVisibleField.setVisible(true);
            confirmPasswordVisibleField.setManaged(true);
            toggleConfirmPasswordBtn.setText("Hide");
        } else {
            confirmPasswordVisibleField.setVisible(false);
            confirmPasswordVisibleField.setManaged(false);
            confirmPasswordField.setVisible(true);
            confirmPasswordField.setManaged(true);
            toggleConfirmPasswordBtn.setText("Show");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #c62828;");
        errorLabel.setVisible(true);
    }

    private void showSuccess(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #2e7d32;");
        errorLabel.setVisible(true);
    }
}
