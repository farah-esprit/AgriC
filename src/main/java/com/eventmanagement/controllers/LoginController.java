package com.eventmanagement.controllers;

import com.eventmanagement.dao.UtilisateurDAO;
import com.eventmanagement.models.Utilisateur;
import com.eventmanagement.utils.PasswordHasher;
import com.eventmanagement.utils.Session;
import com.eventmanagement.utils.ValidationUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {
    
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private Button togglePasswordBtn;
    @FXML private Label errorLabel;
    
    private UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private boolean passwordVisible = false;
    
    @FXML
    public void initialize() {
        passwordVisibleField.setVisible(false);
        passwordVisibleField.setManaged(false);
        
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());
    }
    
    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        
        String emailError = ValidationUtils.validateEmail(email);
        if (emailError != null) {
            showError(emailError);
            return;
        }

        if (!ValidationUtils.isNotEmpty(password)) {
            showError("Le mot de passe est requis");
            return;
        }
        
        String hashedPassword = PasswordHasher.hashPassword(password);
        Utilisateur user = utilisateurDAO.authenticate(email, hashedPassword);
        
        if (user != null) {
            Session.getInstance().setCurrentUser(user);
            
            try {
                if ("ADMIN".equals(user.getRole())) {
                    loadAdminDashboard();
                } else {
                    loadUserDashboard();
                }
            } catch (Exception e) {
                e.printStackTrace();
                showError("Erreur lors du chargement du tableau de bord");
            }
        } else {
            showError("Email ou mot de passe incorrect");
        }
    }
    
    @FXML
    private void handleRegister() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/register.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
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
            togglePasswordBtn.setText("🙈");
        } else {
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            togglePasswordBtn.setText("👁");
        }
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
    
    private void loadUserDashboard() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/user-dashboard.fxml"));
        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Gestion des Événements - Utilisateur");
    }
    
    private void loadAdminDashboard() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/admin-dashboard.fxml"));
        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Gestion des Événements - Admin");
    }
}
