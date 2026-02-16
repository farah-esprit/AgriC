package controller;

import entities.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import service.UserService;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class ProfilAdminController {

    @FXML private Label nomLabel;
    @FXML private Label emailLabel;
    @FXML private Label roleLabel;
    @FXML private Label etatLabel;
    @FXML private Label dateCreationLabel;
    @FXML private Label messageLabel;
    @FXML private Label themeModeIcon;
    @FXML private Label themeModeText;
    @FXML private AnchorPane sidebar;
    @FXML private ToggleButton themeToggle;
    private boolean isDarkMode = false;
    private User currentUser;
    private UserService userService;

    @FXML
    public void initialize() {
        userService = new UserService();
    }

    public void setUser(User user) {
        this.currentUser = user;

        if (nomLabel != null) nomLabel.setText(user.getNom());
        if (emailLabel != null) emailLabel.setText(user.getEmail());
        if (roleLabel != null) roleLabel.setText(user.getRole().toString());
        if (etatLabel != null) {
            etatLabel.setText(user.getEtatCompte().toString());
            if (user.getEtatCompte().toString().equals("ACTIF")) {
                etatLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
            } else {
                etatLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
            }
        }

        if (dateCreationLabel != null && user.getDateCreation() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
            dateCreationLabel.setText(user.getDateCreation().format(formatter));
        }
    }

    @FXML
    private void handleModifierProfil(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/editProfilAdmin.fxml"));
            Parent root = loader.load();

            EditProfilAdminController controller = loader.getController();
            controller.setUser(currentUser);

            Stage stage = (Stage) nomLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Modifier mon profil");

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors du chargement de la page de modification");
        }
    }

    @FXML
    private void handleSupprimerCompte(ActionEvent event) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation de suppression");
        confirmation.setHeaderText("Supprimer votre compte ?");
        confirmation.setContentText("⚠️ ATTENTION : Cette action est IRRÉVERSIBLE !\n\nÊtes-vous absolument sûr de vouloir supprimer votre compte ?");

        ButtonType buttonOui = new ButtonType("Oui, supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType buttonNon = new ButtonType("Non, annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmation.getButtonTypes().setAll(buttonOui, buttonNon);

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == buttonOui) {
            userService.supprimer(currentUser.getId());

            // Redirection vers login
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
                Stage stage = (Stage) nomLabel.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("AgriConnect - Connexion");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleBackToDashboard(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/adminDashboard.fxml"));
            Parent root = loader.load();

            DashboardAdminController controller = loader.getController();
            controller.setUser(currentUser);

            Stage stage = (Stage) nomLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Dashboard Admin");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void openManageUsers(MouseEvent event)
    {
        try {
            System.out.println("📂 Chargement de manageUsers.fxml...");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/managerUsers.fxml"));
            Parent root = loader.load();

            controller.ManageUsersController controller = loader.getController();
            controller.setUser(currentUser);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Utilisateurs");

            System.out.println("✅ liste des utilisateurs  chargée !");

        } catch (Exception e) {
            System.err.println("❌ ERREUR : " + e.getMessage());
            e.printStackTrace();
            showError("Impossible de charger les utilisateurs");
        }
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) nomLabel.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
        }
    }
    @FXML
    private void handleToggleTheme() {
        isDarkMode = !isDarkMode;

        if (isDarkMode) {
            sidebar.setStyle("-fx-background-color: #1a1a1a;");
            // Le contenu principal n'a pas de fx:id dans profilAdmin, on peut le skipper

            if (themeModeIcon != null) themeModeIcon.setText("🌙");
            if (themeModeText != null) themeModeText.setText("Sombre");
            if (themeToggle != null) {
                themeToggle.setText("☀️");
                themeToggle.setStyle("-fx-background-color: #424242; -fx-background-radius: 15; -fx-cursor: hand; -fx-font-size: 14px; -fx-text-fill: white;");
            }
        } else {
            sidebar.setStyle("-fx-background-color: #388e3c;");

            if (themeModeIcon != null) themeModeIcon.setText("☀️");
            if (themeModeText != null) themeModeText.setText("Clair");
            if (themeToggle != null) {
                themeToggle.setText("🌙");
                themeToggle.setStyle("-fx-background-color: #81c784; -fx-background-radius: 15; -fx-cursor: hand; -fx-font-size: 14px; -fx-text-fill: #388e3c;");
            }
        }
    }
}