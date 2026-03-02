package controller;

import entities.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import service.GeminiService1;
import service.MediaService;
import service.ResponseService;
import service.UserService;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ViewCommentairesController {

    // ====================== FXML ======================
    @FXML private Label      threadTitleLabel;
    @FXML private Label      threadAuthorLabel;
    @FXML private Label      threadDateLabel;
    @FXML private Label      threadContentLabel;
    @FXML private Label      commentCountLabel;
    @FXML private Label      dividerLabel;
    @FXML private ScrollPane commentsScrollPane;
    @FXML private VBox       commentsContainer;

    // ====================== STATE =====================
    private ForumThread       currentThread;
    private User              currentUser;
    private Map<Integer, User> userCache = new HashMap<>();

    // ====================== SERVICES ==================
    private final ResponseService responseService = new ResponseService();
    private final MediaService    mediaService    = new MediaService();
    private final UserService     userService     = new UserService();
    private final GeminiService1   geminiService1   = new GeminiService1();

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ====================== INIT ======================

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void setThread(ForumThread thread) {
        this.currentThread = thread;
        if (thread == null) return;

        // Populate thread preview
        threadTitleLabel.setText("→ " + thread.getTitre());
        threadContentLabel.setText(thread.getContenu());
        threadDateLabel.setText(thread.getDateCreation().format(DATE_FMT));

        // Load user cache then load responses
        loadUserCache();
        loadAuthorName(thread);
        loadComments();
    }

    private void loadUserCache() {
        try {
            List<User> users = userService.getAll();
            userCache.clear();
            for (User u : users) userCache.put(u.getId(), u);
        } catch (Exception e) {
            System.err.println("UserCache error: " + e.getMessage());
        }
    }

    private void loadAuthorName(ForumThread thread) {
        User author = userCache.get(thread.getUser().getId());
        String name = (author != null && author.getNom() != null)
                ? "👤 " + author.getNom()
                : "👤 User #" + thread.getUser().getId();
        threadAuthorLabel.setText(name);
    }

    // ====================== LOAD COMMENTS =============

    private void loadComments() {
        commentsContainer.getChildren().clear();
        if (currentThread == null) return;

        try {
            List<Response> responses = responseService.getAll();
            // Filter to this thread only
            List<Response> threadResponses = responses.stream()
                    .filter(r -> r.getThread().getThreadId() == currentThread.getThreadId())
                    .sorted((a, b) -> a.getDateCreation().compareTo(b.getDateCreation()))
                    .collect(java.util.stream.Collectors.toList());

            int count = threadResponses.size();
            commentCountLabel.setText(count + " réponse" + (count > 1 ? "s" : ""));
            dividerLabel.setText("── " + count + " réponse" + (count > 1 ? "s" : "") + " ──");

            if (threadResponses.isEmpty()) {
                showEmptyState();
                return;
            }

            for (Response r : threadResponses) {
                VBox card = buildCommentCard(r);
                commentsContainer.getChildren().add(card);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Load comments error: " + e.getMessage());
        }
    }

    // ====================== CARD BUILDER ==============

    private VBox buildCommentCard(Response response) {
        System.out.println("\n════════════════════════════════════════════════");
        System.out.println("🔨 Building comment card for Response #" + response.getResponseId());
        System.out.println("   User: " + response.getUser().getId());
        System.out.println("   Content: " + response.getContenu().substring(0, Math.min(50, response.getContenu().length())) + "...");

        VBox card = new VBox(0);
        card.getStyleClass().add("comment-card");

        // ── Media section (image or video) ──────────────
        System.out.println("📸 Attempting to load media for Response #" + response.getResponseId());
        VBox mediaSection = buildMediaSection(response);
        if (mediaSection != null) {
            System.out.println("✅ Media section created! Adding to card...");
            card.getChildren().add(mediaSection);
            System.out.println("✅ Media section added to card. Children count: " + card.getChildren().size());
        } else {
            System.out.println("⚠️ No media section returned (null)");
        }

        // ── Card body ────────────────────────────────────
        VBox body = new VBox(10);
        body.getStyleClass().add("comment-card-body");
        body.setPadding(new Insets(14));

        // Author + date row
        HBox metaRow = new HBox(12);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        // Author avatar circle
        Label avatar = new Label();
        User author = userCache.get(response.getUser().getId());
        String authorName = (author != null && author.getNom() != null)
                ? author.getNom()
                : "User #" + response.getUser().getId();

        // First letter for avatar
        String initials = authorName.length() > 0
                ? String.valueOf(authorName.charAt(0)).toUpperCase()
                : "?";
        avatar.setText(initials);
        avatar.getStyleClass().add("comment-avatar");

        VBox authorInfo = new VBox(2);
        Label nameLbl = new Label("👤 " + authorName);
        nameLbl.getStyleClass().add("comment-author");
        Label dateLbl = new Label("📅 " + response.getDateCreation().format(DATE_FMT));
        dateLbl.getStyleClass().add("comment-date");
        authorInfo.getChildren().addAll(nameLbl, dateLbl);

        metaRow.getChildren().addAll(avatar, authorInfo);

        // Content
        Label contentLbl = new Label(response.getContenu());
        contentLbl.getStyleClass().add("comment-content");
        contentLbl.setWrapText(true);

        // ✅ NEW: Action buttons for owner
        HBox actionButtons = buildActionButtons(response);

        body.getChildren().addAll(metaRow, new Separator(), contentLbl);

        // ✅ Add action buttons if user owns this response
        if (actionButtons != null) {
            body.getChildren().add(actionButtons);
        }

        card.getChildren().add(body);

        System.out.println("📦 Card complete! Total sections: " + card.getChildren().size());
        System.out.println("   - Has media: " + (mediaSection != null ? "YES" : "NO"));
        System.out.println("   - Has body: YES");
        System.out.println("════════════════════════════════════════════════\n");

        return card;
    }

    // ✅ NEW: Build action buttons for response owner
    private HBox buildActionButtons(Response response) {
        // Only show buttons if current user owns this response
        if (currentUser == null) return null;
        if (response.getUser().getId() != currentUser.getId()) return null;

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        // Edit button
        Button editBtn = new Button("✏ Modifier");
        editBtn.getStyleClass().addAll("action-btn", "edit-btn");
        editBtn.setOnAction(e -> handleEditResponse(response));

        // Delete button
        Button deleteBtn = new Button("🗑 Supprimer");
        deleteBtn.getStyleClass().addAll("action-btn", "delete-btn");
        deleteBtn.setOnAction(e -> handleDeleteResponse(response));

        buttonBox.getChildren().addAll(editBtn, deleteBtn);
        return buttonBox;
    }

    // ✅ Opens ModifierResponse.fxml window
    private void handleEditResponse(Response response) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ModifierResponse.fxml")
            );
            Parent root = loader.load();

            ModifierResponseController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            ctrl.setResponse(response);
            ctrl.setOnSaveCallback(this::loadComments);

            Stage stage = new Stage();
            stage.setTitle("✏ Modifier le commentaire");
            stage.setScene(new Scene(root, 600, 400));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(commentsContainer.getScene().getWindow());
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir l'editeur:\n" + e.getMessage());
        }
    }

    // ✅ NEW: Handle delete response
    private void handleDeleteResponse(Response response) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer le commentaire");
        confirm.setHeaderText(null);
        confirm.setContentText("⚠️ Êtes-vous sûr de vouloir supprimer ce commentaire ?\n" +
                "Cette action est irréversible.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Delete associated media first
                List<Media> mediaList = mediaService.getMediaByResponse(response.getResponseId());
                if (mediaList != null) {
                    for (Media media : mediaList) {
                        mediaService.delete(media.getMediaId());
                    }
                }

                // Delete the response
                responseService.delete(response.getResponseId());

                // Reload comments
                loadComments();

                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "✅ Commentaire supprimé.");

            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur suppression",
                        "Impossible de supprimer le commentaire:\n" + e.getMessage());
            }
        }
    }

    // ── Media section ──────────────────────────────────
    private VBox buildMediaSection(Response response) {
        try {
            List<Media> mediaList = mediaService.getMediaByResponse(response.getResponseId());

            if (mediaList == null || mediaList.isEmpty()) {
                System.out.println("📸 No media found for response #" + response.getResponseId());
                return null;
            }

            System.out.println("📸 Found " + mediaList.size() + " media item(s) for response #" + response.getResponseId());

            for (Media media : mediaList) {
                if (media == null || media.getUrl() == null || media.getUrl().trim().isEmpty()) {
                    System.err.println("⚠️ Invalid media object or empty URL");
                    continue;
                }

                VBox container = new VBox();
                container.setAlignment(Pos.CENTER);
                container.getStyleClass().add("comment-media-container");

                // ═══════════════════════════════════════════════════════
                //                    IMAGE HANDLING
                // ═══════════════════════════════════════════════════════
                if (media.getType() == MediaType.IMAGE) {
                    System.out.println("🖼️ Loading image: " + media.getUrl());
                    File imgFile = new File(media.getUrl());

                    if (!imgFile.exists()) {
                        System.err.println("❌ Image file not found: " + media.getUrl());
                        // Show error placeholder
                        container.getChildren().add(createImageErrorPlaceholder(imgFile.getName()));
                        return container;
                    }

                    try {
                        Image img = new Image(imgFile.toURI().toString(), 1000, 300, true, true);

                        if (img.isError()) {
                            System.err.println("❌ Image loading error for: " + media.getUrl());
                            container.getChildren().add(createImageErrorPlaceholder(imgFile.getName()));
                            return container;
                        }

                        ImageView iv = new ImageView(img);
                        iv.setFitHeight(300);
                        iv.setFitWidth(1000);
                        iv.setPreserveRatio(true);
                        iv.setSmooth(true);
                        iv.getStyleClass().add("comment-image");

                        // Add click to view full size
                        iv.setOnMouseClicked(e -> openImageFullSize(img, imgFile.getName()));
                        iv.setStyle("-fx-cursor: hand;");

                        container.getChildren().add(iv);
                        System.out.println("✅ Image loaded successfully: " + imgFile.getName());
                        return container;

                    } catch (Exception e) {
                        System.err.println("❌ Exception loading image: " + e.getMessage());
                        e.printStackTrace();
                        container.getChildren().add(createImageErrorPlaceholder(imgFile.getName()));
                        return container;
                    }
                }

                // ═══════════════════════════════════════════════════════
                //                    VIDEO HANDLING
                // ═══════════════════════════════════════════════════════
                else if (media.getType() == MediaType.VIDEO) {
                    System.out.println("🎬 Loading video placeholder: " + media.getUrl());
                    File vFile = new File(media.getUrl());

                    VBox videoPh = new VBox(12);
                    videoPh.setAlignment(Pos.CENTER);
                    videoPh.getStyleClass().add("video-placeholder");
                    videoPh.setPrefHeight(180);
                    videoPh.setMinHeight(180);

                    // Video icon
                    Label icon = new Label("🎬");
                    icon.setStyle("-fx-font-size: 50px;");

                    // Video filename
                    Label name = new Label(vFile.getName());
                    name.getStyleClass().add("video-name-label");
                    name.setWrapText(true);
                    name.setMaxWidth(700);
                    name.setStyle("-fx-text-alignment: center;");

                    // File info
                    String fileSize = vFile.exists()
                            ? String.format("%.2f MB", vFile.length() / (1024.0 * 1024.0))
                            : "Fichier non trouvé";

                    Label hint = new Label("Fichier vidéo • " + fileSize);
                    hint.getStyleClass().add("video-hint-label");

                    // Add click action if file exists
                    if (vFile.exists()) {
                        Label clickHint = new Label("🖱️ Cliquez pour ouvrir");
                        clickHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #90caf9;");
                        videoPh.setOnMouseClicked(e -> openVideoFile(vFile));
                        videoPh.setStyle("-fx-cursor: hand;");
                        videoPh.getChildren().addAll(icon, name, hint, clickHint);
                    } else {
                        Label errorLabel = new Label("⚠️ Fichier introuvable");
                        errorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ef5350;");
                        videoPh.getChildren().addAll(icon, name, errorLabel);
                    }

                    container.getChildren().add(videoPh);
                    System.out.println("✅ Video placeholder created: " + vFile.getName());
                    return container;
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Media load error for response #" + response.getResponseId());
            e.printStackTrace();
        }

        return null;
    }

    // ═══════════════════════════════════════════════════════
    //              HELPER METHODS FOR MEDIA
    // ═══════════════════════════════════════════════════════

    private VBox createImageErrorPlaceholder(String filename) {
        VBox errorBox = new VBox(10);
        errorBox.setAlignment(Pos.CENTER);
        errorBox.getStyleClass().add("comment-media-container");
        errorBox.setPrefHeight(120);
        errorBox.setStyle("-fx-background-color: #ffebee; -fx-border-color: #ef5350; -fx-border-width: 2; -fx-border-radius: 8;");

        Label icon = new Label("⚠️");
        icon.setStyle("-fx-font-size: 36px;");

        Label errorMsg = new Label("Image introuvable");
        errorMsg.setStyle("-fx-font-size: 14px; -fx-text-fill: #c62828; -fx-font-weight: bold;");

        Label filenameLabel = new Label(filename);
        filenameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        errorBox.getChildren().addAll(icon, errorMsg, filenameLabel);
        return errorBox;
    }

    private void openImageFullSize(Image img, String filename) {
        try {
            ImageView fullImageView = new ImageView(img);
            fullImageView.setPreserveRatio(true);
            fullImageView.setFitWidth(800);
            fullImageView.setFitHeight(600);

            StackPane root = new StackPane(fullImageView);
            root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.9);");
            root.setOnMouseClicked(e -> ((Stage) root.getScene().getWindow()).close());

            Stage stage = new Stage();
            stage.setTitle("🖼️ " + filename);
            stage.setScene(new Scene(root, 800, 600));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();

            System.out.println("✅ Opened full-size image: " + filename);
        } catch (Exception e) {
            System.err.println("❌ Error opening full-size image: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openVideoFile(File videoFile) {
        try {
            // Try to open with system's default video player
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(videoFile);
                System.out.println("✅ Opened video with system player: " + videoFile.getName());
            } else {
                showAlert(Alert.AlertType.WARNING, "Non supporté",
                        "Impossible d'ouvrir la vidéo.\nChemin: " + videoFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("❌ Error opening video: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir la vidéo:\n" + e.getMessage());
        }
    }

    // ── Empty state ────────────────────────────────────
    private void showEmptyState() {
        VBox empty = new VBox(14);
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new Insets(50));

        Label icon = new Label("🌾");
        icon.setStyle("-fx-font-size:40px;");
        Label lbl = new Label("Aucun commentaire pour l'instant.\nSoyez le premier à répondre !");
        lbl.setStyle("-fx-font-size:15px;-fx-text-fill:#888;-fx-text-alignment:center;");
        lbl.setWrapText(true);

        Button btn = new Button("➕ Ajouter un commentaire");
        btn.getStyleClass().add("btn-submit");
        btn.setOnAction(e -> openAddComment());

        empty.getChildren().addAll(icon, lbl, btn);
        commentsContainer.getChildren().add(empty);
    }

    // ====================== HANDLERS ==================

    @FXML
    private void openAddComment() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Non connecté",
                    "Veuillez vous connecter pour commenter.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/AddCommentaire.fxml")
            );
            Parent root = loader.load();

            AddCommentaireController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            ctrl.setThread(currentThread);

            Stage stage = new Stage();
            stage.setTitle("💬 Nouveau commentaire");
            stage.setScene(new Scene(root, 580, 480));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(commentsContainer.getScene().getWindow());
            // Refresh comments after adding
            stage.setOnHidden(e -> loadComments());
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir le formulaire:\n" + e.getMessage());
        }
    }

    // ====================== AI SUMMARY ================

    @FXML
    private void summarizeDiscussion() {
        if (currentThread == null) return;

        Stage loadingStage = new Stage();
        loadingStage.initModality(Modality.APPLICATION_MODAL);
        loadingStage.initOwner(commentsContainer.getScene().getWindow());
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

        new Thread(() -> {
            try {
                List<Response> responses = responseService.getAll().stream()
                        .filter(r -> r.getThread().getThreadId() == currentThread.getThreadId())
                        .sorted((a, b) -> a.getDateCreation().compareTo(b.getDateCreation()))
                        .collect(java.util.stream.Collectors.toList());

                String summary = geminiService1.summarizeDiscussion(currentThread, responses);

                javafx.application.Platform.runLater(() -> {
                    loadingStage.close();
                    showSummaryDialog(summary, responses.size());
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    loadingStage.close();
                    showAlert(Alert.AlertType.ERROR, "Erreur IA",
                            "Impossible de générer le résumé:\n" + e.getMessage());
                });
            }
        }).start();
    }

    private void showSummaryDialog(String summary, int responseCount) {
        Stage stage = new Stage();
        stage.setTitle("📄 Résumé IA de la discussion");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(commentsContainer.getScene().getWindow());

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
        Label threadChip = new Label("📌 " + currentThread.getTitre());
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

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) commentsContainer.getScene().getWindow();
        stage.close();
    }

    // ====================== HELPERS ===================

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}