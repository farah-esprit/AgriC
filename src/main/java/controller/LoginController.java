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
import service.VoiceAuthService;

import javax.sound.sampled.*;
import java.io.File;

public class LoginController {

    // ── Champs existants ──────────────────────────────────────────────────
    @FXML private TextField       emailField;
    @FXML private PasswordField   passwordField;
    @FXML private Button          loginButton;
    @FXML private Label           errorLabel;
    @FXML        StackPane        contentPane;

    // ── Google Auth ───────────────────────────────────────────────────────
    @FXML private VBox            loginFormBox;
    @FXML private VBox            googleRoleBox;
    @FXML private Label           googleNameLabel;
    @FXML private Label           googleEmailLabel;
    @FXML private ChoiceBox<Role> googleRoleChoice;
    @FXML private Label           googleErrorLabel;

    // ── Champs VOCAUX pour le formulaire LOGIN ────────────────────────────
    @FXML private VBox            voiceBox;
    @FXML private ProgressBar     voiceProgressBar;
    @FXML private Label           voiceStatusLabel;
    @FXML private Button          btnVoiceRecord;
    @FXML private Button          btnVoiceVerify;
    @FXML private Label           voiceSimilarityLabel;

    // ── Champs VOCAUX pour le formulaire GOOGLE ROLE ──────────────────────
    @FXML private VBox            voiceBoxGoogle;
    @FXML private ProgressBar     voiceProgressBarGoogle;
    @FXML private Label           voiceStatusLabelGoogle;
    @FXML private Button          btnVoiceRecordGoogle;
    @FXML private Button          btnVoiceVerifyGoogle;
    @FXML private Label           voiceSimilarityLabelGoogle;

    // ── Services ──────────────────────────────────────────────────────────
    private UserService           userService;
    private GoogleAuthService     googleAuthService;
    private GoogleAuthService.GoogleUserInfo currentGoogleUser; // null si utilisateur existant
    private final VoiceAuthService voiceAuthService = new VoiceAuthService();

    // ── État interne vocal ─────────────────────────────────────────────────
    private User    pendingUser;
    private File    voiceRecordedFile;
    private boolean isGoogleFlow = false;

    // =========================================================================
    // INITIALISATION
    // =========================================================================
    @FXML
    public void initialize() {
        userService       = new UserService();
        googleAuthService = new GoogleAuthService();

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

    // =========================================================================
    // LOGIN PRINCIPAL
    // =========================================================================
    @FXML
    private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs");
            return;
        }
        if (!isValidEmail(email)) {
            showError("Format d'email invalide");
            return;
        }

        User user = userService.authenticate(email, password);
        if (user == null) {
            showError("Email ou mot de passe incorrect");
            return;
        }
        if (user.getEtatCompte() == EtatCompte.BLOQUE) {
            showError("Votre compte est bloqué. Contactez l'administrateur.");
            return;
        }

