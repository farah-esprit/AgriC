package controller;
import entities.User;
import entities.EtatCompte;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import service.UserService;
public class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;
    @FXML
    StackPane contentPane;
    private UserService userService;

    @FXML
    public void initialize() {
        userService = new UserService();
        if (errorLabel != null) errorLabel.setText("");

        Platform.runLater(() -> {
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setMaximized(true);
            Scene scene = stage.getScene();
            if (scene.getRoot() instanceof Region r) {
                r.prefWidthProperty().bind(scene.widthProperty());
                r.prefHeightProperty().bind(scene.heightProperty());
            }
        });
    }

    @FXML
    private void handleRegister() {
        try {
            // Charger le FXML du formulaire d'inscription
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/register.fxml"));
            Parent root = loader.load();

            // Récupérer le controller de Register pour passer le StackPane
            RegisterController controller = loader.getController();
            controller.setContentPane(this.contentPane); // ⚡ Passe le StackPane droit

            // Afficher le formulaire d'inscription dans le StackPane
            contentPane.getChildren().clear();
            contentPane.getChildren().add(root);

            // Ancrer pour occuper tout l'espace
            AnchorPane.setTopAnchor(root, 0.0);
            AnchorPane.setBottomAnchor(root, 0.0);
            AnchorPane.setLeftAnchor(root, 0.0);
            AnchorPane.setRightAnchor(root, 0.0);

        } catch (Exception e) {
            System.out.println("Erreur lors du chargement de l'inscription");
            e.printStackTrace();
            showError("Erreur lors du chargement de la page d'inscription");
        }
    }
    @FXML
    private void handleForgotPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/forgotPassword.fxml"));
            Parent root = loader.load();

            // Passer le StackPane contentPane au controller du ForgotPassword si besoin
            ForgotPasswordController controller = loader.getController();
            controller.setContentPane(contentPane); // ⚡ Assure-toi d'ajouter un setter dans ForgotPasswordController

            // Afficher dans le côté blanc
            contentPane.getChildren().clear();
            contentPane.getChildren().add(root);

            // Ancrer pour occuper tout l'espace
            AnchorPane.setTopAnchor(root, 0.0);
            AnchorPane.setBottomAnchor(root, 0.0);
            AnchorPane.setLeftAnchor(root, 0.0);
            AnchorPane.setRightAnchor(root, 0.0);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors du chargement de la page Mot de passe oublié");
        }
    }
    // ================= LOGIN AVEC VÉRIFICATION 2FA =================
    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("❌ Veuillez remplir tous les champs");
            return;
        }

        if (!isValidEmail(email)) {
            showError("❌ Format d'email invalide");
            return;
        }

        // Authentification
        User user = userService.authenticate(email, password);

        if (user == null) {
            showError("❌ Email ou mot de passe incorrect");
            return;
        }

        // Vérification de l'état du compte
        if (user.getEtatCompte() == EtatCompte.BLOQUE) {
            showError("❌ Votre compte est bloqué. Contactez l'administrateur.");
            return;
        }

        // ✅ VÉRIFIER SI 2FA EST ACTIVÉ
        boolean twoFactorEnabled = userService.is2FAEnabled(user.getId());

        System.out.println("🔐 2FA activé pour " + user.getEmail() + " : " + twoFactorEnabled);

        if (twoFactorEnabled) {
            // ✅ REDIRIGER VERS PAGE DE VÉRIFICATION 2FA
            System.out.println("🔐 Redirection vers vérification 2FA...");
            redirectToTwoFactorVerification(user);
        } else {
            // Connexion réussie sans 2FA
            showSuccess("✅ Connexion réussie ! Bienvenue " + user.getNom());
            redirectToDashboard(user);
        }
    }

    // ✅ NOUVELLE MÉTHODE : Redirection vers vérification 2FA
    private void redirectToTwoFactorVerification(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/verify2FA.fxml"));
            Parent root = loader.load();

            Verify2FAController controller = loader.getController();
            controller.setUser(user);
            controller.setContentPane(contentPane); // ⚡ Passe le StackPane droit

            // Afficher 2FA dans le StackPane droit
            contentPane.getChildren().clear();
            contentPane.getChildren().add(root);

            AnchorPane.setTopAnchor(root, 0.0);
            AnchorPane.setBottomAnchor(root, 0.0);
            AnchorPane.setLeftAnchor(root, 0.0);
            AnchorPane.setRightAnchor(root, 0.0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // ================= MESSAGES =================
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
    }

    private void showSuccess(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
    }

    // ================= VALIDATION EMAIL =================
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private void redirectToDashboard(User user) {
        try {
            String fxmlFile = "";

            switch (user.getRole()) {
                case AGRICULTEUR: fxmlFile = "/agriculteurDashboard.fxml"; break;
                case EXPERT:      fxmlFile = "/expertDashboard.fxml"; break;
                case FOURNISSEUR: fxmlFile = "/fournisseurDashboard.fxml"; break;
                case ADMIN:       fxmlFile = "/adminDashboard.fxml"; break;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof DashboardAdminController)
                ((DashboardAdminController) controller).setUser(user);
            else if (controller instanceof DashboardAgriculteurController)
                ((DashboardAgriculteurController) controller).setUser(user);
            else if (controller instanceof DashboardFournisseurController)
                ((DashboardFournisseurController) controller).setUser(user);
            else if (controller instanceof DashboardExpertController)
                ((DashboardExpertController) controller).setUser(user);

            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root);

            // ✅ LIER LES DIMENSIONS AVANT DE CHANGER LA SCENE
            if (root instanceof Region r) {
                r.prefWidthProperty().bind(scene.widthProperty());
                r.prefHeightProperty().bind(scene.heightProperty());
            }

            stage.setScene(scene);
            stage.setTitle("AgriConnect - Dashboard " + user.getRole());

            // ✅ MAXIMISER APRÈS
            javafx.application.Platform.runLater(() -> {
                stage.setMaximized(true);
            });

            System.out.println("✅ Redirection réussie vers " + fxmlFile);

        } catch (Exception e) {
            System.err.println("❌ ERREUR lors de la redirection :");
            e.printStackTrace();
            showError("❌ Erreur lors de la redirection : " + e.getMessage());
        }
    }
}