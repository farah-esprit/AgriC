package controller;
import entities.EtatCompte;
import entities.Role;
import entities.User;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import service.CaptchaService;
import service.EmailService;
import service.UserService;
import utils.ValidationUtils;
import static utils.ValidationUtils.showSuccess;

public class RegisterController {
    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordTextField;
    @FXML private ChoiceBox<Role> roleChoice;
    @FXML private Label errorLabel;
    @FXML private Label passwordStrengthLabel;
    @FXML private Button registerButton;
    @FXML private Button togglePasswordBtn;
    @FXML private Button toggleConfirmPasswordBtn;
    @FXML private AnchorPane captchaContainer;
    private WebView captchaWebView;
    @FXML private Label captchaStatusLabel;      // ✅ Nouveau

    private UserService userService;
    private CaptchaService captchaService;       // ✅ Nouveau
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    private boolean captchaVerified = false;     // ✅ Nouveau
    private String captchaToken = "";            // ✅ Nouveau
    private StackPane contentPane;

    public void setContentPane(StackPane contentPane) {
        this.contentPane = contentPane;
    }

    @FXML
    public void initialize() {
        userService = new UserService();
        captchaService = new CaptchaService();
        roleChoice.getItems().addAll(Role.AGRICULTEUR, Role.EXPERT, Role.FOURNISSEUR);
        roleChoice.setValue(Role.AGRICULTEUR);
        bindPasswordFields();
        setupRealTimeValidation();

        Platform.runLater(() -> {
            Stage stage = (Stage) roleChoice.getScene().getWindow();
            stage.setMaximized(true);
            Scene scene = stage.getScene();
            if (scene.getRoot() instanceof Region r) {
                r.prefWidthProperty().bind(scene.widthProperty());
                r.prefHeightProperty().bind(scene.heightProperty());
            }
            // ✅ Charger le CAPTCHA après initialisation
            setupCaptcha();
        });
    }
    private void setupCaptcha() {
        System.out.println("🔧 Initialisation CAPTCHA...");
        if (captchaContainer == null) {
            System.err.println("❌ captchaContainer est null !");
            return;
        }

        try {
            // ✅ Démarrer le serveur HTTP local
            CaptchaService.startServer();

            captchaWebView = new WebView();
            captchaWebView.setPrefHeight(78);
            captchaWebView.setMinHeight(78);
            captchaWebView.setMaxHeight(78);
            captchaWebView.setPrefWidth(380);

            captchaContainer.getChildren().clear();
            captchaContainer.getChildren().add(captchaWebView);

            AnchorPane.setTopAnchor(captchaWebView, 1.0);
            AnchorPane.setBottomAnchor(captchaWebView, 1.0);
            AnchorPane.setLeftAnchor(captchaWebView, 1.0);
            AnchorPane.setRightAnchor(captchaWebView, 1.0);

            WebEngine engine = captchaWebView.getEngine();

            // ✅ INSTALLER LE CONNECTEUR IMMÉDIATEMENT (avant le chargement)
            engine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
                System.out.println("🔄 WebView state: " + newState);

                if (newState == Worker.State.RUNNING) {
                    // ✅ Injecter JavaConnector dès que possible
                    try {
                        JSObject window = (JSObject) engine.executeScript("window");
                        window.setMember("javaConnector", new JavaConnector());
                        System.out.println("✅ JavaConnector pré-installé (RUNNING)");
                    } catch (Exception e) {
                        System.out.println("⚠️ Pas encore prêt pour RUNNING");
                    }
                }

                if (newState == Worker.State.SUCCEEDED) {
                    System.out.println("✅ WebView chargé avec succès");

                    // ✅ Installer/Réinstaller le connecteur pour être sûr
                    try {
                        JSObject window = (JSObject) engine.executeScript("window");
                        window.setMember("javaConnector", new JavaConnector());
                        System.out.println("✅ JavaConnector installé (SUCCEEDED)");

                        // ✅ Vérifier qu'il est accessible
                        Boolean exists = (Boolean) engine.executeScript(
                                "typeof window.javaConnector !== 'undefined'"
                        );
                        System.out.println("🔍 JavaConnector accessible: " + exists);

                        // ✅ Tester l'appel depuis JavaScript
                        engine.executeScript(
                                "console.log('🧪 Test: javaConnector =', window.javaConnector);"
                        );

                    } catch (Exception e) {
                        System.err.println("❌ Erreur installation connecteur: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else if (newState == Worker.State.FAILED) {
                    System.err.println("❌ Échec chargement WebView");
                    Throwable ex = engine.getLoadWorker().getException();
                    if (ex != null) {
                        ex.printStackTrace();
                    }
                }
            });

            // ✅ Capturer les logs JavaScript
            engine.setOnAlert(event -> {
                System.out.println("🔊 JS Alert: " + event.getData());
            });

            // ✅ CHARGER LA PAGE
            String captchaUrl = "http://localhost:8765/captcha";
            engine.load(captchaUrl);
            System.out.println("✅ Chargement depuis : " + captchaUrl);

        } catch (Exception e) {
            System.err.println("❌ Erreur setupCaptcha: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public class JavaConnector {
        public void captchaVerified(String token) {
            System.out.println("🎯🎯🎯 JavaConnector.captchaVerified() APPELÉ !");
            System.out.println("📥 Token reçu : " + token.substring(0, 30) + "...");

            Platform.runLater(() -> {
                captchaToken = token;
                System.out.println("✅ Token stocké dans captchaToken");

                // Vérifier le token avec Google
                new Thread(() -> {
                    System.out.println("🔄 Démarrage de la vérification avec Google...");
                    boolean valid = captchaService.verifyToken(token);

                    Platform.runLater(() -> {
                        if (valid) {
                            captchaVerified = true;
                            System.out.println("✅✅✅ CAPTCHA VÉRIFIÉ ET VALIDÉ !");

                            if (captchaStatusLabel != null) {
                                captchaStatusLabel.setText("✅ Vérification réussie !");
                                captchaStatusLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
                            }
                        } else {
                            captchaVerified = false;
                            System.out.println("❌❌❌ CAPTCHA INVALIDE !");

                            if (captchaStatusLabel != null) {
                                captchaStatusLabel.setText("❌ Vérification échouée");
                                captchaStatusLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
                            }
                        }
                    });
                }).start();
            });
        }
    }

    private void bindPasswordFields() {
        passwordField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.equals(passwordTextField.getText())) passwordTextField.setText(newVal);
        });
        passwordTextField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.equals(passwordField.getText())) passwordField.setText(newVal);
        });
        confirmPasswordField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.equals(confirmPasswordTextField.getText())) confirmPasswordTextField.setText(newVal);
        });
        confirmPasswordTextField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.equals(confirmPasswordField.getText())) confirmPasswordField.setText(newVal);
        });
    }

    @FXML
    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            passwordTextField.setVisible(true); passwordTextField.setManaged(true);
            passwordField.setVisible(false); passwordField.setManaged(false);
            togglePasswordBtn.setText("🙈");
        } else {
            passwordField.setVisible(true); passwordField.setManaged(true);
            passwordTextField.setVisible(false); passwordTextField.setManaged(false);
            togglePasswordBtn.setText("👁️");
        }
    }

    @FXML
    private void toggleConfirmPasswordVisibility() {
        isConfirmPasswordVisible = !isConfirmPasswordVisible;
        if (isConfirmPasswordVisible) {
            confirmPasswordTextField.setVisible(true); confirmPasswordTextField.setManaged(true);
            confirmPasswordField.setVisible(false); confirmPasswordField.setManaged(false);
            toggleConfirmPasswordBtn.setText("🙈");
        } else {
            confirmPasswordField.setVisible(true); confirmPasswordField.setManaged(true);
            confirmPasswordTextField.setVisible(false); confirmPasswordTextField.setManaged(false);
            toggleConfirmPasswordBtn.setText("👁️");
        }
    }

    private void setupRealTimeValidation() {
        nomField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.trim().isEmpty()) {
                if (ValidationUtils.isValidName(newVal)) ValidationUtils.setFieldSuccess(nomField);
                else ValidationUtils.setFieldError(nomField);
            } else ValidationUtils.resetFieldStyle(nomField);
        });
        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.trim().isEmpty()) {
                if (ValidationUtils.isValidEmail(newVal)) ValidationUtils.setFieldSuccess(emailField);
                else ValidationUtils.setFieldError(emailField);
            } else ValidationUtils.resetFieldStyle(emailField);
        });
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> validatePassword(newVal));
        passwordTextField.textProperty().addListener((obs, oldVal, newVal) -> validatePassword(newVal));
        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> validateConfirmPassword(newVal));
        confirmPasswordTextField.textProperty().addListener((obs, oldVal, newVal) -> validateConfirmPassword(newVal));
    }

    private void validatePassword(String password) {
        if (!password.isEmpty()) {
            if (ValidationUtils.isStrongPassword(password)) {
                ValidationUtils.setFieldSuccess(passwordField);
                ValidationUtils.setFieldSuccess(passwordTextField);
                showPasswordStrength("✅ Mot de passe fort", "#4caf50");
            } else if (ValidationUtils.isValidPassword(password)) {
                ValidationUtils.setFieldError(passwordField);
                ValidationUtils.setFieldError(passwordTextField);
                showPasswordStrength("⚠️ Mot de passe faible", "#ff9800");
            } else {
                ValidationUtils.setFieldError(passwordField);
                ValidationUtils.setFieldError(passwordTextField);
                showPasswordStrength("❌ Minimum 8 caractères", "#d32f2f");
            }
        } else {
            ValidationUtils.resetFieldStyle(passwordField);
            ValidationUtils.resetFieldStyle(passwordTextField);
            hidePasswordStrength();
        }
    }

    private void validateConfirmPassword(String confirmPassword) {
        String password = passwordField.getText();
        if (!confirmPassword.isEmpty()) {
            if (confirmPassword.equals(password)) {
                ValidationUtils.setFieldSuccess(confirmPasswordField);
                ValidationUtils.setFieldSuccess(confirmPasswordTextField);
            } else {
                ValidationUtils.setFieldError(confirmPasswordField);
                ValidationUtils.setFieldError(confirmPasswordTextField);
            }
        } else {
            ValidationUtils.resetFieldStyle(confirmPasswordField);
            ValidationUtils.resetFieldStyle(confirmPasswordTextField);
        }
    }

    private void showPasswordStrength(String message, String color) {
        if (passwordStrengthLabel != null) {
            passwordStrengthLabel.setText(message);
            passwordStrengthLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-weight: bold;");
            passwordStrengthLabel.setVisible(true);
        }
    }

    private void hidePasswordStrength() {
        if (passwordStrengthLabel != null) passwordStrengthLabel.setVisible(false);
    }

    @FXML
    private void handleRegister() {
        String nom = ValidationUtils.sanitize(nomField.getText());
        String email = ValidationUtils.sanitize(emailField.getText());
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        Role role = roleChoice.getValue();

        if (!ValidationUtils.isNotEmpty(nom) || !ValidationUtils.isNotEmpty(email) ||
                !ValidationUtils.isNotEmpty(password) || !ValidationUtils.isNotEmpty(confirmPassword)) {
            ValidationUtils.showError(errorLabel, "Veuillez remplir tous les champs");
            return;
        }
        if (!ValidationUtils.isValidName(nom)) {
            ValidationUtils.showError(errorLabel, "Nom invalide");
            ValidationUtils.setFieldError(nomField);
            return;
        }
        if (!ValidationUtils.isValidEmail(email)) {
            ValidationUtils.showError(errorLabel, "Format d'email invalide");
            ValidationUtils.setFieldError(emailField);
            return;
        }
        if (userService.emailExiste(email)) {
            ValidationUtils.showError(errorLabel, "Cet email est déjà utilisé");
            ValidationUtils.setFieldError(emailField);
            return;
        }
        if (!ValidationUtils.isValidPassword(password)) {
            ValidationUtils.showError(errorLabel, "Minimum 8 caractères");
            ValidationUtils.setFieldError(passwordField);
            return;
        }
        if (!password.equals(confirmPassword)) {
            ValidationUtils.showError(errorLabel, "Les mots de passe ne correspondent pas");
            ValidationUtils.setFieldError(confirmPasswordField);
            return;
        }

        // ✅ Vérifier CAPTCHA
        if (!captchaVerified) {
            ValidationUtils.showError(errorLabel, "❌ Veuillez compléter la vérification CAPTCHA !");
            return;
        }

        try {
            User newUser = new User(nom, email, password, role, EtatCompte.ACTIF);
            userService.ajouter(newUser);
            EmailService emailService = new EmailService();
            emailService.sendWelcomeEmail(email, nom);
            showSuccess(errorLabel, "✅ Compte créé ! Consultez votre email.");
            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    Platform.runLater(this::handleBackToLogin);
                } catch (InterruptedException e) { e.printStackTrace(); }
            }).start();
        } catch (Exception e) {
            ValidationUtils.showError(errorLabel, "Erreur lors de la création du compte");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();
            LoginController controller = loader.getController();
            if (root instanceof javafx.scene.layout.HBox hbox) {
                if (hbox.getChildren().size() >= 2) {
                    javafx.scene.layout.StackPane whitePane =
                            (javafx.scene.layout.StackPane) hbox.getChildren().get(1);
                    if (whitePane.getChildren().size() > 0) {
                        javafx.scene.Node loginForm = whitePane.getChildren().get(0);
                        whitePane.getChildren().remove(loginForm);
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(loginForm);
                        controller.contentPane = this.contentPane;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}