        // ✅ Connexion classique directe — vocal est optionnel
        // La vérification vocale se fait SEULEMENT via le bouton "PARLER POUR SE CONNECTER"
        finishLogin(user);
    }
    // =========================================================================
    // AFFICHER LA SECTION VOCALE (LOGIN)
    // =========================================================================
    private void showVoiceSection() {
        if (voiceBox != null) {
            voiceBox.setVisible(true);
            voiceBox.setManaged(true);
        }
        if (loginButton != null) loginButton.setDisable(true);
        setVoiceStatus("🎤 Cliquez Enregistrer et parlez 3 secondes", "#2196f3", false);
        showSuccess("🎤 Vérification vocale requise — parlez dans le micro");
    }

    // =========================================================================
    // AFFICHER LA SECTION VOCALE (GOOGLE)
    // =========================================================================
    private void showVoiceSectionGoogle() {
        if (voiceBoxGoogle != null) {
            voiceBoxGoogle.setVisible(true);
            voiceBoxGoogle.setManaged(true);
        }
        setVoiceStatus("🎤 Cliquez Enregistrer et parlez 3 secondes", "#2196f3", true);
        showGoogleError("🎤 Vérification vocale requise pour finaliser l'inscription");
    }

    // =========================================================================
    // BOUTON "ENREGISTRER (3s)"
    // =========================================================================
    @FXML
    private void handleVoiceRecord() {
        boolean isGoogle = isGoogleFlow;

        setVoiceStatus("🔴 Enregistrement... parlez maintenant !", "#f44336", isGoogle);

        ProgressBar bar       = isGoogle ? voiceProgressBarGoogle : voiceProgressBar;
        Button      recordBtn = isGoogle ? btnVoiceRecordGoogle   : btnVoiceRecord;
        Button      verifyBtn = isGoogle ? btnVoiceVerifyGoogle   : btnVoiceVerify;
        Label       simLabel  = isGoogle ? voiceSimilarityLabelGoogle : voiceSimilarityLabel;

        if (bar       != null) { bar.setVisible(true); bar.setProgress(-1); }
        if (recordBtn != null) recordBtn.setDisable(true);
        if (verifyBtn != null) verifyBtn.setDisable(true);
        if (simLabel  != null) simLabel.setVisible(false);

        new Thread(() -> {
            voiceRecordedFile = recordVoice(3);
            Platform.runLater(() -> {
                if (bar != null) bar.setProgress(1.0);
                if (voiceRecordedFile != null) {
                    setVoiceStatus("✅ Enregistrement terminé — cliquez Vérifier", "#4CAF50", isGoogle);
                    if (verifyBtn != null) verifyBtn.setDisable(false);
                } else {
                    setVoiceStatus("❌ Erreur microphone — réessayez", "#f44336", isGoogle);
                }
                if (recordBtn != null) recordBtn.setDisable(false);
            });
        }).start();
    }

    // =========================================================================
    // BOUTON "VÉRIFIER"
    // =========================================================================
    @FXML
    private void handleVoiceVerify() {
        if (voiceRecordedFile == null || pendingUser == null) {
            setVoiceStatus("⚠️ Enregistrez d'abord votre voix", "#ff9800", isGoogleFlow);
            return;
        }

        boolean isGoogle = isGoogleFlow;

        setVoiceStatus("🔍 Vérification en cours...", "#2196f3", isGoogle);

        ProgressBar bar       = isGoogle ? voiceProgressBarGoogle : voiceProgressBar;
        Button      recordBtn = isGoogle ? btnVoiceRecordGoogle   : btnVoiceRecord;
        Button      verifyBtn = isGoogle ? btnVoiceVerifyGoogle   : btnVoiceVerify;
        Label       simLabel  = isGoogle ? voiceSimilarityLabelGoogle : voiceSimilarityLabel;

        if (bar       != null) { bar.setVisible(true); bar.setProgress(-1); }
        if (verifyBtn != null) verifyBtn.setDisable(true);
        if (recordBtn != null) recordBtn.setDisable(true);

        final User userToVerify = pendingUser;
        final File audioFile    = voiceRecordedFile;

        new Thread(() -> {
            boolean ok = voiceAuthService.verify(userToVerify.getEmail(), audioFile);
            Platform.runLater(() -> {
                if (bar != null) bar.setVisible(false);

                if (ok) {
                    setVoiceStatus("✅ Identité confirmée !", "#4CAF50", isGoogle);
                    if (simLabel != null) {
                        simLabel.setText("✅ Voix reconnue avec succès");
                        simLabel.setVisible(true);
                    }
                    javafx.animation.PauseTransition pause =
                            new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1));
                    pause.setOnFinished(e -> finishLogin(userToVerify));
                    pause.play();
                } else {
                    setVoiceStatus("❌ Voix non reconnue — réessayez", "#f44336", isGoogle);
                    voiceRecordedFile = null;
                    if (recordBtn != null) recordBtn.setDisable(false);
                    if (verifyBtn != null) verifyBtn.setDisable(true);
                }
            });
        }).start();
    }

    // =========================================================================
    // HELPER : status vocal
    // =========================================================================
    private void setVoiceStatus(String text, String color, boolean isGoogle) {
        Label statusLabel = isGoogle ? voiceStatusLabelGoogle : voiceStatusLabel;
        if (statusLabel != null) {
            statusLabel.setText(text);
            statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + color + ";");
        }
    }

    // =========================================================================
    // APRÈS VALIDATION VOCALE → 2FA ou Dashboard
    // =========================================================================
    private void finishLogin(User user) {
        if (loginButton != null) loginButton.setDisable(false);
        boolean twoFactorEnabled = userService.is2FAEnabled(user.getId());
        System.out.println("2FA activé pour " + user.getEmail() + " : " + twoFactorEnabled);

        if (twoFactorEnabled) {
            redirectToTwoFactorVerification(user);
        } else {
            showSuccess("Connexion réussie ! Bienvenue " + user.getNom());
            redirectToDashboard(user);
        }
    }

    // =========================================================================
    // GOOGLE AUTH
    // =========================================================================
    @FXML
    private void handleGoogleLogin() {
        try {
            showSuccess("🌐 Connexion avec Google en cours...");

            new Thread(() -> {
                GoogleAuthService.GoogleUserInfo googleUser = googleAuthService.authenticate();

                if (googleUser != null) {
                    Platform.runLater(() -> {
                        User existingUser = userService.findByEmail(googleUser.getEmail());

                        if (existingUser != null) {
                            System.out.println("✅ Utilisateur Google existant : " + existingUser.getEmail());

                            if (voiceAuthService.hasVoicePrint(existingUser.getEmail())) {
                                // Utilisateur existant avec empreinte vocale
                                // currentGoogleUser reste null — pas besoin de créer un compte
                                pendingUser      = existingUser;
                                isGoogleFlow     = true;
                                currentGoogleUser = null; // explicitement null : compte déjà créé
                                showGoogleRoleSelection(googleUser);
                                showVoiceSectionGoogle();
                                return;
                            }

                            // Utilisateur existant sans empreinte vocale
                            boolean twoFactorEnabled = userService.is2FAEnabled(existingUser.getId());
                            if (twoFactorEnabled) {
                                redirectToTwoFactorVerification(existingUser);
                            } else {
                                showSuccess("Connexion réussie ! Bienvenue " + existingUser.getNom());
                                redirectToDashboard(existingUser);
                            }

                        } else {
                            // Nouvel utilisateur Google → afficher sélection de rôle
                            currentGoogleUser = googleUser;
                            isGoogleFlow      = true;
                            showGoogleRoleSelection(googleUser);
                        }
                    });
                } else {
                    Platform.runLater(() -> showError("Authentification Google annulée ou échouée"));
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors de l'authentification Google");
        }
    }

    private void showGoogleRoleSelection(GoogleAuthService.GoogleUserInfo googleUser) {
        loginFormBox.setVisible(false);
        loginFormBox.setManaged(false);
        googleRoleBox.setVisible(true);
        googleRoleBox.setManaged(true);
        googleNameLabel.setText(googleUser.getName());
        googleEmailLabel.setText(googleUser.getEmail());
        System.out.println("Formulaire de sélection de rôle affiché");
    }

    @FXML
    private void handleGoogleRoleContinue() {
        // ✅ CORRIGÉ : si pendingUser != null, l'utilisateur existe déjà
        //              (cas Google + empreinte vocale) → ne pas recréer de compte
        if (pendingUser != null && currentGoogleUser == null) {
            // Utilisateur existant validé par voix → connexion directe
            finishLogin(pendingUser);
            return;
        }

        // Sinon : nouvel utilisateur Google → créer le compte
        if (currentGoogleUser == null) {
            showGoogleError("Session expirée, veuillez recommencer la connexion Google");
            return;
        }

        Role selectedRole = googleRoleChoice.getValue();
        if (selectedRole == null) {
            showGoogleError("Veuillez sélectionner un type de compte");
            return;
        }

        try {
            User newUser = new User();
            newUser.setNom(currentGoogleUser.getName());
            newUser.setEmail(currentGoogleUser.getEmail());
            newUser.setMotDePasse("GOOGLE_AUTH_" + currentGoogleUser.getGoogleId());
            newUser.setRole(selectedRole);
            newUser.setEtatCompte(EtatCompte.ACTIF);
            userService.ajouter(newUser);
            System.out.println("✅ Compte créé avec rôle : " + selectedRole);
            redirectToDashboard(newUser);
        } catch (Exception e) {
            e.printStackTrace();
            showGoogleError("Erreur lors de la création du compte");
        }
    }

    @FXML
    private void handleBackToLoginForm() {
        loginFormBox.setVisible(true);
        loginFormBox.setManaged(true);
        googleRoleBox.setVisible(false);
        googleRoleBox.setManaged(false);
        currentGoogleUser = null;
        pendingUser       = null;
        isGoogleFlow      = false;
        voiceRecordedFile = null;
        if (errorLabel      != null) errorLabel.setText("");
        if (googleErrorLabel != null) googleErrorLabel.setText("");
        System.out.println("Retour au formulaire de login");
    }

    // =========================================================================
    // NAVIGATION
    // =========================================================================
    @FXML
    private void handleRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/register.fxml"));
            Parent root = loader.load();
            RegisterController controller = loader.getController();
            controller.setContentPane(this.contentPane);
            contentPane.getChildren().clear();
            contentPane.getChildren().add(root);
            setAnchors(root);
        } catch (Exception e) {
            showError("Erreur lors du chargement de la page d'inscription");
            e.printStackTrace();
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
            setAnchors(root);
        } catch (Exception e) {
            showError("Erreur lors du chargement de la page Mot de passe oublié");
            e.printStackTrace();
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
            setAnchors(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void redirectToDashboard(User user) {
        try {
            String fxmlFile = switch (user.getRole()) {
                case AGRICULTEUR -> "/DashboardAgriculteur.fxml";
                case EXPERT      -> "/DashboardAgriculteur.fxml";
                case FOURNISSEUR -> "/home.fxml";           // ✅ était /fournisseurDashboard.fxml
                case ADMIN       -> "/adminDashboard.fxml";
            };

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            Object controller = loader.getController();
            if      (controller instanceof DashboardAdminController)
                ((DashboardAdminController)       controller).setUser(user);
            else if (controller instanceof DashboardAgriculteurController)
                ((DashboardAgriculteurController) controller).setUser(user);
            else if (controller instanceof HomeController)        // ✅ était DashboardFournisseurController
                ((HomeController)                controller).setUser(user);
            // ✅ si HomeController a setUser()
            else if (controller instanceof DashboardAgriculteurController)
                ((DashboardAgriculteurController) controller).setUser(user);

            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root);
            if (root instanceof Region r) {
                r.prefWidthProperty().bind(scene.widthProperty());
                r.prefHeightProperty().bind(scene.heightProperty());
            }
            stage.setScene(scene);
            stage.setTitle("AgriConnect - Dashboard " + user.getRole());
            Platform.runLater(() -> stage.setMaximized(true));

        } catch (Exception e) {
            System.err.println("ERREUR lors de la redirection :");
            e.printStackTrace();
            showError("Erreur lors de la redirection : " + e.getMessage());
        }
    }
    // =========================================================================
    // CONNEXION VOCALE UNIVERSELLE
    // =========================================================================
    @FXML
    private void handleVoiceLogin() {
        if (voiceProgressBar != null) {
            voiceProgressBar.setVisible(true);
            voiceProgressBar.setProgress(-1);
        }
        if (voiceStatusLabel != null) {
            voiceStatusLabel.setText("🔴 Parlez maintenant...");
            voiceStatusLabel.setVisible(true);
        }

        new Thread(() -> {
            File audioFile = recordVoice(3);

            if (audioFile == null) {
                Platform.runLater(() -> {
                    if (voiceProgressBar != null) voiceProgressBar.setVisible(false);
                    if (voiceStatusLabel != null) {
                        voiceStatusLabel.setText("❌ Erreur microphone");
                        voiceStatusLabel.setStyle("-fx-text-fill: #d32f2f;");
                    }
                });
                return;
            }

            VoiceAuthService.VoiceResult result = voiceAuthService.identifyFromFile(audioFile);

            Platform.runLater(() -> {
                if (voiceProgressBar != null) voiceProgressBar.setVisible(false);

                if (result.authenticated && result.similarity >= 0.80) {
                    User user = userService.findByEmail(result.username);

                    if (user != null && user.getEtatCompte() != EtatCompte.BLOQUE) {
                        String percent = String.format("%.0f%%", result.similarity * 100);
                        showSuccess("✅ Bienvenue " + user.getNom() + " ! (similarité " + percent + ")");

                        if (voiceStatusLabel != null) {
                            voiceStatusLabel.setText("✅ Identité confirmée !");
                            voiceStatusLabel.setStyle("-fx-text-fill: #4CAF50;");
                        }

                        javafx.animation.PauseTransition pause =
                                new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));
                        pause.setOnFinished(e -> finishLogin(user));
                        pause.play();

                    } else if (user != null && user.getEtatCompte() == EtatCompte.BLOQUE) {
                        showError("❌ Votre compte est bloqué");
                        if (voiceStatusLabel != null) voiceStatusLabel.setVisible(false);
                    } else {
                        showError("❌ Utilisateur introuvable");
                        if (voiceStatusLabel != null) voiceStatusLabel.setVisible(false);
                    }
                } else {
                    showError("❌ Voix non reconnue. Utilisez la connexion classique.");
                    if (voiceStatusLabel != null) {
                        voiceStatusLabel.setText("⚠️ Aucune correspondance (seuil: 80%)");
                        voiceStatusLabel.setStyle("-fx-text-fill: #ff9800;");
                    }
                }
            });
        }).start();
    }

    // =========================================================================
    // UTILITAIRES
    // =========================================================================
    private void setAnchors(Parent root) {
        AnchorPane.setTopAnchor(root, 0.0);
        AnchorPane.setBottomAnchor(root, 0.0);
        AnchorPane.setLeftAnchor(root, 0.0);
        AnchorPane.setRightAnchor(root, 0.0);
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
        }
    }

    private void showSuccess(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
        }
    }

    private void showGoogleError(String message) {
        if (googleErrorLabel != null) {
            googleErrorLabel.setText(message);
            googleErrorLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
        }
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

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

            File tmp = File.createTempFile("voice_login_", ".wav");
            tmp.deleteOnExit();
            AudioSystem.write(
                    new AudioInputStream(
                            new java.io.ByteArrayInputStream(buffer, 0, bytesRead),
                            format, bytesRead / format.getFrameSize()),
                    AudioFileFormat.Type.WAVE, tmp);
            return tmp;

        } catch (Exception e) {
            System.err.println("Erreur enregistrement : " + e.getMessage());
            return null;
        }
    }
}