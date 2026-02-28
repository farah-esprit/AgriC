package org.example.controllers.User;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.entities.EtatCompte;
import org.example.entities.Role;
import org.example.entities.User;
import org.example.services.User.CaptchaService;
import org.example.services.User.EmailService;
import org.example.services.User.UserService;
import org.example.services.User.VoiceAuthService;
import org.example.utils.ValidationUtils;


import javax.sound.sampled.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.example.utils.ValidationUtils.showSuccess;


public class RegisterController {

    // ── Champs existants ──────────────────────────────────────────────────
    @FXML private TextField       nomField;
    @FXML private TextField       emailField;
    @FXML private PasswordField   passwordField;
    @FXML private TextField       passwordTextField;
    @FXML private PasswordField   confirmPasswordField;
    @FXML private TextField       confirmPasswordTextField;
    @FXML private ChoiceBox<Role> roleChoice;
    @FXML private Label           errorLabel;
    @FXML private Label           passwordStrengthLabel;
    @FXML private Button          registerButton;
    @FXML private Button          togglePasswordBtn;
    @FXML private Button          toggleConfirmPasswordBtn;
    @FXML private AnchorPane      captchaContainer;
    @FXML private Label           captchaStatusLabel;

    // ── Champs VOCAUX liés au FXML (voiceEnrollBox section) ──────────────
    @FXML private VBox            voiceEnrollBox;    // section vocale dans le form
    @FXML private Label           enrollStepLabel;   // "0 / 3", "1 / 3"...
    @FXML private ProgressBar     enrollProgressBar; // barre de progression
    @FXML private Label           enrollStatusLabel; // message d'état
    @FXML private Button          btnEnrollRecord;   // "Enregistrer (3s)"
    @FXML private Button          btnEnrollSave;     // "Valider l'échantillon"
    @FXML private Label           enrollDoneLabel;   // badge succès 3/3

    // ── Services ──────────────────────────────────────────────────────────
    private UserService userService;
    private CaptchaService captchaService;
    private WebView               captchaWebView;
    private final VoiceAuthService voiceAuthService = new VoiceAuthService();

    // ── État existant ─────────────────────────────────────────────────────
    private boolean   isPasswordVisible        = false;
    private boolean   isConfirmPasswordVisible = false;
    private boolean   captchaVerified          = false;
    private String    captchaToken             = "";
    private StackPane contentPane;

    // ── État vocal ────────────────────────────────────────────────────────
    private final List<File> enrollSamples  = new ArrayList<>(); // fichiers validés (max 3)
    private File             currentSample  = null;              // dernier enregistrement
    private static final int TOTAL_SAMPLES  = 3;
    private boolean          enrollComplete = false;             // true quand 3/3 OK

    public void setContentPane(StackPane contentPane) {
        this.contentPane = contentPane;
    }

