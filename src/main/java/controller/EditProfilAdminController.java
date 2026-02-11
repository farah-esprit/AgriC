package controller;

import entities.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import service.UserService;

public class EditProfilAdminController {

    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private PasswordField ancienMdpField;
    @FXML private PasswordField nouveauMdpField;
    @FXML private PasswordField confirmMdpField;
    @FXML private Label messageLabel;

    private User currentUser;
    private UserService userService;

    @FXML
    public void initialize() {
        userService = new UserService();
    }

    public void setUser(User user) {
        this.currentUser = user;
        if (nomField != null) nomField.setText(user.getNom());
        if (emailField != null) emailField.setText(user.getEmail());
    }

    @FXML
    private void handleEnregistrer(ActionEvent event) {
        String nouveauNom = nomField.getText().trim();
        String nouvelEmail = emailField.getText().trim();

        if (nouveauNom.isEmpty() || nouvelEmail.isEmpty()) {
            showError("❌ Veuillez remplir tous les champs obligatoires");
            return;
        }

        // Vérifier l'email
        if (!isValidEmail(nouvelEmail)) {
            showError("❌ Format d'email invalide");
            return;
        }

        // Mettre à jour les informations de base
        currentUser.setNom(nouveauNom);
        currentUser.setEmail(nouvelEmail);

        // Changer le mot de passe si rempli
        String ancien = ancienMdpField.getText();
        String nouveau = nouveauMdpField.getText();
        String confirm = confirmMdpField.getText();

        if (!ancien.isEmpty() || !nouveau.isEmpty() || !confirm.isEmpty()) {
            if (ancien.isEmpty() || nouveau.isEmpty() || confirm.isEmpty()) {
                showError("❌ Veuillez remplir tous les champs du mot de passe");
                return;
            }

            if (!nouveau.equals(confirm)) {
                showError("❌ Les mots de passe ne correspondent pas");
                return;
            }

            if (nouveau.length() < 6) {
                showError("❌ Le mot de passe doit contenir au moins 6 caractères");
                return;
            }

            boolean mdpChange = userService.changerMotDePasse(currentUser.getId(), ancien, nouveau);
            if (!mdpChange) {
                showError("❌ Ancien mot de passe incorrect");
                return;
            }
        }

        // Enregistrer les modifications
        userService.modifier(currentUser);
        showSuccess("✅ Profil modifié avec succès !");

        // Rediriger vers la page de profil après 1 seconde
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                javafx.application.Platform.runLater(() -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/profilAdmin.fxml"));
                        Parent root = loader.load();

                        ProfilAdminController controller = loader.getController();
                        controller.setUser(currentUser);

                        Stage stage = (Stage) nomField.getScene().getWindow();
                        stage.setScene(new Scene(root));
                        stage.setTitle("AgriConnect - Mon Profil");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void handleAnnuler(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profilAdmin.fxml"));
            Parent root = loader.load();

            ProfilAdminController controller = loader.getController();
            controller.setUser(currentUser);

            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Mon Profil");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBackToDashboard(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/adminDashboard.fxml"));
            Parent root = loader.load();

            DashboardAdminController controller = loader.getController();
            controller.setUser(currentUser);

            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Dashboard Admin");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private void showError(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
        }
    }

    private void showSuccess(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
        }
    }
}