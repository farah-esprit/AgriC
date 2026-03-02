package controller;

import entities.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.transform.Rotate;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import service.GeminiService1;
import service.MediaService;
import service.NotificationService;
import service.ResponseService;
import service.ThreadService;
import service.TranslationService;
import service.UserService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ForumDashboardController implements Initializable {

    // ====================== FXML ======================
    @FXML private VBox              sidebar;
    @FXML private Label             sidebarAppName;
    @FXML private Label             sidebarAppSub;
    @FXML private VBox              sidebarUserCard;
    @FXML private Label             sidebarRoleLabel;
    @FXML private VBox              sidebarNav;
    @FXML private Label             sidebarFooter;
    @FXML private Label        welcomeLabel;

    @FXML private TextField         searchField;
    @FXML private Button            addThreadBtn;
    @FXML private ComboBox<String>  filterComboBox;
    @FXML private ComboBox<String>  languageComboBox;
    @FXML private Button            mesSujetsBtn;
    @FXML private Button            tousLesSujetsBtn;
    @FXML private ScrollPane        threadsScrollPane;
    @FXML private VBox              threadsContainer;

    @FXML private Label             userNameLabel;
    @FXML private Label             userStatusLabel;

    // ✅ Notification components
    private StackPane notificationBellContainer;
    private Label     notificationIcon;
    private Label     notificationCountLabel;

    // ====================== SIDEBAR COLLAPSE ==========
    private static final double SIDEBAR_EXPANDED  = 265;
    private static final double SIDEBAR_COLLAPSED = 64;
    private Timeline sidebarTimeline;
    private boolean  sidebarExpanded = true;

    // ====================== STATE =====================
    private User              currentUser;
    private List<ForumThread> allThreads       = new ArrayList<>();
    private Map<Integer,User> userCache        = new HashMap<>();
    private boolean           showingMyThreads = false;
    private Map<Integer, ForumThread> originalThreads = new HashMap<>();

    // ====================== SERVICES ==================
    private final ThreadService        threadService        = new ThreadService();
    private final MediaService         mediaService         = new MediaService();
    private final ResponseService      responseService      = new ResponseService();
    private final UserService          userService          = new UserService();
    private final NotificationService  notificationService  = new NotificationService();
    private final TranslationService   translationService   = new TranslationService();
    private final GeminiService1        geminiService1        = new GeminiService1();

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String[] CATEGORIES = {
            "All","🌾 Cultures & Récoltes","🐄 Élevage & Animaux",
            "🚜 Matériel & Équipement","🌱 Semences & Plantation",
            "💧 Irrigation & Eau","🧪 Engrais & Traitements",
            "📊 Agroéconomie & Marché","🌍 Agroécologie & Bio",
            "🔧 Réparations & Astuces","❓ Questions Générales"
    };

    private Timeline notificationRefreshTimer;

    // ====================== INITIALIZE ================
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        filterComboBox.getItems().addAll(CATEGORIES);
        filterComboBox.setValue("All");
        filterComboBox.setOnAction(e -> applyFilters());
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());

        // Setup language selector
        languageComboBox.getItems().addAll("Original", "🇫🇷 Français", "🇬🇧 English", "🇸🇦 العربية", "🇪🇸 Español", "🇩🇪 Deutsch");
        languageComboBox.setValue("Original");
        languageComboBox.setOnAction(e -> handleLanguageChange());

        setupSidebarHover();

        Platform.runLater(() -> {
            // ── FULLSCREEN on startup ──────────────────────────────────────────
            try {
                Stage stage = (Stage) threadsContainer.getScene().getWindow();
                if (stage != null) {
                    stage.setMaximized(true);          // Maximized window (taskbar still visible)
                    // If you want TRUE fullscreen (no taskbar), uncomment the next line:
                    // stage.setFullScreen(true);
                }
            } catch (Exception ex) {
                System.err.println("⚠ Could not maximize window: " + ex.getMessage());
            }
            // ──────────────────────────────────────────────────────────────────
            setupNotificationBell();
        });

        loadUserCache();
        loadAllThreads();
        applyFilters();
    }

    // ====================== TRANSLATION ====================

    private void handleLanguageChange() {
        String selected = languageComboBox.getValue();
        if (selected == null) return;

        if (selected.equals("Original")) {
            restoreOriginalThreads();
            applyFilters();
            return;
        }

        String targetLang = getLanguageCode(selected);
        if (targetLang == null) return;

        Label loading = new Label("🔄 Traduction en cours...");
        loading.setStyle("-fx-font-size: 16px; -fx-text-fill: #666; -fx-padding: 20px;");
        threadsContainer.getChildren().clear();
        threadsContainer.getChildren().add(loading);

        new Thread(() -> {
            try {
                List<ForumThread> translatedThreads = new ArrayList<>();
                for (ForumThread thread : allThreads) {
                    if (!originalThreads.containsKey(thread.getThreadId())) {
                        originalThreads.put(thread.getThreadId(), cloneThread(thread));
                    }
                    if (translationService.needsTranslation(thread.getTitre(), targetLang)) {
                        ForumThread translated = translationService.translateThread(thread, targetLang);
                        translatedThreads.add(translated);
                    } else {
                        translatedThreads.add(thread);
                    }
                }
                Platform.runLater(() -> {
                    allThreads.clear();
                    allThreads.addAll(translatedThreads);
                    applyFilters();
                });
            } catch (Exception e) {
                System.err.println("❌ Translation error: " + e.getMessage());
                Platform.runLater(() -> {
                    alert(Alert.AlertType.ERROR, "Erreur", "Impossible de traduire: " + e.getMessage());
                    restoreOriginalThreads();
                    applyFilters();
                });
            }
        }).start();
    }

    private void restoreOriginalThreads() {
        if (originalThreads.isEmpty()) return;
        for (int i = 0; i < allThreads.size(); i++) {
            ForumThread thread = allThreads.get(i);
            if (originalThreads.containsKey(thread.getThreadId())) {
                allThreads.set(i, originalThreads.get(thread.getThreadId()));
            }
        }
    }

    private String getLanguageCode(String displayName) {
        if (displayName.contains("Français")) return "fr";
        if (displayName.contains("English")) return "en";
        if (displayName.contains("العربية")) return "ar";
        if (displayName.contains("Español")) return "es";
        if (displayName.contains("Deutsch")) return "de";
        return null;
    }

    private ForumThread cloneThread(ForumThread original) {
        ForumThread clone = new ForumThread();
        clone.setThreadId(original.getThreadId());
        clone.setTitre(original.getTitre());
        clone.setContenu(original.getContenu());
        clone.setDateCreation(original.getDateCreation());
        clone.setStatus(original.getStatus());
        clone.setUser(original.getUser());
        clone.setCategory(original.getCategory());
        clone.setTags(original.getTags());
        clone.setViews(original.getViews());
        clone.setLikes(original.getLikes());
        clone.setLikedBy(original.getLikedBy());

        return clone;
    }
    public void setUser(User user) {
        this.currentUser = user;
        if (welcomeLabel != null) welcomeLabel.setText("Bienvenue, " + user.getNom() + " !");
        if (userNameLabel != null) userNameLabel.setText(user.getNom());
        System.out.println("✅ Utilisateur : " + user.getNom());
    }

    // ====================== NOTIFICATION BELL =========

    private void setupNotificationBell() {
        try {
            notificationBellContainer = (StackPane) threadsContainer.getScene().getRoot()
                    .lookup(".topbar-notif-wrap");

            if (notificationBellContainer != null) {
                notificationIcon = (Label) notificationBellContainer.lookup(".topbar-notif-icon");
                notificationCountLabel = (Label) notificationBellContainer.lookup(".topbar-notif-count");

                notificationBellContainer.setOnMouseClicked(e -> openNotificationsWindow());
                notificationBellContainer.setStyle(notificationBellContainer.getStyle() + "-fx-cursor: hand;");

                notificationBellContainer.setOnMouseEntered(e -> {
                    ScaleTransition scale = new ScaleTransition(Duration.millis(150), notificationBellContainer);
                    scale.setToX(1.15);
                    scale.setToY(1.15);
                    scale.play();
                });

                notificationBellContainer.setOnMouseExited(e -> {
                    ScaleTransition scale = new ScaleTransition(Duration.millis(150), notificationBellContainer);
                    scale.setToX(1.0);
                    scale.setToY(1.0);
                    scale.play();
                });

                updateNotificationBadge();
                System.out.println("✅ Notification bell setup complete");
            } else {
                System.err.println("⚠ Notification bell container not found.");
            }
        } catch (Exception e) {
            System.err.println("⚠ Error setting up notification bell: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateNotificationBadge() {
        if (currentUser == null || notificationCountLabel == null) {
            if (notificationCountLabel != null) notificationCountLabel.setVisible(false);
            return;
        }
        try {
            int unreadCount = notificationService.countUnreadByUser(currentUser.getId());
            if (unreadCount > 0) {
                notificationCountLabel.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
                notificationCountLabel.setVisible(true);
                ScaleTransition pulse = new ScaleTransition(Duration.millis(300), notificationCountLabel);
                pulse.setFromX(1.0); pulse.setFromY(1.0);
                pulse.setToX(1.3);   pulse.setToY(1.3);
                pulse.setCycleCount(2);
                pulse.setAutoReverse(true);
                pulse.play();
            } else {
                notificationCountLabel.setVisible(false);
            }
        } catch (Exception e) {
            System.err.println("Error updating notification badge: " + e.getMessage());
            if (notificationCountLabel != null) notificationCountLabel.setVisible(false);
        }
    }

    private void startNotificationRefreshTimer() {
        if (notificationRefreshTimer != null) notificationRefreshTimer.stop();
        notificationRefreshTimer = new Timeline(
                new KeyFrame(Duration.seconds(30), e -> updateNotificationBadge())
        );
        notificationRefreshTimer.setCycleCount(Timeline.INDEFINITE);
        notificationRefreshTimer.play();
    }

    private void stopNotificationRefreshTimer() {
        if (notificationRefreshTimer != null) {
            notificationRefreshTimer.stop();
            notificationRefreshTimer = null;
        }
    }

    private void openNotificationsWindow() {
        if (!requireLogin()) return;
        try {
            Stage stage = new Stage();
            stage.setTitle("🔔 Notifications");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(threadsContainer.getScene().getWindow());
            VBox root = buildNotificationsView();
            Scene scene = new Scene(root, 500, 600);
            stage.setScene(scene);
            stage.setOnHidden(e -> { updateNotificationBadge(); refresh(); });
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            alert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir les notifications:\n" + e.getMessage());
        }
    }

    private VBox buildNotificationsView() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f5f5f5;");

        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20));
        header.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");

        Label titleLabel = new Label("🔔 Notifications");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Button markAllReadBtn = new Button("✓ Tout marquer comme lu");
        markAllReadBtn.setStyle(
                "-fx-background-color: #4CAF50; -fx-text-fill: white; " +
                        "-fx-font-size: 12px; -fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand;"
        );
        markAllReadBtn.setOnAction(e -> {
            notificationService.markAllAsReadByUser(currentUser.getId());
            ((Stage) markAllReadBtn.getScene().getWindow()).close();
        });

        Button clearReadBtn = new Button("🗑 Effacer lues");
        clearReadBtn.setStyle(
                "-fx-background-color: #757575; -fx-text-fill: white; " +
                        "-fx-font-size: 12px; -fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand;"
        );
        clearReadBtn.setOnAction(e -> {
            notificationService.deleteReadByUser(currentUser.getId());
            ((Stage) clearReadBtn.getScene().getWindow()).close();
        });

        header.getChildren().addAll(titleLabel, markAllReadBtn, clearReadBtn);

        VBox notificationsList = new VBox(0);
        ScrollPane scrollPane = new ScrollPane(notificationsList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        try {
            List<Notification> notifications = notificationService.getByUser(currentUser.getId());
            if (notifications.isEmpty()) {
                VBox emptyState = new VBox(20);
                emptyState.setAlignment(Pos.CENTER);
                emptyState.setPadding(new Insets(60));
                Label emptyLabel = new Label("🌾 Aucune notification");
                emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #888;");
                Label emptySubLabel = new Label("Vous serez notifié des nouvelles réponses\net des changements de statut");
                emptySubLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #aaa; -fx-text-alignment: center;");
                emptySubLabel.setWrapText(true);
                emptyState.getChildren().addAll(emptyLabel, emptySubLabel);
                notificationsList.getChildren().add(emptyState);
            } else {
                for (Notification notif : notifications) {
                    notificationsList.getChildren().add(buildNotificationCard(notif));
                }
            }
        } catch (Exception e) {
            Label errorLabel = new Label("⚠ Erreur lors du chargement des notifications");
            errorLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #F44336; -fx-padding: 20;");
            notificationsList.getChildren().add(errorLabel);
        }

        root.getChildren().addAll(header, scrollPane);
        return root;
    }

    private VBox buildNotificationCard(Notification notif) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15, 20, 15, 20));
        String bgColor = notif.isRead() ? "#ffffff" : "#E8F5E9";
        card.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-border-color: #e0e0e0;" +
                        "-fx-border-width: 0 0 1 0;" +
                        "-fx-cursor: hand;"
        );
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-background-color: #f9f9f9;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-border-color: #e0e0e0;" +
                        "-fx-border-width: 0 0 1 0;" +
                        "-fx-cursor: hand;"
        ));

        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        Label iconLabel = new Label(getNotificationIcon(notif.getType()));
        iconLabel.setStyle("-fx-font-size: 20px;");
        Label typeLabel = new Label(getNotificationTypeLabel(notif.getType()));
        typeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label dateLabel = new Label(notif.getDateCreation().format(DATE_FMT));
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");

        if (!notif.isRead()) {
            Label unreadDot = new Label("⬤");
            unreadDot.setStyle("-fx-font-size: 8px; -fx-text-fill: #4CAF50;");
            headerRow.getChildren().addAll(iconLabel, typeLabel, spacer, dateLabel, unreadDot);
        } else {
            headerRow.getChildren().addAll(iconLabel, typeLabel, spacer, dateLabel);
        }

        Label messageLabel = new Label(notif.getMessage());
        messageLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #333; -fx-wrap-text: true;");
        messageLabel.setWrapText(true);
        card.getChildren().addAll(headerRow, messageLabel);

        card.setOnMouseClicked(e -> {
            if (!notif.isRead()) notificationService.markAsRead(notif.getNotificationId());
            if (notif.getThread() != null) {
                ForumThread thread = allThreads.stream()
                        .filter(t -> t.getThreadId() == notif.getThread().getThreadId())
                        .findFirst().orElse(null);
                if (thread != null) {
                    ((Stage) card.getScene().getWindow()).close();
                    openViewWindow(thread);
                }
            }
        });
        return card;
    }

    private String getNotificationIcon(NotificationType type) {
        switch (type) {
            case NEW_RESPONSE:       return "💬";
            case THREAD_REPLY:       return "📩";
            case THREAD_STATUS_CHANGE: return "🔄";
            case MENTION:            return "📢";
            default:                 return "🔔";
        }
    }

    private String getNotificationTypeLabel(NotificationType type) {
        switch (type) {
            case NEW_RESPONSE:       return "Nouvelle réponse";
            case THREAD_REPLY:       return "Réponse à votre sujet";
            case THREAD_STATUS_CHANGE: return "Changement de statut";
            case MENTION:            return "Mention";
            default:                 return "Notification";
        }
    }

    // ====================== SIDEBAR HOVER =============

    private void setupSidebarHover() {
        sidebar.setOnMouseEntered(e -> animateSidebar(true));
        sidebar.setOnMouseExited(e -> animateSidebar(false));
        sidebar.setPrefWidth(SIDEBAR_COLLAPSED);
        setSidebarLabelsVisible(false);
        setSidebarButtonTextVisible(false);
        sidebarExpanded = false;
    }

    private void animateSidebar(boolean expand) {
        if (expand == sidebarExpanded) return;
        sidebarExpanded = expand;
        double targetWidth = expand ? SIDEBAR_EXPANDED : SIDEBAR_COLLAPSED;
        if (sidebarTimeline != null) sidebarTimeline.stop();
        sidebarTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(sidebar.prefWidthProperty(), sidebar.getPrefWidth(), Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(220),
                        new KeyValue(sidebar.prefWidthProperty(), targetWidth, Interpolator.EASE_BOTH))
        );
        if (expand) {
            setSidebarLabelsVisible(true);
            setSidebarButtonTextVisible(true);
            FadeTransition ft = new FadeTransition(Duration.millis(180), sidebar);
            ft.setFromValue(0.7); ft.setToValue(1.0); ft.play();
        } else {
            FadeTransition ft = new FadeTransition(Duration.millis(120), sidebar);
            ft.setFromValue(1.0); ft.setToValue(0.85); ft.play();
            sidebarTimeline.setOnFinished(e -> {
                setSidebarLabelsVisible(false);
                setSidebarButtonTextVisible(false);
            });
        }
        sidebarTimeline.play();
    }

    private void setSidebarLabelsVisible(boolean visible) {
        double opacity = visible ? 1.0 : 0.0;
        if (sidebarAppName   != null) sidebarAppName.setOpacity(opacity);
        if (sidebarAppSub    != null) sidebarAppSub.setOpacity(opacity);
        if (userNameLabel    != null) userNameLabel.setOpacity(opacity);
        if (userStatusLabel  != null) userStatusLabel.setOpacity(opacity);
        if (sidebarRoleLabel != null) sidebarRoleLabel.setOpacity(opacity);
        if (sidebarFooter    != null) sidebarFooter.setOpacity(opacity);
    }

    private void setSidebarButtonTextVisible(boolean visible) {
        if (mesSujetsBtn     != null) mesSujetsBtn.setText(visible    ? "📋  Mes Sujets"    : "📋");
        if (tousLesSujetsBtn != null) tousLesSujetsBtn.setText(visible ? "📚  Tous les Sujets" : "📚");
    }

    // ====================== DATA ======================

    private void loadUserCache() {
        try {
            List<User> users = userService.getAll();
            userCache.clear();
            for (User u : users) userCache.put(u.getId(), u);
        } catch (Exception e) {
            System.err.println("Cache users error: " + e.getMessage());
        }
    }

    private void loadAllThreads() {
        originalThreads.clear();
        try {
            allThreads = threadService.getAll();
        } catch (Exception e) {
            allThreads = new ArrayList<>();
            System.err.println("Load threads error: " + e.getMessage());
        }
    }

    private void refresh() {
        loadUserCache();
        loadAllThreads();
        applyFilters();
        updateNotificationBadge();
    }

    // ====================== FILTER ====================

    private void applyFilters() {
        String search   = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        String category = filterComboBox.getValue();

        List<ForumThread> result = new ArrayList<>();
        for (ForumThread t : allThreads) {
            if (showingMyThreads) {
                if (currentUser == null) continue;
                if (t.getUser().getId() != currentUser.getId()) continue;
            }
            boolean ms = search.isEmpty()
                    || t.getTitre().toLowerCase().contains(search)
                    || t.getContenu().toLowerCase().contains(search);
            boolean mc = "All".equals(category) || category.equals(t.getCategory());
            if (ms && mc) result.add(t);
        }
        result.sort((a, b) -> b.getDateCreation().compareTo(a.getDateCreation()));
        displayThreads(result);
        updateTabStyles();
    }

    private void updateTabStyles() {
        if (mesSujetsBtn == null || tousLesSujetsBtn == null) return;
        if (showingMyThreads) {
            mesSujetsBtn.setStyle("-fx-background-color:#4CAF50;-fx-text-fill:white;-fx-font-weight:bold;");
            tousLesSujetsBtn.setStyle("");
        } else {
            tousLesSujetsBtn.setStyle("-fx-background-color:#4CAF50;-fx-text-fill:white;-fx-font-weight:bold;");
            mesSujetsBtn.setStyle("");
        }
    }

    // ====================== DISPLAY ===================

    private void displayThreads(List<ForumThread> threads) {
        threadsContainer.getChildren().clear();
        if (threads == null || threads.isEmpty()) { showEmptyState(); return; }
        for (ForumThread t : threads) {
            try {
                StackPane flippableCard = buildFlippableCard(t);
                threadsContainer.getChildren().add(flippableCard);
                VBox.setMargin(flippableCard, new Insets(0, 0, 14, 0));
            } catch (Exception e) {
                System.err.println("Card error #" + t.getThreadId() + ": " + e.getMessage());
            }
        }
    }

    // ====================== FLIPPABLE CARD ============

    private StackPane buildFlippableCard(ForumThread thread) {
        VBox frontFace = buildCard(thread);
        frontFace.getStyleClass().remove("thread-card");
        VBox backFace = buildBackFace(thread);
        backFace.setVisible(false);
        StackPane wrapper = new StackPane(frontFace, backFace);
        wrapper.getStyleClass().add("thread-card");

        final boolean[] flipped   = {false};
        final boolean[] animating = {false};

        wrapper.setOnMouseClicked(event -> {
            if (event.getTarget() instanceof Button) return;
            if (animating[0]) return;
            animating[0] = true;

            if (!flipped[0]) {
                ScaleTransition zoomIn = new ScaleTransition(Duration.millis(150), wrapper);
                zoomIn.setFromX(1.0); zoomIn.setToX(1.06);
                zoomIn.setFromY(1.0); zoomIn.setToY(1.06);
                zoomIn.setInterpolator(Interpolator.EASE_OUT);

                RotateTransition rotateOut = new RotateTransition(Duration.millis(200), wrapper);
                rotateOut.setAxis(Rotate.Y_AXIS);
                rotateOut.setFromAngle(0); rotateOut.setToAngle(90);
                rotateOut.setInterpolator(Interpolator.EASE_IN);
                rotateOut.setOnFinished(e -> {
                    frontFace.setVisible(false); backFace.setVisible(true);
                    wrapper.setRotationAxis(Rotate.Y_AXIS); wrapper.setRotate(-90);
                    RotateTransition rotateIn = new RotateTransition(Duration.millis(200), wrapper);
                    rotateIn.setAxis(Rotate.Y_AXIS);
                    rotateIn.setFromAngle(-90); rotateIn.setToAngle(0);
                    rotateIn.setInterpolator(Interpolator.EASE_OUT);
                    ScaleTransition settle = new ScaleTransition(Duration.millis(130), wrapper);
                    settle.setFromX(1.06); settle.setToX(1.0);
                    settle.setFromY(1.06); settle.setToY(1.0);
                    settle.setInterpolator(Interpolator.EASE_IN);
                    settle.setOnFinished(ev -> { flipped[0] = true; animating[0] = false; });
                    rotateIn.setOnFinished(ev -> settle.play());
                    rotateIn.play();
                });
                new SequentialTransition(zoomIn, rotateOut).play();
            } else {
                RotateTransition rotateOut2 = new RotateTransition(Duration.millis(200), wrapper);
                rotateOut2.setAxis(Rotate.Y_AXIS);
                rotateOut2.setFromAngle(0); rotateOut2.setToAngle(90);
                rotateOut2.setInterpolator(Interpolator.EASE_IN);
                rotateOut2.setOnFinished(e -> {
                    backFace.setVisible(false); frontFace.setVisible(true);
                    wrapper.setRotationAxis(Rotate.Y_AXIS); wrapper.setRotate(-90);
                    RotateTransition rotateIn2 = new RotateTransition(Duration.millis(200), wrapper);
                    rotateIn2.setAxis(Rotate.Y_AXIS);
                    rotateIn2.setFromAngle(-90); rotateIn2.setToAngle(0);
                    rotateIn2.setInterpolator(Interpolator.EASE_OUT);
                    rotateIn2.setOnFinished(ev -> { flipped[0] = false; animating[0] = false; });
                    rotateIn2.play();
                });
                rotateOut2.play();
            }
        });
        return wrapper;
    }

    // ====================== BACK FACE =================

    private VBox buildBackFace(ForumThread thread) {
        VBox back = new VBox(12);
        back.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #2E7D32, #1B5E20);" +
                        "-fx-background-radius: 14;" +
                        "-fx-padding: 18;"
        );
        back.setPrefWidth(Double.MAX_VALUE);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("💬");
        icon.setStyle("-fx-font-size:20px;");
        Label titleLbl = new Label(thread.getTitre());
        titleLbl.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;");
        HBox.setHgrow(titleLbl, Priority.ALWAYS);
        Label hint = new Label("✖ cliquer pour fermer");
        hint.setStyle("-fx-font-size:10px;-fx-text-fill:rgba(255,255,255,0.50);");
        header.getChildren().addAll(icon, titleLbl, hint);
        back.getChildren().add(header);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:rgba(255,255,255,0.25);");
        back.getChildren().add(sep);

        VBox commentsList = new VBox(10);
        try {
            List<Response> responses = responseService.getByThread(thread.getThreadId());
            if (responses == null || responses.isEmpty()) {
                Label empty = new Label("🌿 Pas encore de commentaires — soyez le premier !");
                empty.setStyle("-fx-text-fill:rgba(255,255,255,0.70);-fx-font-style:italic;-fx-font-size:13px;");
                commentsList.getChildren().add(empty);
            } else {
                for (Response r : responses) commentsList.getChildren().add(buildCommentRow(r));
            }
        } catch (Exception ex) {
            Label err = new Label("⚠ Impossible de charger les commentaires.");
            err.setStyle("-fx-text-fill:rgba(255,220,0,0.9);-fx-font-size:12px;");
            commentsList.getChildren().add(err);
        }

        ScrollPane scroll = new ScrollPane(commentsList);
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(260);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;-fx-border-color:transparent;");
        scroll.setOnMouseClicked(e -> e.consume());
        VBox.setVgrow(scroll, Priority.ALWAYS);
        back.getChildren().add(scroll);

        Button addBtn = new Button("💬 Ajouter un commentaire");
        addBtn.setStyle(
                "-fx-background-color:white;-fx-text-fill:#2E7D32;" +
                        "-fx-font-weight:bold;-fx-font-size:12px;" +
                        "-fx-background-radius:20;-fx-padding:9 18 9 18;-fx-cursor:hand;"
        );
        addBtn.setOnAction(e -> { e.consume(); handleAddComment(thread); });
        addBtn.setOnMouseClicked(e -> e.consume());
        back.getChildren().add(addBtn);
        return back;
    }

    private HBox buildCommentRow(Response r) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);
        row.setStyle("-fx-background-color:rgba(255,255,255,0.12);-fx-background-radius:10;-fx-padding:10 12 10 12;");

        Label avatar = new Label("👤");
        avatar.setStyle("-fx-font-size:18px;");

        VBox textCol = new VBox(3);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        String authorName = "Utilisateur";
        try {
            User author = userCache.get(r.getUser().getId());
            if (author != null && author.getNom() != null) authorName = author.getNom();
        } catch (Exception ignored) {}

        HBox metaLine = new HBox(10);
        metaLine.setAlignment(Pos.CENTER_LEFT);
        Label authorLbl = new Label(authorName);
        authorLbl.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:white;");
        Label dateLbl = new Label(r.getDateCreation() != null ? r.getDateCreation().format(DATE_FMT) : "");
        dateLbl.setStyle("-fx-font-size:10px;-fx-text-fill:rgba(255,255,255,0.55);");
        metaLine.getChildren().addAll(authorLbl, dateLbl);

        Label contentLbl = new Label(r.getContenu());
        contentLbl.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.88);-fx-wrap-text:true;");
        contentLbl.setWrapText(true);

        HBox likeLine = new HBox(8);
        likeLine.setAlignment(Pos.CENTER_LEFT);

        boolean isLiked = false;
        if (currentUser != null) {
            isLiked = responseService.hasUserLikedResponse(r.getResponseId(), currentUser.getId());
        }

        Label responseLikeIcon  = new Label(isLiked ? "❤️" : "🤍");
        responseLikeIcon.setStyle("-fx-font-size:12px;-fx-cursor:hand;");
        Label responseLikeCount = new Label(r.getLikes() + " likes");
        responseLikeCount.setStyle("-fx-font-size:11px;-fx-text-fill:rgba(255,255,255,0.7);");

        HBox likeBox = new HBox(4);
        likeBox.setAlignment(Pos.CENTER_LEFT);
        likeBox.getChildren().addAll(responseLikeIcon, responseLikeCount);
        likeBox.setStyle("-fx-cursor:hand;");

        likeBox.setOnMouseClicked(ev -> {
            ev.consume();
            if (!requireLogin()) return;
            boolean liked = responseService.likeResponse(r.getResponseId(), currentUser.getId());
            responseLikeIcon.setText(liked ? "❤️" : "🤍");
            Response updated = responseService.getByThread(r.getThread().getThreadId())
                    .stream()
                    .filter(resp -> resp.getResponseId() == r.getResponseId())
                    .findFirst().orElse(null);
            if (updated != null) {
                responseLikeCount.setText(updated.getLikes() + " likes");
                r.setLikes(updated.getLikes());
                r.setLikedBy(updated.getLikedBy());
            }
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), responseLikeIcon);
            scale.setFromX(1.0); scale.setFromY(1.0);
            scale.setToX(1.4);   scale.setToY(1.4);
            scale.setCycleCount(2); scale.setAutoReverse(true); scale.play();
        });

        likeLine.getChildren().add(likeBox);
        textCol.getChildren().addAll(metaLine, contentLbl, likeLine);
        row.getChildren().addAll(avatar, textCol);
        return row;
    }

    // ====================== CARD ======================

    private VBox buildCard(ForumThread thread) {
        VBox card = new VBox(0);
        card.getStyleClass().add("thread-card");
        card.getChildren().add(buildImageSection(thread));

        VBox body = new VBox(10);
        body.getStyleClass().add("card-body");
        body.setPadding(new Insets(14));

        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label titleLbl = new Label(thread.getTitre());
        titleLbl.getStyleClass().add("thread-title");
        titleLbl.setWrapText(true);
        HBox.setHgrow(titleLbl, Priority.ALWAYS);
        titleRow.getChildren().addAll(titleLbl, buildStatusBadge(thread.getStatus()));

        HBox metaRow = new HBox(10);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        if (thread.getCategory() != null && !thread.getCategory().isEmpty()) {
            Label cat = new Label(thread.getCategory());
            cat.getStyleClass().add("category-badge");
            metaRow.getChildren().add(cat);
        }
        if (thread.getTags() != null && !thread.getTags().trim().isEmpty()) {
            Label tags = new Label("🏷 " + thread.getTags());
            tags.getStyleClass().add("tags-label");
            metaRow.getChildren().add(tags);
        }

        Label content = new Label(thread.getContenu());
        content.getStyleClass().add("thread-content");
        content.setWrapText(true);
        content.setMaxHeight(72);

        body.getChildren().addAll(titleRow, metaRow, content, new Separator(),
                buildFooter(thread), buildActions(thread));
        card.getChildren().add(body);
        return card;
    }

    private VBox buildImageSection(ForumThread thread) {
        VBox container = new VBox();
        container.setAlignment(Pos.CENTER);
        container.getStyleClass().add("card-image-container");
        boolean loaded = false;
        try {
            List<Media> mediaList = mediaService.getMediaByThread(thread.getThreadId());
            if (mediaList != null) {
                for (Media media : mediaList) {
                    if (media.getType() == MediaType.IMAGE && media.getUrl() != null) {
                        File imgFile = new File(media.getUrl());
                        if (imgFile.exists()) {
                            Image img = new Image(imgFile.toURI().toString(), 800, 260, true, true);
                            if (!img.isError()) {
                                ImageView iv = new ImageView(img);
                                iv.setFitHeight(260); iv.setFitWidth(800);
                                iv.setPreserveRatio(true); iv.setSmooth(true);
                                iv.getStyleClass().add("card-image");
                                container.getChildren().add(iv);
                                loaded = true;
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Image error thread #" + thread.getThreadId() + ": " + e.getMessage());
        }
        if (!loaded) {
            VBox ph = new VBox();
            ph.getStyleClass().add("no-image-placeholder");
            ph.setAlignment(Pos.CENTER);
            ph.setPrefHeight(80);
            Label icon = new Label("🌾");
            icon.setStyle("-fx-font-size:28px;");
            ph.getChildren().add(icon);
            container.getChildren().add(ph);
        }
        return container;
    }

    private Label buildStatusBadge(ThreadStatus status) {
        String text = "🟢 OPEN", css = "status-open";
        if (status != null) switch (status) {
            case RESOLVED: text = "🟠 RESOLVED"; css = "status-resolved"; break;
            case CLOSED:   text = "⚫ CLOSED";   css = "status-closed";   break;
        }
        Label badge = new Label(text);
        badge.getStyleClass().addAll("thread-status", css);
        return badge;
    }

    private HBox buildFooter(ForumThread thread) {
        HBox footer = new HBox(20);
        footer.getStyleClass().add("thread-footer-row");
        footer.setAlignment(Pos.CENTER_LEFT);
        User author = userCache.get(thread.getUser().getId());
        String name = (author != null && author.getNom() != null)
                ? author.getNom() : "User #" + thread.getUser().getId();
        footer.getChildren().add(chip("👤", name));
        footer.getChildren().add(chip("📅", thread.getDateCreation().format(DATE_FMT)));
        footer.getChildren().add(chip("👁", thread.getViews() + " vues"));
        footer.getChildren().add(buildLikeChip(thread));
        int count = 0;
        try { count = responseService.countByThread(thread.getThreadId()); }
        catch (Exception e) { System.err.println("countByThread error: " + e.getMessage()); }
        footer.getChildren().add(chip("💬", count + " réponses"));
        return footer;
    }

    private HBox chip(String emoji, String text) {
        HBox h = new HBox(5);
        h.setAlignment(Pos.CENTER_LEFT);
        Label t = new Label(text);
        t.getStyleClass().add("footer-text");
        h.getChildren().addAll(new Label(emoji), t);
        return h;
    }

    private HBox buildLikeChip(ForumThread thread) {
        HBox h = new HBox(5);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setStyle("-fx-cursor: hand;");
        boolean isLiked = currentUser != null
                && threadService.hasUserLikedThread(thread.getThreadId(), currentUser.getId());
        Label heartIcon  = new Label(isLiked ? "❤️" : "🤍");
        heartIcon.setStyle("-fx-font-size: 14px;");
        Label likeCount  = new Label(thread.getLikes() + " likes");
        likeCount.getStyleClass().add("footer-text");
        h.getChildren().addAll(heartIcon, likeCount);

        h.setOnMouseClicked(e -> {
            e.consume();
            if (!requireLogin()) return;
            boolean liked = threadService.likeThread(thread.getThreadId(), currentUser.getId());
            heartIcon.setText(liked ? "❤️" : "🤍");
            ForumThread updated = threadService.getById(thread.getThreadId());
            if (updated != null) {
                likeCount.setText(updated.getLikes() + " likes");
                thread.setLikes(updated.getLikes());
                thread.setLikedBy(updated.getLikedBy());
            }
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), heartIcon);
            scale.setFromX(1.0); scale.setFromY(1.0);
            scale.setToX(1.5);   scale.setToY(1.5);
            scale.setCycleCount(2); scale.setAutoReverse(true); scale.play();
        });
        h.setOnMouseEntered(e -> {
            ScaleTransition hover = new ScaleTransition(Duration.millis(100), h);
            hover.setToX(1.1); hover.setToY(1.1); hover.play();
        });
        h.setOnMouseExited(e -> {
            ScaleTransition hover = new ScaleTransition(Duration.millis(100), h);
            hover.setToX(1.0); hover.setToY(1.0); hover.play();
        });
        return h;
    }

    private HBox buildActions(ForumThread thread) {
        HBox box = new HBox(10);
        box.getStyleClass().add("thread-actions");
        box.setAlignment(Pos.CENTER_LEFT);
        boolean isOwner = currentUser != null
                && currentUser.getId() == thread.getUser().getId();
        if (isOwner) {
            Button editBtn = new Button("✏ Modifier");
            editBtn.getStyleClass().addAll("action-btn", "edit-btn");
            editBtn.setOnAction(e -> { e.consume(); openEditWindow(thread); });
            editBtn.setOnMouseClicked(e -> e.consume());
            Button deleteBtn = new Button("🗑 Supprimer");
            deleteBtn.getStyleClass().addAll("action-btn", "delete-btn");
            deleteBtn.setOnAction(e -> { e.consume(); handleDelete(thread); });
            deleteBtn.setOnMouseClicked(e -> e.consume());
            box.getChildren().addAll(editBtn, deleteBtn);
        }
        Button viewBtn = new Button("👁 Voir");
        viewBtn.getStyleClass().addAll("action-btn", "view-btn");
        viewBtn.setOnAction(e -> { e.consume(); openViewWindow(thread); });
        viewBtn.setOnMouseClicked(e -> e.consume());
        Button commentBtn = new Button("💬 Commenter");
        commentBtn.getStyleClass().addAll("action-btn", "comment-btn");
        commentBtn.setOnAction(e -> { e.consume(); handleAddComment(thread); });
        commentBtn.setOnMouseClicked(e -> e.consume());
        Button summaryBtn = new Button("📄 Résumé IA");
        summaryBtn.getStyleClass().addAll("action-btn", "view-btn");
        summaryBtn.setStyle("-fx-background-color:#7c3aed;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:20;-fx-cursor:hand;");
        summaryBtn.setOnAction(e -> { e.consume(); openAISummary(thread); });
        summaryBtn.setOnMouseClicked(e -> e.consume());
        box.getChildren().addAll(viewBtn, commentBtn, summaryBtn);
        return box;
    }

    // ====================== AI SUMMARY ================

    private void openAISummary(ForumThread thread) {
        // Show loading dialog while Gemini processes
        Stage loadingStage = new Stage();
        loadingStage.initModality(Modality.APPLICATION_MODAL);
        loadingStage.initOwner(threadsContainer.getScene().getWindow());
        loadingStage.setTitle("🤖 Analyse IA");
        VBox loadBox = new VBox(16);
        loadBox.setAlignment(Pos.CENTER);
        loadBox.setPadding(new Insets(30));
        loadBox.setStyle("-fx-background-color:#1e1e2e;");
        Label spinner = new Label("⏳");
        spinner.setStyle("-fx-font-size:36px;");
        Label msg = new Label("L'IA analyse la discussion...");
        msg.setStyle("-fx-text-fill:#cdd6f4;-fx-font-size:14px;");
        loadBox.getChildren().addAll(spinner, msg);
        loadingStage.setScene(new Scene(loadBox, 300, 130));
        loadingStage.show();

        // Animate spinner
        RotateTransition spin = new RotateTransition(Duration.millis(1000), spinner);
        spin.setByAngle(360); spin.setCycleCount(Timeline.INDEFINITE); spin.play();

        new Thread(() -> {
            try {
                List<Response> responses = responseService.getByThread(thread.getThreadId());
                String summary = geminiService1.summarizeDiscussion(thread, responses);
                Platform.runLater(() -> {
                    spin.stop();
                    loadingStage.close();
                    showSummaryPopup(thread, summary, responses.size());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    spin.stop();
                    loadingStage.close();
                    alert(Alert.AlertType.ERROR, "Erreur IA", "Impossible de générer le résumé:\n" + e.getMessage());
                });
            }
        }).start();
    }

    private void showSummaryPopup(ForumThread thread, String summary, int responseCount) {
        Stage stage = new Stage();
        stage.setTitle("📄 Résumé IA");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(threadsContainer.getScene().getWindow());

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color:#1e1e2e;");

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label aiIcon = new Label("🤖");
        aiIcon.setStyle("-fx-font-size:28px;");
        VBox titleBox = new VBox(2);
        Label titleLbl = new Label("Résumé IA");
        titleLbl.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#cdd6f4;");
        Label subtitleLbl = new Label("Discussion analysée • " + responseCount + " réponse(s)");
        subtitleLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#6c7086;");
        titleBox.getChildren().addAll(titleLbl, subtitleLbl);
        header.getChildren().addAll(aiIcon, titleBox);

        // Thread title chip
        Label threadChip = new Label("📌 " + thread.getTitre());
        threadChip.setStyle("-fx-background-color:#313244;-fx-text-fill:#89b4fa;" +
                "-fx-padding:6 12 6 12;-fx-background-radius:20;-fx-font-size:12px;");
        threadChip.setWrapText(true);

        // Summary bullet points
        VBox summaryBox = new VBox(10);
        summaryBox.setStyle("-fx-background-color:#181825;-fx-background-radius:12;-fx-padding:18;");
        for (String line : summary.split("\n")) {
            if (line.trim().isEmpty()) continue;
            Label lineLbl = new Label(line.trim());
            lineLbl.setWrapText(true);
            lineLbl.setMaxWidth(500);
            if (line.trim().startsWith("-")) {
                lineLbl.setStyle("-fx-font-size:13px;-fx-text-fill:#cdd6f4;-fx-padding:3 0 3 0;");
            } else {
                lineLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#6c7086;-fx-font-style:italic;");
            }
            summaryBox.getChildren().add(lineLbl);
        }
        ScrollPane scroll = new ScrollPane(summaryBox);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(280);
        scroll.setStyle("-fx-background:transparent;-fx-background-color:transparent;");

        // Close button
        Button closeBtn = new Button("✓ Fermer");
        closeBtn.setStyle("-fx-background-color:#89b4fa;-fx-text-fill:#1e1e2e;" +
                "-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:8 24;-fx-cursor:hand;");
        closeBtn.setOnAction(e -> stage.close());
        HBox btnRow = new HBox(closeBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(header, threadChip, new Separator(), scroll, btnRow);
        stage.setScene(new Scene(root, 560, 460));
        stage.show();
    }

    private void showEmptyState() {
        VBox empty = new VBox(16);
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new Insets(60));
        String msg = showingMyThreads ? "🌾 Vous n'avez pas encore créé de sujet" : "🌾 Aucun sujet trouvé";
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-font-size:18px;-fx-text-fill:#888;");
        Button btn = new Button("➕ Créer un sujet");
        btn.getStyleClass().add("add-btn");
        btn.setOnAction(e -> handleAddThread());
        empty.getChildren().addAll(lbl, btn);
        threadsContainer.getChildren().add(empty);
    }

    // ====================== FXML HANDLERS =============

    @FXML
    private void handleAddThread() {
        if (!requireLogin()) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterThread.fxml"));
            Parent root = loader.load();
            ForumAddThreadController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            Stage stage = new Stage();
            stage.setTitle("➕ Nouveau sujet");
            stage.setScene(new Scene(root, 800, 600));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(threadsContainer.getScene().getWindow());
            stage.setOnHidden(e -> refresh());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            alert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleMesSujets() {
        if (!requireLogin()) return;
        showingMyThreads = true;
        loadUserCache(); loadAllThreads(); applyFilters();
    }

    @FXML
    private void handleTousLesSujets() {
        showingMyThreads = false;
        searchField.clear();
        filterComboBox.setValue("All");
        loadUserCache(); loadAllThreads(); applyFilters();
    }

    // ====================== EDIT ======================

    private void openEditWindow(ForumThread thread) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierThread.fxml"));
            Parent root = loader.load();
            ForumEditThreadController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            ctrl.setThread(thread);
            Stage stage = new Stage();
            stage.setTitle("✏ Modifier: " + thread.getTitre());
            stage.setScene(new Scene(root, 800, 600));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(threadsContainer.getScene().getWindow());
            stage.setOnHidden(e -> refresh());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            alert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir l'éditeur:\n" + e.getMessage());
        }
    }

    // ====================== DELETE ====================

    private void handleDelete(ForumThread thread) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer le sujet");
        confirm.setHeaderText(null);
        confirm.setContentText("⚠️ Supprimer « " + thread.getTitre() + " » ?\nCette action est irréversible.");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                threadService.delete(thread.getThreadId());
                allThreads.removeIf(t -> t.getThreadId() == thread.getThreadId());
                applyFilters();
                alert(Alert.AlertType.INFORMATION, "Succès", "✅ Sujet supprimé.");
            } catch (Exception e) {
                e.printStackTrace();
                alert(Alert.AlertType.ERROR, "Erreur suppression", "Impossible de supprimer:\n" + e.getMessage());
            }
        }
    }

    // ====================== VIEW ======================

    private void openViewWindow(ForumThread thread) {
        if (!requireLogin()) return;
        threadService.incrementViews(thread.getThreadId());
        ForumThread updatedThread = threadService.getById(thread.getThreadId());
        if (updatedThread != null) thread.setViews(updatedThread.getViews());
        try {
            String[] possiblePaths = {
                    "/ViewCommentaires.fxml", "/ForumView.fxml",
                    "/ViewCommentaire.fxml",  "/view_commentaires.fxml"
            };
            URL fxmlUrl = null;
            for (String path : possiblePaths) {
                fxmlUrl = getClass().getResource(path);
                if (fxmlUrl != null) break;
            }
            if (fxmlUrl == null) {
                alert(Alert.AlertType.ERROR, "Erreur", "ViewCommentaires.fxml introuvable.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            ViewCommentairesController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            ctrl.setThread(updatedThread != null ? updatedThread : thread);
            Stage stage = new Stage();
            stage.setTitle("💬 " + thread.getTitre());
            stage.setScene(new Scene(root, 1080, 720));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(threadsContainer.getScene().getWindow());
            stage.setOnHidden(e -> refresh());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            alert(Alert.AlertType.ERROR, "Erreur navigation", "Impossible d'ouvrir la vue:\n" + e.getMessage());
        }
    }

    // ====================== COMMENT ===================

    private void handleAddComment(ForumThread thread) {
        if (!requireLogin()) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AddCommentaire.fxml"));
            Parent root = loader.load();
            AddCommentaireController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            ctrl.setThread(thread);
            Stage stage = new Stage();
            stage.setTitle("💬 Nouveau commentaire");
            stage.setScene(new Scene(root, 580, 480));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(threadsContainer.getScene().getWindow());
            stage.setOnHidden(e -> refresh());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            alert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire:\n" + e.getMessage());
        }
    }

    // ====================== HELPERS ===================

    private boolean requireLogin() {
        if (currentUser != null) return true;
        alert(Alert.AlertType.WARNING, "Non connecté", "Veuillez vous connecter pour cette action.");
        return false;
    }

    private void alert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("✅ User set: " + (user != null ? user.getNom() : "null"));
        updateUserHeader();
        updateNotificationBadge();
        if (user != null) startNotificationRefreshTimer();
        else              stopNotificationRefreshTimer();
        applyFilters();
    }

    private void updateUserHeader() {
        if (userNameLabel == null || userStatusLabel == null) return;
        if (currentUser != null) {
            userNameLabel.setText("👤 " + currentUser.getNom());
            switch (currentUser.getEtatCompte()) {
                case ACTIF:
                    userStatusLabel.setText("⬤ Actif");
                    userStatusLabel.setStyle("-fx-text-fill:#4CAF50;-fx-font-size:11px;-fx-font-weight:bold;");
                    break;
                case BLOQUE:
                    userStatusLabel.setText("⬤ Bloqué");
                    userStatusLabel.setStyle("-fx-text-fill:#F44336;-fx-font-size:11px;-fx-font-weight:bold;");
                    break;
                default:
                    userStatusLabel.setText("⬤ " + currentUser.getEtatCompte().name());
                    userStatusLabel.setStyle("-fx-text-fill:#888;-fx-font-size:11px;-fx-font-weight:bold;");
                    break;
            }
            if (sidebarRoleLabel != null) {
                sidebarRoleLabel.setText(getRoleDisplayText(currentUser.getRole()));
                sidebarRoleLabel.setStyle("-fx-text-fill:#4CAF50;-fx-font-size:11px;-fx-font-weight:bold;");
            }
        } else {
            userNameLabel.setText("👤 —");
            userStatusLabel.setText("⬤ Non connecté");
            userStatusLabel.setStyle("-fx-text-fill:#888;-fx-font-size:11px;");
            if (sidebarRoleLabel != null) {
                sidebarRoleLabel.setText("👤 Visiteur");
                sidebarRoleLabel.setStyle("-fx-text-fill:#888;-fx-font-size:11px;");
            }
        }
    }

    private String getRoleDisplayText(Role role) {
        if (role == null) return "👤 Utilisateur";
        switch (role) {
            case AGRICULTEUR: return "🌱 Agriculteur";
            case EXPERT:      return "🎓 Expert";
            case FOURNISSEUR: return "📦 Fournisseur";
            case ADMIN:       return "👑 Administrateur";
            default:          return "👤 " + role.name();
        }
    }
}