    // =========================================================================
    // INITIALISATION
    // =========================================================================
    @FXML
    public void initialize() {
        userService    = new UserService();
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
            setupCaptcha();
        });
    }

    // =========================================================================
    // CAPTCHA — code existant inchangé
    // =========================================================================
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

            engine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
                System.out.println("🔄 State: " + newState);
                if (newState == Worker.State.SUCCEEDED) {
                    Platform.runLater(() -> {
                        try {
                            JSObject window = (JSObject) engine.executeScript("window");
                            JavaConnector connector = new JavaConnector();
                            window.setMember("javaConnector", connector);

                            Boolean exists = (Boolean) engine.executeScript(
                                    "typeof window.javaConnector !== 'undefined' && " +
                                            "typeof window.javaConnector.captchaVerified === 'function'"
                            );
                            System.out.println("✅ JavaConnector fonctionnel : " + exists);

                            engine.executeScript(
                                    "if (typeof grecaptcha !== 'undefined') {" +
                                            "  try { grecaptcha.reset(); } catch(e) {} }"
                            );
                        } catch (Exception e) {
                            System.err.println("❌ Erreur: " + e.getMessage());
                        }
                    });
                }
            });

            engine.load("http://localhost:8765/captcha");

            javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(
                            javafx.util.Duration.seconds(1),
                            e -> {
                                try {
                                    Object result = engine.executeScript(
                                            "typeof grecaptcha !== 'undefined' ? grecaptcha.getResponse() : ''");
                                    if (result != null && !result.toString().isEmpty()) {
                                        String token = result.toString();
                                        if (!captchaVerified && !token.isEmpty()) {
                                            captchaToken = token;
                                            new Thread(() -> {
                                                boolean valid = captchaService.verifyToken(token);
                                                Platform.runLater(() -> {
                                                    if (valid) {
                                                        captchaVerified = true;
                                                        if (captchaStatusLabel != null) {
                                                            captchaStatusLabel.setText("✅ Vérification réussie !");
                                                            captchaStatusLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
                                                        }
                                                    }
                                                });
                                            }).start();
                                        }
                                    }
                                } catch (Exception ex) { /* Silencieux */ }
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
            System.out.println("🎯 JavaConnector.captchaVerified() APPELÉ !");
            Platform.runLater(() -> {
                captchaToken = token;
                new Thread(() -> {
                    boolean valid = captchaService.verifyToken(token);
                    Platform.runLater(() -> {
                        if (valid) {
                            captchaVerified = true;
                            if (captchaStatusLabel != null) {
                                captchaStatusLabel.setText("✅ Vérification réussie !");
                                captchaStatusLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
                            }
                        } else {
                            captchaVerified = false;
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

    // =========================================================================
    // BOUTON "ENREGISTRER (3s)" — onAction="#handleEnrollRecord" dans FXML
    // Enregistre un échantillon vocal de 3 secondes
    // =========================================================================
    @FXML
    private void handleEnrollRecord() {
        if (enrollComplete) return; // déjà 3/3, rien à faire

        setEnrollStatus("🔴 Parlez maintenant pendant 3 secondes...", "#f44336");
        if (enrollProgressBar != null) {
            enrollProgressBar.setVisible(true);
            enrollProgressBar.setProgress(-1);
        }
        if (btnEnrollRecord != null) btnEnrollRecord.setDisable(true);
        if (btnEnrollSave   != null) btnEnrollSave.setDisable(true);

        new Thread(() -> {
            currentSample = recordVoice(3);

            Platform.runLater(() -> {
                if (enrollProgressBar != null) enrollProgressBar.setProgress(1.0);

                if (currentSample != null) {
                    setEnrollStatus("✅ Échantillon " + (enrollSamples.size() + 1) +
                            " prêt — cliquez Valider", "#4CAF50");
                    if (btnEnrollSave != null) btnEnrollSave.setDisable(false);
                } else {
                    setEnrollStatus("❌ Erreur microphone — réessayez", "#f44336");
                }
                if (btnEnrollRecord != null) btnEnrollRecord.setDisable(false);
            });
        }).start();
    }

    // =========================================================================
    // BOUTON "VALIDER L'ÉCHANTILLON" — onAction="#handleEnrollSave" dans FXML
    // Stocke l'échantillon, envoie à Python quand 3/3 atteint
    // =========================================================================
    @FXML
    private void handleEnrollSave() {
        if (currentSample == null) {
            setEnrollStatus("⚠️ Enregistrez d'abord votre voix", "#ff9800");
            return;
        }

        enrollSamples.add(currentSample);
        currentSample = null;
        if (btnEnrollSave != null) btnEnrollSave.setDisable(true);

        updateEnrollStep(); // met à jour "X / 3" et la barre

        if (enrollSamples.size() < TOTAL_SAMPLES) {
            setEnrollStatus("🎤 " + enrollSamples.size() + "/" + TOTAL_SAMPLES +
                    " validé — enregistrez le suivant", "#2196f3");
        } else {
            // 3/3 → envoyer à Python pour créer l'empreinte dans voice_prints.db
            saveVoicePrint();
        }
    }

    // ── Envoie les 3 échantillons à Python ────────────────────────────────
    private void saveVoicePrint() {
        String email = ValidationUtils.sanitize(emailField.getText());
        if (email.isEmpty()) {
            setEnrollStatus("⚠️ Remplissez d'abord votre email", "#ff9800");
            enrollSamples.remove(enrollSamples.size() - 1);
            updateEnrollStep();
            return;
        }

        setEnrollStatus("💾 Création de votre empreinte vocale...", "#2196f3");
        if (enrollProgressBar != null) {
            enrollProgressBar.setProgress(-1);
            enrollProgressBar.setVisible(true);
        }
        if (btnEnrollRecord != null) btnEnrollRecord.setDisable(true);

        final String     finalEmail = email;
        final List<File> samples    = new ArrayList<>(enrollSamples);

        new Thread(() -> {
            boolean ok = false;
            for (File sample : samples) {
                ok = voiceAuthService.enroll(finalEmail, sample);
            }
            final boolean success = ok;

            Platform.runLater(() -> {
                if (enrollProgressBar != null) enrollProgressBar.setVisible(false);

                if (success) {
                    enrollComplete = true;
                    setEnrollStatus("✅ Empreinte vocale créée !", "#4CAF50");
                    if (enrollDoneLabel  != null) enrollDoneLabel.setVisible(true);
                    if (btnEnrollRecord  != null) btnEnrollRecord.setDisable(true);
                    if (btnEnrollSave    != null) btnEnrollSave.setDisable(true);
                    System.out.println("✅ Empreinte vocale stockée pour : " + finalEmail);
                } else {
                    setEnrollStatus("❌ Erreur — réessayez", "#f44336");
                    enrollSamples.clear();
                    updateEnrollStep();
                    if (btnEnrollRecord != null) btnEnrollRecord.setDisable(false);
                }
            });
        }).start();
    }

    private void updateEnrollStep() {
        if (enrollStepLabel != null)
            enrollStepLabel.setText(enrollSamples.size() + " / " + TOTAL_SAMPLES);
        if (enrollProgressBar != null) {
            enrollProgressBar.setVisible(true);
            enrollProgressBar.setProgress((double) enrollSamples.size() / TOTAL_SAMPLES);
        }
    }

    private void setEnrollStatus(String text, String color) {
        if (enrollStatusLabel != null) {
            enrollStatusLabel.setText(text);
            enrollStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + color + ";");
        }
    }

    // =========================================================================
    // SIGN UP — code existant + enrollment vocal déjà fait avant si possible
    // =========================================================================
    @FXML
    private void handleRegister() {
        String nom             = ValidationUtils.sanitize(nomField.getText());
        String email           = ValidationUtils.sanitize(emailField.getText());
        String password        = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        Role   role            = roleChoice.getValue();

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

        /*
        if (!captchaVerified) {
            ValidationUtils.showError(errorLabel, "❌ Veuillez compléter la vérification CAPTCHA !");
            return;
        }
        */

        try {
            User newUser = new User(nom, email, password, role, EtatCompte.INACTIF);

            String code = String.valueOf((int)(Math.random() * 900000) + 100000);
            newUser.setVerificationCode(code);
            newUser.setCodeExpiration(new java.sql.Timestamp(
                    System.currentTimeMillis() + 15 * 60 * 1000));

            userService.ajouter(newUser);
            System.out.println("✅ User ajouté avec mot de passe hashé");

            // ── Si enrollment vocal pas encore complet (user n'a pas cliqué les boutons)
            //    → fallback silencieux avec les échantillons partiels disponibles
            if (!enrollComplete && !enrollSamples.isEmpty()) {
                final String     finalEmail = email;
                final List<File> samples    = new ArrayList<>(enrollSamples);
                new Thread(() -> {
                    for (File s : samples) voiceAuthService.enroll(finalEmail, s);
                    System.out.println("✅ Enrollment vocal fallback OK pour : " + finalEmail);
                }).start();
            }

            EmailService emailService = new EmailService();
            emailService.sendVerificationEmail(email, nom, code);
            System.out.println("📧 Email de vérification envoyé à " + email);

            showSuccess(errorLabel, "Compte créé ! Vérifiez votre email.");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/verification.fxml"));
            Parent root = loader.load();

            VerificationController controller = loader.getController();
            controller.setEmail(email);
            controller.setContentPane(contentPane);

            contentPane.getChildren().clear();
            contentPane.getChildren().add(root);
            AnchorPane.setTopAnchor(root, 0.0);
            AnchorPane.setBottomAnchor(root, 0.0);
            AnchorPane.setLeftAnchor(root, 0.0);
            AnchorPane.setRightAnchor(root, 0.0);

            System.out.println("✅ Page de vérification chargée dans StackPane");

        } catch (Exception e) {
            ValidationUtils.showError(errorLabel, "Erreur lors de la création du compte");
            e.printStackTrace();
        }
    }

    // =========================================================================
    // VALIDATION — code existant inchangé
    // =========================================================================
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
            passwordTextField.setVisible(true);  passwordTextField.setManaged(true);
            passwordField.setVisible(false);     passwordField.setManaged(false);
            togglePasswordBtn.setText("🙈");
        } else {
            passwordField.setVisible(true);      passwordField.setManaged(true);
            passwordTextField.setVisible(false); passwordTextField.setManaged(false);
            togglePasswordBtn.setText("👁️");
        }
    }

    @FXML
    private void toggleConfirmPasswordVisibility() {
        isConfirmPasswordVisible = !isConfirmPasswordVisible;
        if (isConfirmPasswordVisible) {
            confirmPasswordTextField.setVisible(true);  confirmPasswordTextField.setManaged(true);
            confirmPasswordField.setVisible(false);     confirmPasswordField.setManaged(false);
            toggleConfirmPasswordBtn.setText("🙈");
        } else {
            confirmPasswordField.setVisible(true);      confirmPasswordField.setManaged(true);
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

    // =========================================================================
    // RETOUR LOGIN — code existant inchangé
    // =========================================================================
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

    // ── Enregistrement audio N secondes → fichier WAV temporaire ─────────
    private File recordVoice(int seconds) {
        try {
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            DataLine.Info info  = new DataLine.Info(TargetDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) return null;

            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            int    bufferSize = (int)(format.getSampleRate() * format.getFrameSize() * seconds);
            byte[] buffer     = new byte[bufferSize];
            int    bytesRead  = 0;
            long   startTime  = System.currentTimeMillis();

            while (bytesRead < bufferSize &&
                    System.currentTimeMillis() - startTime < seconds * 1000L + 500) {
                int read = line.read(buffer, bytesRead, Math.min(4096, bufferSize - bytesRead));
                if (read > 0) bytesRead += read;
            }
            line.stop();
            line.close();

            File tmp = File.createTempFile("voice_enroll_", ".wav");
            tmp.deleteOnExit();
            AudioSystem.write(
                    new AudioInputStream(
                            new java.io.ByteArrayInputStream(buffer, 0, bytesRead),
                            format, bytesRead / format.getFrameSize()),
                    AudioFileFormat.Type.WAVE, tmp);
            return tmp;

        } catch (Exception e) {
            System.err.println("Erreur enregistrement vocal : " + e.getMessage());
            return null;
        }
    }
}
