package com.eventmanagement.controllers;

import com.eventmanagement.dao.EvenementDAO;
import com.eventmanagement.dao.ReclamationDAO;
import com.eventmanagement.dao.UtilisateurDAO;
import com.eventmanagement.models.Evenement;
import com.eventmanagement.models.Reclamation;
import com.eventmanagement.models.Utilisateur;
import com.eventmanagement.utils.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AdminDashboardControllerV2 {
    
    @FXML private TabPane mainTabPane;
    @FXML private Tab eventsTab;
    @FXML private Tab claimsTab;
    @FXML private Tab usersTab;
    @FXML private Tab statsTab;
    
    // Events Tab
    @FXML private TableView<Evenement> eventsTable;
    @FXML private TableColumn<Evenement, String> eventTitleCol;
    @FXML private TableColumn<Evenement, String> eventStatusCol;
    @FXML private TextField eventSearchField;
    @FXML private ComboBox<String> eventStatusFilter;
    @FXML private Label eventStatsLabel;
    
    // Claims Tab
    @FXML private TableView<Reclamation> claimsTable;
    @FXML private TableColumn<Reclamation, String> claimSubjectCol;
    @FXML private TableColumn<Reclamation, String> claimStatusCol;
    @FXML private TextField claimSearchField;
    @FXML private ComboBox<String> claimStatusFilter;
    
    // Users Tab
    @FXML private TableView<Utilisateur> usersTable;
    @FXML private TableColumn<Utilisateur, String> userNameCol;
    @FXML private TableColumn<Utilisateur, String> userRoleCol;
    @FXML private TextField userSearchField;
    
    // Statistics
    @FXML private Label totalEventsLabel;
    @FXML private Label approvedEventsLabel;
    @FXML private Label pendingEventsLabel;
    @FXML private Label totalClaimsLabel;
    @FXML private Label resolvedClaimsLabel;
    @FXML private Label totalUsersLabel;
    
    private EvenementDAO evenementDAO = new EvenementDAO();
    private ReclamationDAO reclamationDAO = new ReclamationDAO();
    private UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    
    private static final int PAGE_SIZE = 20;
    private int currentEventPage = 1;
    private int currentClaimPage = 1;
    
    @FXML
    public void initialize() {
        setupEventsTables();
        setupClaimsTables();
        setupUsersTables();
        loadStatistics();
        loadEvents();
        loadClaims();
        loadUsers();
    }
    
    // ==================== EVENTS MANAGEMENT ====================
    private void setupEventsTables() {
        eventStatusFilter.getItems().addAll("TOUS", "APPROUVE", "EN_ATTENTE", "REJETE");
        eventStatusFilter.setValue("TOUS");
        eventStatusFilter.setOnAction(e -> loadEventsByFilter());
    }
    
    private void loadEvents() {
        List<Evenement> evenements = evenementDAO.getAll();
        eventsTable.getItems().clear();
        eventsTable.getItems().addAll(evenements);
        updateEventStatistics();
    }
    
    private void loadEventsByFilter() {
        String status = eventStatusFilter.getValue();
        String search = eventSearchField.getText();
        
        List<Evenement> evenements;
        if ("TOUS".equals(status)) {
            if (search != null && !search.isEmpty()) {
                evenements = evenementDAO.searchAll(search);
            } else {
                evenements = evenementDAO.getAll();
            }
        } else {
            if (search != null && !search.isEmpty()) {
                List<Evenement> allByStatus = evenementDAO.getByStatut(status);
                evenements = allByStatus.stream()
                    .filter(e -> e.getTitre().toLowerCase().contains(search.toLowerCase()))
                    .toList();
            } else {
                evenements = evenementDAO.getByStatut(status);
            }
        }
        
        eventsTable.getItems().clear();
        eventsTable.getItems().addAll(evenements);
    }
    
    @FXML
    private void onSearchEvents() {
        loadEventsByFilter();
    }
    
    @FXML
    private void onAddEvent() {
        Dialog<Evenement> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un Événement");
        dialog.setHeaderText("Créer un nouvel événement");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField titleField = new TextField();
        titleField.setPromptText("Titre");
        TextArea descArea = new TextArea();
        descArea.setPromptText("Description");
        descArea.setPrefRowCount(3);
        TextField locationField = new TextField();
        locationField.setPromptText("Lieu");
        TextField capacityField = new TextField();
        capacityField.setPromptText("Capacité Max");
        DatePicker startDatePicker = new DatePicker();
        startDatePicker.setPromptText("Date de début");
        DatePicker endDatePicker = new DatePicker();
        endDatePicker.setPromptText("Date de fin");
        
        grid.add(new Label("Titre:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descArea, 1, 1);
        grid.add(new Label("Lieu:"), 0, 2);
        grid.add(locationField, 1, 2);
        grid.add(new Label("Capacité Max:"), 0, 3);
        grid.add(capacityField, 1, 3);
        grid.add(new Label("Date Début:"), 0, 4);
        grid.add(startDatePicker, 1, 4);
        grid.add(new Label("Date Fin:"), 0, 5);
        grid.add(endDatePicker, 1, 5);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                Evenement event = new Evenement();
                event.setTitre(titleField.getText());
                event.setDescription(descArea.getText());
                event.setLieu(locationField.getText());
                event.setCapaciteMax(Integer.parseInt(capacityField.getText()));
                event.setDateDebut(startDatePicker.getValue());
                event.setDateFin(endDatePicker.getValue());
                event.setOrganisateurId(Session.getInstance().getCurrentUser().getId());
                event.setStatut("EN_ATTENTE");
                return event;
            }
            return null;
        });
        
        Optional<Evenement> result = dialog.showAndWait();
        result.ifPresent(event -> {
            String error = EventValidationHelper.validateEvent(event);
            if (error == null) {
                if (evenementDAO.create(event)) {
                    showSuccessAlert("Événement créé avec succès");
                    loadEvents();
                }
            } else {
                showErrorAlert(error);
            }
        });
    }
    
    @FXML
    private void onEditEvent() {
        Evenement selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarningAlert("Sélectionnez un événement à modifier");
            return;
        }
        
        Dialog<Evenement> dialog = new Dialog<>();
        dialog.setTitle("Modifier l'Événement");
        dialog.setHeaderText("Modifier les détails de l'événement");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField titleField = new TextField(selected.getTitre());
        TextArea descArea = new TextArea(selected.getDescription());
        descArea.setPrefRowCount(3);
        TextField locationField = new TextField(selected.getLieu());
        TextField capacityField = new TextField(String.valueOf(selected.getCapaciteMax()));
        DatePicker startDatePicker = new DatePicker(selected.getDateDebut());
        DatePicker endDatePicker = new DatePicker(selected.getDateFin());
        
        grid.add(new Label("Titre:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descArea, 1, 1);
        grid.add(new Label("Lieu:"), 0, 2);
        grid.add(locationField, 1, 2);
        grid.add(new Label("Capacité Max:"), 0, 3);
        grid.add(capacityField, 1, 3);
        grid.add(new Label("Date Début:"), 0, 4);
        grid.add(startDatePicker, 1, 4);
        grid.add(new Label("Date Fin:"), 0, 5);
        grid.add(endDatePicker, 1, 5);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                selected.setTitre(titleField.getText());
                selected.setDescription(descArea.getText());
                selected.setLieu(locationField.getText());
                selected.setCapaciteMax(Integer.parseInt(capacityField.getText()));
                selected.setDateDebut(startDatePicker.getValue());
                selected.setDateFin(endDatePicker.getValue());
                return selected;
            }
            return null;
        });
        
        Optional<Evenement> result = dialog.showAndWait();
        result.ifPresent(event -> {
            if (evenementDAO.update(event)) {
                showSuccessAlert("Événement modifié avec succès");
                loadEvents();
            }
        });
    }
    
    @FXML
    private void onDeleteEvent() {
        Evenement selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarningAlert("Sélectionnez un événement à supprimer");
            return;
        }
        
        if (showConfirmationDialog("Êtes-vous sûr de vouloir supprimer cet événement ?")) {
            if (evenementDAO.delete(selected.getIdEvenement())) {
                showSuccessAlert("Événement supprimé avec succès");
                loadEvents();
            }
        }
    }
    
    @FXML
    private void onApproveEvent() {
        Evenement selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarningAlert("Sélectionnez un événement à approver");
            return;
        }
        
        if (evenementDAO.approveEvent(selected.getIdEvenement())) {
            showSuccessAlert("Événement approuvé avec succès");
            loadEvents();
        }
    }
    
    @FXML
    private void onRejectEvent() {
        Evenement selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarningAlert("Sélectionnez un événement à rejeter");
            return;
        }
        
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Rejeter l'Événement");
        dialog.setHeaderText("Entrez la raison du rejet");
        
        TextArea reasonArea = new TextArea();
        reasonArea.setPromptText("Raison du rejet...");
        reasonArea.setPrefRowCount(5);
        reasonArea.setWrapText(true);
        
        dialog.getDialogPane().setContent(reasonArea);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(buttonType -> buttonType == ButtonType.OK ? reasonArea.getText() : null);
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(reason -> {
            if (!reason.isEmpty()) {
                if (evenementDAO.rejectEvent(selected.getIdEvenement(), reason)) {
                    showSuccessAlert("Événement rejeté");
                    loadEvents();
                }
            }
        });
    }
    
    private void updateEventStatistics() {
        long total = evenementDAO.getTotalCount();
        long approved = evenementDAO.getCountByStatut("APPROUVE");
        long pending = evenementDAO.getCountByStatut("EN_ATTENTE");
        
        eventStatsLabel.setText(String.format(
            "Total: %d | Approuvés: %d | En attente: %d", 
            total, approved, pending
        ));
    }
    
    // ==================== CLAIMS MANAGEMENT ====================
    private void setupClaimsTables() {
        claimStatusFilter.getItems().addAll("TOUS", "EN_ATTENTE", "EN_COURS", "TRAITEE", "CLOTUREE");
        claimStatusFilter.setValue("TOUS");
        claimStatusFilter.setOnAction(e -> loadClaimsByFilter());
    }
    
    private void loadClaims() {
        List<Reclamation> reclamations = reclamationDAO.getAll();
        claimsTable.getItems().clear();
        claimsTable.getItems().addAll(reclamations);
    }
    
    private void loadClaimsByFilter() {
        String status = claimStatusFilter.getValue();
        String search = claimSearchField.getText();
        
        List<Reclamation> reclamations;
        if ("TOUS".equals(status)) {
            if (search != null && !search.isEmpty()) {
                reclamations = reclamationDAO.searchAll(search);
            } else {
                reclamations = reclamationDAO.getAll();
            }
        } else {
            if (search != null && !search.isEmpty()) {
                List<Reclamation> allByStatus = reclamationDAO.getByStatut(status);
                reclamations = allByStatus.stream()
                    .filter(c -> c.getObjet().toLowerCase().contains(search.toLowerCase()))
                    .toList();
            } else {
                reclamations = reclamationDAO.getByStatut(status);
            }
        }
        
        claimsTable.getItems().clear();
        claimsTable.getItems().addAll(reclamations);
    }
    
    @FXML
    private void onSearchClaims() {
        loadClaimsByFilter();
    }
    
    @FXML
    private void onViewClaimDetails() {
        Reclamation selected = claimsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarningAlert("Sélectionnez une réclamation");
            return;
        }
        
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Détails de la Réclamation");
        
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));
        vbox.getChildren().addAll(
            new Label("Objet: " + selected.getObjet()),
            new Label("Description: " + selected.getDescription()),
            new Label("Statut: " + selected.getStatut()),
            new Label("Priorité: " + selected.getPriorite()),
            new Label("Type: " + selected.getType()),
            new Label("Date création: " + selected.getDateCreation())
        );
        
        if (selected.getReponseAdmin() != null) {
            vbox.getChildren().add(new Label("Réponse admin: " + selected.getReponseAdmin()));
        }
        
        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
    
    @FXML
    private void onChangeClaimStatus() {
        Reclamation selected = claimsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarningAlert("Sélectionnez une réclamation");
            return;
        }
        
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Changer le Statut");
        
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("EN_ATTENTE", "EN_COURS", "TRAITEE", "CLOTUREE");
        statusCombo.setValue(selected.getStatut());
        
        dialog.getDialogPane().setContent(statusCombo);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(buttonType -> buttonType == ButtonType.OK ? statusCombo.getValue() : null);
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newStatus -> {
            if (reclamationDAO.updateStatut(selected.getIdReclamation(), newStatus)) {
                showSuccessAlert("Statut mis à jour");
                loadClaims();
            }
        });
    }
    
    @FXML
    private void onReplyToClaim() {
        Reclamation selected = claimsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarningAlert("Sélectionnez une réclamation");
            return;
        }
        
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Répondre à la Réclamation");
        
        TextArea replyArea = new TextArea();
        replyArea.setPromptText("Votre réponse...");
        replyArea.setPrefRowCount(5);
        replyArea.setWrapText(true);
        
        dialog.getDialogPane().setContent(replyArea);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(buttonType -> buttonType == ButtonType.OK ? replyArea.getText() : null);
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(reply -> {
            if (!reply.isEmpty()) {
                if (reclamationDAO.repondre(selected.getIdReclamation(), reply)) {
                    showSuccessAlert("Réponse envoyée");
                    loadClaims();
                }
            }
        });
    }
    
    // ==================== USERS MANAGEMENT ====================
    private void setupUsersTables() {
        // Initialize user table
    }
    
    private void loadUsers() {
        List<Utilisateur> users = utilisateurDAO.getAll();
        usersTable.getItems().clear();
        usersTable.getItems().addAll(users);
    }
    
    @FXML
    private void onSearchUsers() {
        String search = userSearchField.getText();
        if (search.isEmpty()) {
            loadUsers();
        } else {
            List<Utilisateur> users = utilisateurDAO.search(search);
            usersTable.getItems().clear();
            usersTable.getItems().addAll(users);
        }
    }
    
    @FXML
    private void onAddUser() {
        Dialog<Utilisateur> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un Utilisateur");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField nomField = new TextField();
        nomField.setPromptText("Nom");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        TextField phoneField = new TextField();
        phoneField.setPromptText("Téléphone");
        TextField addressField = new TextField();
        addressField.setPromptText("Adresse");
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("USER", "ADMIN");
        roleCombo.setValue("USER");
        
        grid.add(new Label("Nom:"), 0, 0);
        grid.add(nomField, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("Téléphone:"), 0, 2);
        grid.add(phoneField, 1, 2);
        grid.add(new Label("Adresse:"), 0, 3);
        grid.add(addressField, 1, 3);
        grid.add(new Label("Rôle:"), 0, 4);
        grid.add(roleCombo, 1, 4);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                Utilisateur user = new Utilisateur();
                user.setNom(nomField.getText());
                user.setEmail(emailField.getText());
                user.setTelephone(phoneField.getText());
                user.setAdresse(addressField.getText());
                user.setRole(roleCombo.getValue());
                user.setStatut("ACTIF");
                user.setMotDePasse("TempPassword123!");
                return user;
            }
            return null;
        });
        
        Optional<Utilisateur> result = dialog.showAndWait();
        result.ifPresent(user -> {
            if (utilisateurDAO.create(user)) {
                showSuccessAlert("Utilisateur créé avec succès");
                loadUsers();
            }
        });
    }
    
    @FXML
    private void onEditUser() {
        Utilisateur selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarningAlert("Sélectionnez un utilisateur");
            return;
        }
        
        Dialog<Utilisateur> dialog = new Dialog<>();
        dialog.setTitle("Modifier l'Utilisateur");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField nomField = new TextField(selected.getNom());
        TextField emailField = new TextField(selected.getEmail());
        TextField phoneField = new TextField(selected.getTelephone() != null ? selected.getTelephone() : "");
        TextField addressField = new TextField(selected.getAdresse() != null ? selected.getAdresse() : "");
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("USER", "ADMIN");
        roleCombo.setValue(selected.getRole());
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("ACTIF", "INACTIF");
        statusCombo.setValue(selected.getStatut());
        
        grid.add(new Label("Nom:"), 0, 0);
        grid.add(nomField, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("Téléphone:"), 0, 2);
        grid.add(phoneField, 1, 2);
        grid.add(new Label("Adresse:"), 0, 3);
        grid.add(addressField, 1, 3);
        grid.add(new Label("Rôle:"), 0, 4);
        grid.add(roleCombo, 1, 4);
        grid.add(new Label("Statut:"), 0, 5);
        grid.add(statusCombo, 1, 5);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                selected.setNom(nomField.getText());
                selected.setEmail(emailField.getText());
                selected.setTelephone(phoneField.getText());
                selected.setAdresse(addressField.getText());
                selected.setRole(roleCombo.getValue());
                selected.setStatut(statusCombo.getValue());
                return selected;
            }
            return null;
        });
        
        Optional<Utilisateur> result = dialog.showAndWait();
        result.ifPresent(user -> {
            if (utilisateurDAO.update(user)) {
                showSuccessAlert("Utilisateur modifié avec succès");
                loadUsers();
            }
        });
    }
    
    @FXML
    private void onDeleteUser() {
        Utilisateur selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarningAlert("Sélectionnez un utilisateur");
            return;
        }
        
        if (showConfirmationDialog("Êtes-vous sûr de vouloir supprimer cet utilisateur ?")) {
            if (utilisateurDAO.delete(selected.getId())) {
                showSuccessAlert("Utilisateur supprimé");
                loadUsers();
            }
        }
    }
    
    // ==================== STATISTICS ====================
    private void loadStatistics() {
        long totalEvents = evenementDAO.getTotalCount();
        long approvedEvents = evenementDAO.getCountByStatut("APPROUVE");
        long pendingEvents = evenementDAO.getCountByStatut("EN_ATTENTE");
        long totalClaims = reclamationDAO.getTotalCount();
        long resolvedClaims = reclamationDAO.getCountByStatut("TRAITEE");
        long totalUsers = utilisateurDAO.getTotalCount();
        
        totalEventsLabel.setText(String.valueOf(totalEvents));
        approvedEventsLabel.setText(String.valueOf(approvedEvents));
        pendingEventsLabel.setText(String.valueOf(pendingEvents));
        totalClaimsLabel.setText(String.valueOf(totalClaims));
        resolvedClaimsLabel.setText(String.valueOf(resolvedClaims));
        totalUsersLabel.setText(String.valueOf(totalUsers));
    }
    
    // ==================== HELPER METHODS ====================
    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showWarningAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Attention");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private boolean showConfirmationDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    @FXML
    private void onLogout() {
        Session.getInstance().logout();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) mainTabPane.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

// Helper class for event validation
class EventValidationHelper {
    static String validateEvent(Evenement event) {
        if (event.getTitre() == null || event.getTitre().isEmpty())
            return "Le titre ne peut pas être vide";
        if (event.getDescription() == null || event.getDescription().isEmpty())
            return "La description ne peut pas être vide";
        if (event.getLieu() == null || event.getLieu().isEmpty())
            return "Le lieu ne peut pas être vide";
        if (event.getCapaciteMax() <= 0)
            return "La capacité doit être positive";
        if (event.getDateDebut() == null || event.getDateFin() == null)
            return "Les dates ne peuvent pas être vides";
        if (event.getDateFin().isBefore(event.getDateDebut()))
            return "La date de fin doit être après la date de début";
        return null;
    }
}
