package controller;
import entities.User;
import entities.EtatCompte;
import entities.Role;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import service.GoogleAuthService;
import service.UserService;

public class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;
    @FXML StackPane contentPane;

    // ✅ NOUVEAUX ÉLÉMENTS POUR GOOGLE AUTH
    @FXML private VBox loginFormBox;           // Le formulaire de login normal
    @FXML private VBox googleRoleBox;          // Le formulaire de sélection de rôle
    @FXML private Label googleNameLabel;
    @FXML private Label googleEmailLabel;
    @FXML private ChoiceBox<Role> googleRoleChoice;
    @FXML private Label googleErrorLabel;

    private UserService userService;
    private GoogleAuthService googleAuthService;
    private GoogleAuthService.GoogleUserInfo currentGoogleUser; // ✅ Stocker l'utilisateur Google

    @FXML
    public void initialize() {
        userService = new UserService();
        googleAuthService = new GoogleAuthService();

        // ✅ Initialiser le ChoiceBox des rôles
        if (googleRoleChoice != null) {
            googleRoleChoice.getItems().addAll(Role.AGRICULTEUR, Role.EXPERT, Role.FOURNISSEUR);
            googleRoleChoice.setValue(Role.AGRICULTEUR);
        }

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

    // ✅ MÉTHODE : Authentification Google
    @FXML
    private void handleGoogleLogin() {
        try {
            showSuccess("🔄 Connexion avec Google en cours...");

            new Thread(() -> {
                GoogleAuthService.GoogleUserInfo googleUser = googleAuthService.authenticate();

                if (googleUser != null) {
                    Platform.runLater(() -> {
                        // Vérifier si l'utilisateur existe déjà
                        User existingUser = userService.findByEmail(googleUser.getEmail());

                        if (existingUser != null) {
                            // ✅ Utilisateur existe → Connexion directe
                            showSuccess("✅ Connexion réussie ! Bienvenue " + existingUser.getNom());
                            redirectToDashboard(existingUser);

                        } else {
                            // ❌ Utilisateur n'existe pas → 🔥 AFFICHER LE CHOICEBOX
                            currentGoogleUser = googleUser;
                            showGoogleRoleSelection(googleUser);
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        showError("❌ Authentification Google annulée ou échouée");
                    });
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            showError("❌ Erreur lors de l'authentification Google");
        }
    }

    // ✅ MÉTHODE : Afficher le formulaire de sélection de rôle
    private void showGoogleRoleSelection(GoogleAuthService.GoogleUserInfo googleUser) {
        // Cacher le formulaire de login
        loginFormBox.setVisible(false);
        loginFormBox.setManaged(false);

        // Afficher le formulaire de sélection de rôle
        googleRoleBox.setVisible(true);
        googleRoleBox.setManaged(true);

        // Remplir les informations
        googleNameLabel.setText(googleUser.getName());
        googleEmailLabel.setText(googleUser.getEmail());

        System.out.println("✅ Formulaire de sélection de rôle affiché");
    }

    // ✅ MÉTHODE : Continuer avec le rôle sélectionné
    @FXML
    private void handleGoogleRoleContinue() {
        Role selectedRole = googleRoleChoice.getValue();

        if (selectedRole == null) {
            showGoogleError("❌ Veuillez sélectionner un type de compte");
            return;
        }

        try {
            // Créer le nouvel utilisateur
            User newUser = new User();
            newUser.setNom(currentGoogleUser.getName());
            newUser.setEmail(currentGoogleUser.getEmail());
            newUser.setMotDePasse("GOOGLE_AUTH_" + currentGoogleUser.getGoogleId());
            newUser.setRole(selectedRole);
            newUser.setEtatCompte(EtatCompte.ACTIF);

            // Ajouter en base de données
            userService.ajouter(newUser);

            System.out.println("✅ Compte créé avec rôle : " + selectedRole);

            // Rediriger vers le dashboard
            redirectToDashboard(newUser);

        } catch (Exception e) {
            e.printStackTrace();
            showGoogleError("❌ Erreur lors de la création du compte");
        }
    }

    // ✅ MÉTHODE : Retour au formulaire de login
    @FXML
    private void handleBackToLoginForm() {
        // Afficher le formulaire de login
        loginFormBox.setVisible(true);
        loginFormBox.setManaged(true);

        // Cacher le formulaire de sélection de rôle
        googleRoleBox.setVisible(false);
        googleRoleBox.setManaged(false);

        // Réinitialiser
        currentGoogleUser = null;
        errorLabel.setText("");
        googleErrorLabel.setText("");

        System.out.println("✅ Retour au formulaire de login");
    }

    // ================= RESTE DU CODE (handleRegister, handleForgotPassword, etc.) =================

    @FXML
    private void handleRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/register.fxml"));
            Parent root = loader.load();

            RegisterController controller = loader.getController();
            controller.setContentPane(this.contentPane);

            contentPane.getChildren().clear();
            contentPane.getChildren().add(root);

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

            ForgotPasswordController controller = loader.getController();
            controller.setContentPane(contentPane);

            contentPane.getChildren().clear();
            contentPane.getChildren().add(root);

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

        User user = userService.authenticate(email, password);

        if (user == null) {
            showError("❌ Email ou mot de passe incorrect");
            return;
        }

        if (user.getEtatCompte() == EtatCompte.BLOQUE) {
            showError("❌ Votre compte est bloqué. Contactez l'administrateur.");
            return;
        }

        boolean twoFactorEnabled = userService.is2FAEnabled(user.getId());

        System.out.println("🔐 2FA activé pour " + user.getEmail() + " : " + twoFactorEnabled);

        if (twoFactorEnabled) {
            System.out.println("🔐 Redirection vers vérification 2FA...");
            redirectToTwoFactorVerification(user);
        } else {
            showSuccess("✅ Connexion réussie ! Bienvenue " + user.getNom());
            redirectToDashboard(user);
        }
    }

    private void redirectToTwoFactorVerification(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/verify2FA.fxml"));
            Parent root = loader.load();

            Verify2FAController controller = loader.getController();
            controller.setUser(user);
            controller.setContentPane(contentPane);

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

    private void showGoogleError(String message) {
        googleErrorLabel.setText(message);
        googleErrorLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
    }

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

            if (root instanceof Region r) {
                r.prefWidthProperty().bind(scene.widthProperty());
                r.prefHeightProperty().bind(scene.heightProperty());
            }

            stage.setScene(scene);
            stage.setTitle("AgriConnect - Dashboard " + user.getRole());

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