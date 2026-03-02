package controller;

import entities.User;
import utils.Session;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardAgriculteurController {

    // ══════════════════════════════════════════════════════════════════════
    // LABELS DASHBOARD
    // ══════════════════════════════════════════════════════════════════════
    @FXML private Label        welcomeLabel;
    @FXML private Label        dateLabel;
    @FXML private Label        userNameLabel;
    @FXML private Label        themeModeText;
    @FXML private Label        messageLabel;
    @FXML private Label        culturesCountLabel;
    @FXML private Label        problemsCountLabel;
    @FXML private Label        ordersCountLabel;

    // ══════════════════════════════════════════════════════════════════════
    // LABELS MÉTÉO
    // ══════════════════════════════════════════════════════════════════════
    @FXML private Label        temperatureLabel;
    @FXML private Label        windspeedLabel;
    @FXML private Label        weatherCodeLabel;

    // ══════════════════════════════════════════════════════════════════════
    // LAYOUT
    // ══════════════════════════════════════════════════════════════════════
    @FXML private AnchorPane   contentPane;
    @FXML private AnchorPane   sidebarExpanded;
    @FXML private AnchorPane   sidebarCollapsed;
    @FXML private ToggleButton themeToggle;

    // ✅ ZONES DASHBOARD
    @FXML private VBox         dashboardStaticContent;   // Dashboard par défaut
    @FXML private AnchorPane   dynamicContent;          // Zone de chargement dynamique

    // ══════════════════════════════════════════════════════════════════════
    // MENUS SIDEBAR
    // ══════════════════════════════════════════════════════════════════════
    @FXML private HBox menuHome;
    @FXML private HBox menuCulture;
    @FXML private HBox menuProblemes;
    @FXML private HBox menuForum;
    @FXML private HBox menuReclamation;
    @FXML private HBox menuBoutique;
    @FXML private HBox menuAnalyse;

    // ══════════════════════════════════════════════════════════════════════
    // ÉTAT INTERNE
    // ══════════════════════════════════════════════════════════════════════
    private User       currentUser;
    private boolean    isDarkMode = false;
    private List<Node> originalDashboardContent;

    // ══════════════════════════════════════════════════════════════════════
    // API MÉTÉO
    // ══════════════════════════════════════════════════════════════════════
    private final String API_URL =
            "https://api.open-meteo.com/v1/forecast?latitude=36.8065&longitude=10.1815&current_weather=true";

    // ══════════════════════════════════════════════════════════════════════
    // INITIALISATION
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        System.out.println("✅ DashboardAgriculteurController initialisé");

        if (dateLabel != null) {
            LocalDate today = LocalDate.now();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
            dateLabel.setText(today.format(fmt));
        }

        if (menuHome != null) setActiveMenu(menuHome);

        if (culturesCountLabel != null) culturesCountLabel.setText("0");
        if (problemsCountLabel != null) problemsCountLabel.setText("0");
        if (ordersCountLabel   != null) ordersCountLabel.setText("0");

        loadWeather();
        startAutoRefresh();

        Platform.runLater(() -> {
            try {
                AnchorPane root = (AnchorPane) sidebarCollapsed.getParent();
                if (root != null && root.getScene() != null) {
                    Stage stage = (Stage) root.getScene().getWindow();
                    Scene scene = root.getScene();
                    stage.setMaximized(true);
                    if (scene.getRoot() instanceof Region r) {
                        r.prefWidthProperty().bind(scene.widthProperty());
                        r.prefHeightProperty().bind(scene.heightProperty());
                    }
                }

                // ✅ S'assurer que dashboard est visible au démarrage
                if (dashboardStaticContent != null) {
                    dashboardStaticContent.setVisible(true);
                    dashboardStaticContent.setManaged(true);
                }
                if (dynamicContent != null) {
                    dynamicContent.setVisible(false);
                    dynamicContent.setManaged(false);
                }

                setupSidebarHover();

            } catch (Exception e) {
                System.err.println("⚠️ Erreur initialisation : " + e.getMessage());
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // MÉTÉO
    // ══════════════════════════════════════════════════════════════════════
    private void loadWeather() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    URL url = new URL(API_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.setRequestMethod("GET");

                    if (conn.getResponseCode() == 200) {
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(conn.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) response.append(line);
                        reader.close();
                        conn.disconnect();

                        JSONObject json    = new JSONObject(response.toString());
                        JSONObject current = json.getJSONObject("current_weather");

                        double temperature = current.getDouble("temperature");
                        double windspeed   = current.getDouble("windspeed");
                        int    weathercode = current.getInt("weathercode");
                        String description = getWeatherDescription(weathercode);

                        Platform.runLater(() -> {
                            if (temperatureLabel != null) temperatureLabel.setText(temperature + " °C");
                            if (windspeedLabel   != null) windspeedLabel.setText(windspeed + " km/h");
                            if (weatherCodeLabel != null) weatherCodeLabel.setText(description);
                        });
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        if (temperatureLabel != null) temperatureLabel.setText("--");
                        if (windspeedLabel   != null) windspeedLabel.setText("--");
                        if (weatherCodeLabel != null) weatherCodeLabel.setText("Erreur API ⚠");
                    });
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private String getWeatherDescription(int code) {
        return switch (code) {
            case 0        -> "Ensoleillé ☀";
            case 1, 2, 3  -> "Partiellement nuageux ⛅";
            case 45, 48   -> "Brouillard 🌫";
            case 51,53,55 -> "Pluie légère 🌦";
            case 61,63,65 -> "Pluie 🌧";
            case 71,73,75 -> "Neige ❄";
            case 95       -> "Orage ⛈";
            default       -> "Conditions inconnues";
        };
    }

    private void startAutoRefresh() {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.minutes(30), e -> loadWeather())
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    // ══════════════════════════════════════════════════════════════════════
    // SIDEBAR HOVER
    // ══════════════════════════════════════════════════════════════════════
    private void setupSidebarHover() {
        sidebarCollapsed.setOnMouseEntered(e -> expandSidebar());
        sidebarExpanded.setOnMouseEntered(e  -> expandSidebar());
        sidebarCollapsed.setOnMouseExited(e  -> { if (!sidebarExpanded.isHover()) collapseSidebar(); });
        sidebarExpanded.setOnMouseExited(e   -> { if (!sidebarCollapsed.isHover()) collapseSidebar(); });
    }

    private void expandSidebar() {
        sidebarExpanded.setVisible(true);
        AnchorPane.setLeftAnchor(contentPane, 310.0);
    }

    private void collapseSidebar() {
        sidebarExpanded.setVisible(false);
        AnchorPane.setLeftAnchor(contentPane, 70.0);
    }

    // ══════════════════════════════════════════════════════════════════════
    // SET USER
    // ══════════════════════════════════════════════════════════════════════
    public void setUser(User user) {
        this.currentUser = user;
        // Aligner le module Event/Reclamation qui utilise utils.Session
        Session.getInstance().setCurrentUser(user);
        if (welcomeLabel != null) welcomeLabel.setText("Bienvenue, " + user.getNom() + " !");
        if (userNameLabel != null) userNameLabel.setText(user.getNom());
        System.out.println("✅ Utilisateur : " + user.getNom());
    }

    // ══════════════════════════════════════════════════════════════════════
    // THEME TOGGLE
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    private void handleToggleTheme() {
        isDarkMode = !isDarkMode;
        if (isDarkMode) {
            sidebarCollapsed.setStyle("-fx-background-color: #1a1a1a;");
            sidebarExpanded.setStyle("-fx-background-color: #1a1a1a; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 18, 0, 6, 0);");
            contentPane.setStyle("-fx-background-color: #1e1e1e;");
            if (themeModeText != null) themeModeText.setText("Sombre");
            if (themeToggle   != null) {
                themeToggle.setText("☀");
                themeToggle.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 10; -fx-cursor: hand;");
            }
        } else {
            sidebarCollapsed.setStyle("-fx-background-color: #2e4d2e;");
            sidebarExpanded.setStyle("-fx-background-color: #2e4d2e; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 18, 0, 6, 0);");
            contentPane.setStyle("-fx-background-color: #f0f4f0;");
            if (themeModeText != null) themeModeText.setText("Clair");
            if (themeToggle   != null) {
                themeToggle.setText("🌙");
                themeToggle.setStyle("-fx-background-color: #f5f8f5; -fx-background-radius: 10; -fx-cursor: hand;");
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // MENU ACTIF
    // ══════════════════════════════════════════════════════════════════════
    private void setActiveMenu(HBox activeMenuItem) {
        String def    = "-fx-cursor: hand; -fx-background-radius: 12; -fx-padding: 0 20;";
        String active = "-fx-cursor: hand; -fx-background-color: rgba(255,255,255,0.25); -fx-background-radius: 12; -fx-padding: 0 20;";

        if (menuHome        != null) menuHome.setStyle(def);
        if (menuCulture     != null) menuCulture.setStyle(def);
        if (menuProblemes   != null) menuProblemes.setStyle(def);
        if (menuForum       != null) menuForum.setStyle(def);
        if (menuReclamation != null) menuReclamation.setStyle(def);
        if (menuBoutique    != null) menuBoutique.setStyle(def);
        if (menuAnalyse     != null) menuAnalyse.setStyle(def);

        if (activeMenuItem  != null) activeMenuItem.setStyle(active);
    }

    // ══════════════════════════════════════════════════════════════════════
    // ✅ NOUVELLE MÉTHODE - CHARGER DANS ZONE DYNAMIQUE
    // ══════════════════════════════════════════════════════════════════════
    private FXMLLoader loadContentInDynamic(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();

            // Masquer dashboard statique
            if (dashboardStaticContent != null) {
                dashboardStaticContent.setVisible(false);
                dashboardStaticContent.setManaged(false);
            }

            // Afficher zone dynamique
            if (dynamicContent != null) {
                dynamicContent.setVisible(true);
                dynamicContent.setManaged(true);
                dynamicContent.getChildren().clear();
                dynamicContent.getChildren().add(content);

                AnchorPane.setTopAnchor(content, 0.0);
                AnchorPane.setBottomAnchor(content, 0.0);
                AnchorPane.setLeftAnchor(content, 0.0);
                AnchorPane.setRightAnchor(content, 0.0);
            }

            System.out.println("✅ Contenu chargé dans dynamicContent : " + fxmlPath);
            return loader;

        } catch (Exception e) {
            System.err.println("❌ Erreur chargement : " + fxmlPath + " → " + e.getMessage());
            e.printStackTrace();
            showMessage("⚠️ Impossible de charger la page.");
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // ANCIENNE MÉTHODE - COMPATIBILITÉ (utilise dynamicContent maintenant)
    // ══════════════════════════════════════════════════════════════════════
    private FXMLLoader loadContent(String fxmlPath) {
        return loadContentInDynamic(fxmlPath);
    }

    // ══════════════════════════════════════════════════════════════════════
    // ACCESSEURS POUR LES CONTRÔLEURS ENFANTS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * ✅ Expose le contentPane pour que CultureListController puisse
     * y charger CultureForm directement (sans nouveau Stage).
     */
    public AnchorPane getContentPane() {
        return contentPane;
    }

    /**
     * ✅ Expose la zone dynamique pour y charger le formulaire (Ma culture).
     * Ainsi "Annuler" peut revenir à la liste en rechargeant dans cette même zone.
     */
    public AnchorPane getDynamicContent() {
        return dynamicContent;
    }

    /**
     * ✅ Recharge la liste des cultures dans le contentPane.
     * Appelé par CultureController après sauvegarde ou annulation.
     */
    public void handleShowCultureDirect() {
        setActiveMenu(menuCulture);
        FXMLLoader loader = loadContent("/CreerCultureList.fxml");
        if (loader != null) {
            try {
                CultureListController ctrl = loader.getController();
                if (ctrl != null) ctrl.setDashboardController(this);
            } catch (Exception e) {
                System.err.println("⚠️ Erreur rechargement liste cultures : " + e.getMessage());
            }
        }
        System.out.println("📍 Retour liste cultures");
    }

    // ══════════════════════════════════════════════════════════════════════
    // NAVIGATION MENU
    // ══════════════════════════════════════════════════════════════════════

    /**
     * 🏠 RETOUR AU DASHBOARD
     */
    @FXML
    private void handleShowHome(MouseEvent event) {
        setActiveMenu(menuHome);
        System.out.println("🏠 Retour au dashboard principal");

        // Masquer la zone dynamique
        if (dynamicContent != null) {
            dynamicContent.setVisible(false);
            dynamicContent.setManaged(false);
            dynamicContent.getChildren().clear();
        }

        // Afficher le dashboard statique
        if (dashboardStaticContent != null) {
            dashboardStaticContent.setVisible(true);
            dashboardStaticContent.setManaged(true);
        }

        // Recharger la météo
        loadWeather();

        if (messageLabel != null) {
            messageLabel.setText("");
        }

        System.out.println("✅ Dashboard restauré");
    }

    @FXML
    private void handleShowCulture(MouseEvent event) {
        handleShowCultureDirect();
    }

    @FXML
    private void handleShowProblems(MouseEvent event) {
        setActiveMenu(menuProblemes);
        FXMLLoader loader = loadContent("/DiagnosticForm.fxml");
        if (loader != null) {
            try {
                DiagnosticController ctrl = loader.getController();
                if (ctrl != null) {
                    ctrl.setDashboardController(this);
                    if (currentUser != null) ctrl.setUserId(currentUser.getId());
                }
            } catch (Exception e) {
                System.err.println("⚠️ Erreur injection DiagnosticController : " + e.getMessage());
            }
        }
        System.out.println("📍 Problèmes / Diagnostic");
    }

    @FXML
    private void handleShowForum(MouseEvent event) {
        setActiveMenu(menuForum);
        FXMLLoader loader = loadContent("/ForumDashboard.fxml");
        if (loader != null) {
            try {
                ForumDashboardController ctrl = loader.getController();
                if (ctrl != null && currentUser != null) {
                    ctrl.setUser(currentUser);
                    ctrl.setCurrentUser(currentUser);
                }
            } catch (Exception e) {
                System.err.println("⚠️ Erreur injection ForumDashboardController : " + e.getMessage());
            }
        }
        System.out.println("📍 Forum");
    }

    @FXML
    private void handleShowReclamation(MouseEvent event) {
        setActiveMenu(menuReclamation);
        // Utilise le loader commun qui charge dans dynamicContent
        FXMLLoader loader = loadContent("/user-dashboard.fxml");
        if (loader != null) {
            System.out.println("✅ UserDashboard chargé dans dynamicContent");
        }
    }

    /**
     * 🛒 BOUTIQUE - Charge l'interface de commande
     */
    @FXML
    private void handleShowBoutique(MouseEvent event) {
        setActiveMenu(menuBoutique);
        System.out.println("🛒 Chargement de la boutique (interface commande)...");

        try {
            // Charger le FXML de l'interface commande
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/commande.fxml"));
            Parent commandeRoot = loader.load();

            // Masquer le dashboard statique
            if (dashboardStaticContent != null) {
                dashboardStaticContent.setVisible(false);
                dashboardStaticContent.setManaged(false);
            }

            // Afficher la zone dynamique
            if (dynamicContent != null) {
                dynamicContent.setVisible(true);
                dynamicContent.setManaged(true);
                dynamicContent.getChildren().clear();
                dynamicContent.getChildren().add(commandeRoot);

                // Ancrer le contenu
                AnchorPane.setTopAnchor(commandeRoot, 0.0);
                AnchorPane.setBottomAnchor(commandeRoot, 0.0);
                AnchorPane.setLeftAnchor(commandeRoot, 0.0);
                AnchorPane.setRightAnchor(commandeRoot, 0.0);
            }

            System.out.println("✅ Interface commande chargée avec succès");

            if (messageLabel != null) {
                messageLabel.setText("🛒 Boutique - Passez vos commandes");
                messageLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
            }

        } catch (IOException e) {
            System.err.println("❌ Erreur lors du chargement de commande.fxml");
            e.printStackTrace();
            showMessage("❌ Erreur lors du chargement de la boutique");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // ANALYSE / PLANT DISEASE
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    public void goAnalyse(MouseEvent event) {
        setActiveMenu(menuAnalyse);
        System.out.println("🌿 Lancement de l'analyse maladie...");

        FXMLLoader loader = loadContent("/PlantDiseaseApp.fxml");
        if (loader == null) {
            showMessage("❌ Erreur lors du chargement de l'analyse");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // SCANNER PLANTE (PlantNet)
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    public void handleScanPlant(ActionEvent event) {
        System.out.println("🌱 Lancement du scanner PlantNet...");

        FXMLLoader loader = loadContent("/PlantNetView.fxml");
        if (loader != null) {
            try {
                PlantNetController plantNetController = loader.getController();
                if (plantNetController != null) {
                    plantNetController.setDashboardController(this);
                }
                System.out.println("✅ PlantNet chargé");
            } catch (Exception e) {
                System.err.println("❌ Erreur injection PlantNetController");
                e.printStackTrace();
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // PROFIL
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    private void handleGoToProfil(MouseEvent event) {
        FXMLLoader loader = loadContent("/profil.fxml");
        if (loader != null) {
            try {
                ProfilController controller = loader.getController();
                controller.setUser(currentUser);
                controller.setDashboardController(this);
                System.out.println("✅ Profil chargé");
            } catch (Exception e) {
                System.err.println("❌ Erreur injection ProfilController");
                e.printStackTrace();
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // LOGOUT
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    private void handleLogout(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) contentPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Login");
            stage.setMaximized(false);
            System.out.println("✅ Déconnexion");
        } catch (Exception e) {
            System.err.println("❌ Erreur déconnexion");
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // RELOAD DASHBOARD
    // ══════════════════════════════════════════════════════════════════════
    public void reloadDashboardContent() {
        handleShowHome(null);
    }

    /** Alias pour compatibilité */
    public void reloadDashboard() {
        reloadDashboardContent();
    }

    // ══════════════════════════════════════════════════════════════════════
    // DIAGNOSTIC POUR UNE CULTURE SPÉCIFIQUE
    // ══════════════════════════════════════════════════════════════════════
    public void ouvrirDiagnosticPourCulture(entities.Culture culture) {
        setActiveMenu(menuProblemes);
        FXMLLoader loader = loadContent("/DiagnosticForm.fxml");
        if (loader != null) {
            try {
                DiagnosticController ctrl = loader.getController();
                if (ctrl != null) {
                    ctrl.setDashboardController(this);
                    ctrl.setCulture(culture);
                    if (currentUser != null) ctrl.setUserId(currentUser.getId());
                }
            } catch (Exception e) {
                System.err.println("⚠️ Erreur injection DiagnosticController : " + e.getMessage());
            }
        }
        System.out.println("📍 Diagnostic pour culture : " + culture.getNom());
    }

    // ══════════════════════════════════════════════════════════════════════
    // UTILITAIRES
    // ══════════════════════════════════════════════════════════════════════
    private void showMessage(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }
}