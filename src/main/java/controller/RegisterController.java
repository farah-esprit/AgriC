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
    @FXML private Label captchaStatusLabel;

    private UserService userService;
    private CaptchaService captchaService;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    private boolean captchaVerified = false;
    private String captchaToken = "";
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
        if (captchaContainer == null) return;

        try {
            CaptchaService.startServer();

            captchaWebView = new WebView();
            captchaWebView.setPrefHeight(78);
            captchaWebView.setPrefWidth(380);

            captchaContainer.getChildren().clear();
            captchaContainer.getChildren().add(captchaWebView);
            AnchorPane.setTopAnchor(captchaWebView, 1.0);
            AnchorPane.setBottomAnchor(captchaWebView, 1.0);
            AnchorPane.setLeftAnchor(captchaWebView, 1.0);
            AnchorPane.setRightAnchor(captchaWebView, 1.0);

            WebEngine engine = captchaWebView.getEngine();

            // ✅ Réinstaller le connecteur à CHAQUE changement d'état
            engine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
                System.out.println("🔄 State: " + newState);
                if (newState == Worker.State.SUCCEEDED) {
                    Platform.runLater(() -> {
                        try {
                            JSObject window = (JSObject) engine.executeScript("window");
                            JavaConnector connector = new JavaConnector();
                            window.setMember("javaConnector", connector);

                            // ✅ Vérifier immédiatement
                            Boolean exists = (Boolean) engine.executeScript(
                                    "typeof window.javaConnector !== 'undefined' && " +
                                            "typeof window.javaConnector.captchaVerified === 'function'"
                            );
                            System.out.println("✅ JavaConnector fonctionnel : " + exists);

                            // ✅ Re-rendre le CAPTCHA après installation du connecteur
                            engine.executeScript(
                                    "if (typeof grecaptcha !== 'undefined') {" +
                                            "  try { grecaptcha.reset(); console.log('♻️ CAPTCHA reset'); }" +
                                            "  catch(e) { console.log('ℹ️ Reset info: ' + e); }" +
                                            "}"
                            );

                        } catch (Exception e) {
                            System.err.println("❌ Erreur: " + e.getMessage());
                        }
                    });
                }
            });

            engine.load("http://localhost:8765/captcha");

            // ✅ Ajouter après engine.load("http://localhost:8765/captcha");
            javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(
                            javafx.util.Duration.seconds(1),
                            e -> {
                                try {
                                    Object result = engine.executeScript(
                                            "typeof grecaptcha !== 'undefined' ? grecaptcha.getResponse() : ''"
                                    );
                                    if (result != null && !result.toString().isEmpty()) {
                                        String token = result.toString();
                                        if (!captchaVerified && !token.isEmpty()) {
                                            System.out.println("✅ Token récupéré par polling : " + token.substring(0, 20));
                                            captchaToken = token;
                                            // Vérifier avec Google
                                            new Thread(() -> {
                                                boolean valid = captchaService.verifyToken(token);
                                                Platform.runLater(() -> {
                                                    if (valid) {
                                                        captchaVerified = true;
                                                        if (captchaStatusLabel != null) {
                                                            captchaStatusLabel.setText("✅ Vérification réussie !");
                                                            captchaStatusLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
                                                        }
                                                        System.out.println("✅ CAPTCHA validé par polling !");
                                                    }
                                                });
                                            }).start();
                                        }
                                    }
                                } catch (Exception ex) {
                                    // Silencieux
                                }
                            }
                    )
            );
            timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
            timeline.play();
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

        // ✅ Validation des champs
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

        // ✅ Vérifier CAPTCHA (commenté pour test)
        /*
        if (!captchaVerified) {
            ValidationUtils.showError(errorLabel, "❌ Veuillez compléter la vérification CAPTCHA !");
            return;
        }
        */

        try {
            // ✅ Créer utilisateur avec état INACTIF
            User newUser = new User(nom, email, password, role, EtatCompte.INACTIF);

            // ✅ Générer un code de vérification
            String code = String.valueOf((int)(Math.random() * 900000) + 100000); // 6 chiffres
            newUser.setVerificationCode(code);

            // ✅ Expiration du code (15 minutes)
            newUser.setCodeExpiration(new java.sql.Timestamp(System.currentTimeMillis() + 15 * 60 * 1000));

            // ✅ Ajouter l'utilisateur en base
            userService.ajouter(newUser);

            System.out.println("User ajouté avec mot de passe hashé");
            System.out.println("Code de vérification : " + code);

            // ✅ Envoyer email de vérification
            EmailService emailService = new EmailService();
            emailService.sendVerificationEmail(email, nom, code);
            System.out.println("Email de vérification envoyé à " + email);

            showSuccess(errorLabel, "Compte créé ! Vérifiez votre email.");

            // ✅ CHARGER VERIFICATION DANS LE STACKPANE
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/verification.fxml"));
            Parent root = loader.load();

            VerificationController controller = loader.getController();
            controller.setEmail(email);
            controller.setContentPane(contentPane); // 🔥 PASSER LE STACKPANE

            // ✅ Afficher dans le StackPane
            contentPane.getChildren().clear();
            contentPane.getChildren().add(root);

            AnchorPane.setTopAnchor(root, 0.0);
            AnchorPane.setBottomAnchor(root, 0.0);
            AnchorPane.setLeftAnchor(root, 0.0);
            AnchorPane.setRightAnchor(root, 0.0);

            System.out.println("Page de vérification chargée dans StackPane");

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
                        System.out.println("Retour au login dans StackPane");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}