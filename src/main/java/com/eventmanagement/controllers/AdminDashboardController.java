
package com.eventmanagement.controllers;

import com.eventmanagement.dao.EvenementDAO;
import com.eventmanagement.dao.ReclamationDAO;
import com.eventmanagement.dao.UtilisateurDAO;
import com.eventmanagement.models.Evenement;
import com.eventmanagement.models.Reclamation;
import com.eventmanagement.models.Utilisateur;
import com.eventmanagement.services.CSVService;
import com.eventmanagement.services.PDFService;
import com.eventmanagement.utils.PasswordHasher;
import com.eventmanagement.utils.Session;
import com.eventmanagement.utils.ValidationUtils;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdminDashboardController {

    private static final int PAGE_SIZE = 10;

    @FXML private Label welcomeLabel;
    @FXML private VBox evenementsSection;
    @FXML private VBox reclamationsSection;
    @FXML private VBox utilisateursSection;
    @FXML private VBox statsSection;

    @FXML private Label totalEvenementsLabel;
    @FXML private Label totalReclamationsLabel;
    @FXML private Label totalUtilisateursLabel;
    @FXML private TextArea statsTextArea;

    @FXML private TableView<Evenement> evenementsTable;
    @FXML private TableColumn<Evenement, Integer> evtIdCol;
    @FXML private TableColumn<Evenement, String> evtTitreCol;
    @FXML private TableColumn<Evenement, String> evtLieuCol;
    @FXML private TableColumn<Evenement, LocalDate> evtDateDebutCol;
    @FXML private TableColumn<Evenement, LocalDate> evtDateFinCol;
    @FXML private TableColumn<Evenement, Integer> evtCapaciteCol;
    @FXML private TableColumn<Evenement, String> evtStatutCol;
    @FXML private TableColumn<Evenement, Integer> evtOrgCol;
    @FXML private TextField evtSearchField;
    @FXML private ComboBox<String> evtStatusFilter;
    @FXML private TextField evtLieuFilterField;
    @FXML private DatePicker evtDateFromFilter;
    @FXML private DatePicker evtDateToFilter;
    @FXML private Pagination evtPagination;

    @FXML private TableView<Reclamation> reclamationsTable;
    @FXML private TableColumn<Reclamation, Integer> reclIdCol;
    @FXML private TableColumn<Reclamation, String> reclObjetCol;
    @FXML private TableColumn<Reclamation, LocalDate> reclDateCol;
    @FXML private TableColumn<Reclamation, String> reclStatutCol;
    @FXML private TableColumn<Reclamation, String> reclPrioriteCol;
    @FXML private TableColumn<Reclamation, String> reclTypeCol;
    @FXML private TableColumn<Reclamation, Integer> reclUserCol;
    @FXML private TextField reclSearchField;
    @FXML private ComboBox<String> reclStatusFilter;
    @FXML private ComboBox<String> reclTypeFilter;
    @FXML private Pagination reclPagination;

    @FXML private TableView<Utilisateur> utilisateursTable;
    @FXML private TableColumn<Utilisateur, Integer> userIdCol;
    @FXML private TableColumn<Utilisateur, String> userNomCol;
    @FXML private TableColumn<Utilisateur, String> userEmailCol;
    @FXML private TableColumn<Utilisateur, String> userRoleCol;
    @FXML private TableColumn<Utilisateur, String> userStatusCol;
    @FXML private TableColumn<Utilisateur, String> userPhoneCol;
    @FXML private TableColumn<Utilisateur, String> userAddressCol;
    @FXML private TextField userSearchField;
    @FXML private ComboBox<String> userRoleFilter;
    @FXML private ComboBox<String> userStatusFilter;
    @FXML private Pagination userPagination;

    private final EvenementDAO evenementDAO = new EvenementDAO();
    private final ReclamationDAO reclamationDAO = new ReclamationDAO();
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();

    private final ObservableList<Evenement> evenementsPage = FXCollections.observableArrayList();
    private final ObservableList<Reclamation> reclamationsPage = FXCollections.observableArrayList();
    private final ObservableList<Utilisateur> utilisateursPage = FXCollections.observableArrayList();

    private List<Evenement> allEvenements = new ArrayList<>();
    private List<Evenement> filteredEvenements = new ArrayList<>();
    private List<Reclamation> allReclamations = new ArrayList<>();
    private List<Reclamation> filteredReclamations = new ArrayList<>();
    private List<Utilisateur> allUtilisateurs = new ArrayList<>();
    private List<Utilisateur> filteredUtilisateurs = new ArrayList<>();

    @FXML
    public void initialize() {
        Utilisateur currentUser = Session.getInstance().getCurrentUser();
        String adminName = currentUser != null ? currentUser.getNom() : "Admin";
        welcomeLabel.setText("Bienvenue, " + adminName + " - panneau d'administration");

        setupTables();
        setupFilters();
        setupPagination();
        loadAllData();
        showStatsSection();
    }

    private void setupTables() {
        evtIdCol.setCellValueFactory(new PropertyValueFactory<>("idEvenement"));
        evtTitreCol.setCellValueFactory(new PropertyValueFactory<>("titre"));
        evtLieuCol.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        evtDateDebutCol.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        evtDateFinCol.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        evtCapaciteCol.setCellValueFactory(new PropertyValueFactory<>("capaciteMax"));
        evtStatutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));
        evtOrgCol.setCellValueFactory(new PropertyValueFactory<>("organisateurId"));
        evtStatutCol.setCellFactory(col -> createBadgeCell());
        evenementsTable.setItems(evenementsPage);

        reclIdCol.setCellValueFactory(new PropertyValueFactory<>("idReclamation"));
        reclObjetCol.setCellValueFactory(new PropertyValueFactory<>("objet"));
        reclDateCol.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
        reclStatutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));
        reclPrioriteCol.setCellValueFactory(new PropertyValueFactory<>("priorite"));
        reclTypeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        reclUserCol.setCellValueFactory(new PropertyValueFactory<>("idUtilisateur"));
        reclStatutCol.setCellFactory(col -> createBadgeCell());
        reclamationsTable.setItems(reclamationsPage);

        userIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        userNomCol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        userEmailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        userRoleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        userStatusCol.setCellValueFactory(new PropertyValueFactory<>("statut"));
        userPhoneCol.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        userAddressCol.setCellValueFactory(new PropertyValueFactory<>("adresse"));
        userStatusCol.setCellFactory(col -> createBadgeCell());
        utilisateursTable.setItems(utilisateursPage);
    }

    private <T> TableCell<T, String> createBadgeCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("badge-approuve", "badge-en-attente", "badge-rejete", "badge-traite", "badge-en-cours", "badge-cloturee", "badge-actif", "badge-inactif");
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    return;
                }
                setText(item);
                setStyle("-fx-alignment: CENTER;");
                switch (item) {
                    case "APPROUVE" -> getStyleClass().add("badge-approuve");
                    case "EN_ATTENTE" -> getStyleClass().add("badge-en-attente");
                    case "REJETE" -> getStyleClass().add("badge-rejete");
                    case "TRAITEE" -> getStyleClass().add("badge-traite");
                    case "EN_COURS" -> getStyleClass().add("badge-en-cours");
                    case "CLOTUREE" -> getStyleClass().add("badge-cloturee");
                    case "ACTIF" -> getStyleClass().add("badge-actif");
                    case "INACTIF" -> getStyleClass().add("badge-inactif");
                    default -> setStyle("-fx-font-weight: 700; -fx-alignment: CENTER;");
                }
            }
        };
    }

    private void setupFilters() {
        evtStatusFilter.setItems(FXCollections.observableArrayList("TOUS", "APPROUVE", "EN_ATTENTE", "REJETE"));
        evtStatusFilter.setValue("TOUS");
        reclStatusFilter.setItems(FXCollections.observableArrayList("TOUS", "EN_ATTENTE", "EN_COURS", "TRAITEE", "CLOTUREE"));
        reclStatusFilter.setValue("TOUS");
        userRoleFilter.setItems(FXCollections.observableArrayList("TOUS", "USER", "ADMIN"));
        userRoleFilter.setValue("TOUS");
        userStatusFilter.setItems(FXCollections.observableArrayList("TOUS", "ACTIF", "INACTIF"));
        userStatusFilter.setValue("TOUS");

        evtStatusFilter.setOnAction(e -> handleSearchEvenements());
        reclStatusFilter.setOnAction(e -> handleSearchReclamations());
        reclTypeFilter.setOnAction(e -> handleSearchReclamations());
        userRoleFilter.setOnAction(e -> handleSearchUtilisateurs());
        userStatusFilter.setOnAction(e -> handleSearchUtilisateurs());
    }

    private void setupPagination() {
        evtPagination.setPageFactory(pageIndex -> { refreshEvenementsPage(pageIndex); return new Region(); });
        reclPagination.setPageFactory(pageIndex -> { refreshReclamationsPage(pageIndex); return new Region(); });
        userPagination.setPageFactory(pageIndex -> { refreshUtilisateursPage(pageIndex); return new Region(); });
    }

    private void loadAllData() {
        allEvenements = evenementDAO.getAll();
        allReclamations = reclamationDAO.getAll();
        allUtilisateurs = utilisateurDAO.getAll();

        List<String> claimTypes = allReclamations.stream()
            .map(Reclamation::getType)
            .filter(t -> t != null && !t.isBlank())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
        claimTypes.add(0, "TOUS");
        reclTypeFilter.setItems(FXCollections.observableArrayList(claimTypes));
        reclTypeFilter.setValue("TOUS");

        applyEvenementFilters();
        applyReclamationFilters();
        applyUtilisateurFilters();
        updateStats();
    }

    private void updateStats() {
        totalEvenementsLabel.setText(String.valueOf(allEvenements.size()));
        totalReclamationsLabel.setText(String.valueOf(allReclamations.size()));
        totalUtilisateursLabel.setText(String.valueOf(allUtilisateurs.size()));

        Map<String, Integer> statsReclByStatus = reclamationDAO.getStatsByStatut();
        Map<String, Integer> statsReclByType = reclamationDAO.getStatsByType();

        long approuves = allEvenements.stream().filter(e -> "APPROUVE".equals(e.getStatut())).count();
        long enAttente = allEvenements.stream().filter(e -> "EN_ATTENTE".equals(e.getStatut())).count();
        long rejetes = allEvenements.stream().filter(e -> "REJETE".equals(e.getStatut())).count();

        StringBuilder sb = new StringBuilder();
        sb.append("=== STATISTIQUES EVENEMENTS ===\n");
        sb.append("Total: ").append(allEvenements.size()).append("\n");
        sb.append("Approuves: ").append(approuves).append("\n");
        sb.append("En attente: ").append(enAttente).append("\n");
        sb.append("Rejetes: ").append(rejetes).append("\n\n");

        sb.append("=== STATISTIQUES RECLAMATIONS PAR STATUT ===\n");
        statsReclByStatus.forEach((key, value) -> sb.append("- ").append(key).append(": ").append(value).append("\n"));
        sb.append("\n=== STATISTIQUES RECLAMATIONS PAR TYPE ===\n");
        statsReclByType.forEach((key, value) -> sb.append("- ").append(key).append(": ").append(value).append("\n"));

        long usersActifs = allUtilisateurs.stream().filter(u -> "ACTIF".equals(u.getStatut())).count();
        sb.append("\n=== UTILISATEURS ===\n");
        sb.append("Actifs: ").append(usersActifs).append("\n");
        sb.append("Inactifs: ").append(allUtilisateurs.size() - usersActifs).append("\n");
        statsTextArea.setText(sb.toString());
    }

    private void refreshEvenementsPage(int pageIndex) {
        int from = pageIndex * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, filteredEvenements.size());
        evenementsPage.setAll(from < to ? filteredEvenements.subList(from, to) : List.of());
    }

    private void refreshReclamationsPage(int pageIndex) {
        int from = pageIndex * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, filteredReclamations.size());
        reclamationsPage.setAll(from < to ? filteredReclamations.subList(from, to) : List.of());
    }

    private void refreshUtilisateursPage(int pageIndex) {
        int from = pageIndex * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, filteredUtilisateurs.size());
        utilisateursPage.setAll(from < to ? filteredUtilisateurs.subList(from, to) : List.of());
    }

    private void applyEvenementFilters() {
        String search = safe(evtSearchField.getText()).toLowerCase();
        String status = evtStatusFilter.getValue();
        String lieu = safe(evtLieuFilterField.getText()).toLowerCase();
        LocalDate fromDate = evtDateFromFilter.getValue();
        LocalDate toDate = evtDateToFilter.getValue();

        filteredEvenements = allEvenements.stream().filter(e -> {
            boolean matchesSearch = search.isBlank() || safe(e.getTitre()).toLowerCase().contains(search) || safe(e.getLieu()).toLowerCase().contains(search);
            boolean matchesStatus = status == null || "TOUS".equals(status) || status.equals(e.getStatut());
            boolean matchesLieu = lieu.isBlank() || safe(e.getLieu()).toLowerCase().contains(lieu);
            boolean matchesFrom = fromDate == null || (e.getDateDebut() != null && !e.getDateDebut().isBefore(fromDate));
            boolean matchesTo = toDate == null || (e.getDateFin() != null && !e.getDateFin().isAfter(toDate));
            return matchesSearch && matchesStatus && matchesLieu && matchesFrom && matchesTo;
        }).collect(Collectors.toList());

        int pageCount = Math.max(1, (int) Math.ceil((double) filteredEvenements.size() / PAGE_SIZE));
        evtPagination.setPageCount(pageCount);
        evtPagination.setCurrentPageIndex(0);
        refreshEvenementsPage(0);
    }

    private void applyReclamationFilters() {
        String search = safe(reclSearchField.getText()).toLowerCase();
        String status = reclStatusFilter.getValue();
        String type = reclTypeFilter.getValue();

        filteredReclamations = allReclamations.stream().filter(r -> {
            boolean matchesSearch = search.isBlank() || safe(r.getObjet()).toLowerCase().contains(search) || safe(r.getType()).toLowerCase().contains(search);
            boolean matchesStatus = status == null || "TOUS".equals(status) || status.equals(r.getStatut());
            boolean matchesType = type == null || "TOUS".equals(type) || type.equals(r.getType());
            return matchesSearch && matchesStatus && matchesType;
        }).collect(Collectors.toList());

        int pageCount = Math.max(1, (int) Math.ceil((double) filteredReclamations.size() / PAGE_SIZE));
        reclPagination.setPageCount(pageCount);
        reclPagination.setCurrentPageIndex(0);
        refreshReclamationsPage(0);
    }

    private void applyUtilisateurFilters() {
        String search = safe(userSearchField.getText()).toLowerCase();
        String role = userRoleFilter.getValue();
        String status = userStatusFilter.getValue();

        filteredUtilisateurs = allUtilisateurs.stream().filter(u -> {
            boolean matchesSearch = search.isBlank() || safe(u.getNom()).toLowerCase().contains(search) || safe(u.getEmail()).toLowerCase().contains(search);
            boolean matchesRole = role == null || "TOUS".equals(role) || role.equals(u.getRole());
            boolean matchesStatus = status == null || "TOUS".equals(status) || status.equals(u.getStatut());
            return matchesSearch && matchesRole && matchesStatus;
        }).collect(Collectors.toList());

        int pageCount = Math.max(1, (int) Math.ceil((double) filteredUtilisateurs.size() / PAGE_SIZE));
        userPagination.setPageCount(pageCount);
        userPagination.setCurrentPageIndex(0);
        refreshUtilisateursPage(0);
    }

    private String safe(String value) { return value == null ? "" : value.trim(); }

    private void hideAllSections() {
        statsSection.setVisible(false); statsSection.setManaged(false);
        evenementsSection.setVisible(false); evenementsSection.setManaged(false);
        reclamationsSection.setVisible(false); reclamationsSection.setManaged(false);
        utilisateursSection.setVisible(false); utilisateursSection.setManaged(false);
    }

    private void showSectionWithFade(VBox section) {
        hideAllSections();
        section.setManaged(true);
        section.setVisible(true);
        FadeTransition fade = new FadeTransition(Duration.millis(220), section);
        fade.setFromValue(0.2);
        fade.setToValue(1.0);
        fade.play();
    }

    @FXML private void showStatsSection() { showSectionWithFade(statsSection); }
    @FXML private void showEvenementsSection() { showSectionWithFade(evenementsSection); }
    @FXML private void showReclamationsSection() { showSectionWithFade(reclamationsSection); }
    @FXML private void showUtilisateursSection() { showSectionWithFade(utilisateursSection); }

    @FXML private void handleSearchEvenements() { applyEvenementFilters(); }
    @FXML private void handleSearchReclamations() { applyReclamationFilters(); }
    @FXML private void handleSearchUtilisateurs() { applyUtilisateurFilters(); }

    @FXML
    private void handleResetEvenementFilters() {
        evtSearchField.clear();
        evtLieuFilterField.clear();
        evtStatusFilter.setValue("TOUS");
        evtDateFromFilter.setValue(null);
        evtDateToFilter.setValue(null);
        applyEvenementFilters();
    }

    @FXML
    private void handleResetReclamationFilters() {
        reclSearchField.clear();
        reclStatusFilter.setValue("TOUS");
        reclTypeFilter.setValue("TOUS");
        applyReclamationFilters();
    }

    @FXML
    private void handleResetUtilisateurFilters() {
        userSearchField.clear();
        userRoleFilter.setValue("TOUS");
        userStatusFilter.setValue("TOUS");
        applyUtilisateurFilters();
    }

    @FXML
    private void handleAddEvenement() {
        Dialog<Evenement> dialog = buildEventDialog(null);
        Optional<Evenement> result = dialog.showAndWait();
        result.ifPresent(event -> {
            event.setStatut("EN_ATTENTE");
            if (evenementDAO.create(event)) {
                loadAllData();
                showInfo("Succes", "Evenement cree avec succes.");
            }
        });
    }

    @FXML
    private void handleEditEvenement() {
        Evenement selected = evenementsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Erreur", "Selectionnez un evenement a modifier."); return; }

        Dialog<Evenement> dialog = buildEventDialog(selected);
        Optional<Evenement> result = dialog.showAndWait();
        result.ifPresent(event -> {
            event.setIdEvenement(selected.getIdEvenement());
            event.setStatut(selected.getStatut());
            event.setOrganisateurId(selected.getOrganisateurId());
            event.setRaisonRejet(selected.getRaisonRejet());
            if (evenementDAO.update(event)) {
                loadAllData();
                showInfo("Succes", "Evenement mis a jour.");
            }
        });
    }

    @FXML
    private void handleApproveEvenement() {
        Evenement selected = evenementsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Erreur", "Selectionnez un evenement."); return; }
        if (evenementDAO.approveEvent(selected.getIdEvenement())) {
            loadAllData();
            showInfo("Succes", "Evenement approuve.");
        }
    }

    @FXML
    private void handleRejectEvenement() {
        Evenement selected = evenementsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Erreur", "Selectionnez un evenement."); return; }

        TextInputDialog reasonDialog = new TextInputDialog();
        reasonDialog.setTitle("Rejet evenement");
        reasonDialog.setHeaderText("Entrez une raison de rejet");
        reasonDialog.setContentText("Raison:");
        Optional<String> reason = reasonDialog.showAndWait();

        if (reason.isPresent() && !safe(reason.get()).isBlank()) {
            if (evenementDAO.rejectEvent(selected.getIdEvenement(), safe(reason.get()))) {
                loadAllData();
                showInfo("Succes", "Evenement rejete.");
            }
        }
    }

    @FXML
    private void handleDeleteEvenement() {
        Evenement selected = evenementsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Erreur", "Selectionnez un evenement."); return; }
        if (!confirm("Supprimer l'evenement selectionne ?")) { return; }
        if (evenementDAO.delete(selected.getIdEvenement())) {
            loadAllData();
            showInfo("Succes", "Evenement supprime.");
        }
    }

    @FXML
    private void handleChangeReclamationStatus() {
        Reclamation selected = reclamationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Erreur", "Selectionnez une reclamation."); return; }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(selected.getStatut(), "EN_ATTENTE", "EN_COURS", "TRAITEE", "CLOTUREE");
        dialog.setTitle("Statut reclamation");
        dialog.setHeaderText("Changer le statut");
        Optional<String> status = dialog.showAndWait();

        status.ifPresent(newStatus -> {
            if ("TRAITEE".equals(newStatus)) {
                if (reclamationDAO.markAsProcessed(selected.getIdReclamation())) {
                    loadAllData();
                    showInfo("Succes", "Statut reclamation mis a jour.");
                }
                return;
            }
            if (reclamationDAO.updateStatut(selected.getIdReclamation(), newStatus)) {
                loadAllData();
                showInfo("Succes", "Statut reclamation mis a jour.");
            }
        });
    }

    @FXML
    private void handleReplyReclamation() {
        Reclamation selected = reclamationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Erreur", "Selectionnez une reclamation."); return; }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Repondre reclamation");
        dialog.setHeaderText("Reponse admin");
        dialog.setContentText("Reponse:");
        Optional<String> reply = dialog.showAndWait();

        if (reply.isPresent() && !safe(reply.get()).isBlank()) {
            if (reclamationDAO.repondre(selected.getIdReclamation(), safe(reply.get()))) {
                loadAllData();
                showInfo("Succes", "Reponse envoyee.");
            }
        }
    }

    @FXML
    private void handleMarkReclamationProcessed() {
        Reclamation selected = reclamationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Erreur", "Selectionnez une reclamation."); return; }
        if (reclamationDAO.markAsProcessed(selected.getIdReclamation())) {
            loadAllData();
            showInfo("Succes", "Reclamation marquee comme traitee.");
        }
    }

    @FXML
    private void handleDeleteReclamation() {
        Reclamation selected = reclamationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Erreur", "Selectionnez une reclamation."); return; }
        if (!confirm("Supprimer la reclamation selectionnee ?")) { return; }
        if (reclamationDAO.delete(selected.getIdReclamation())) {
            loadAllData();
            showInfo("Succes", "Reclamation supprimee.");
        }
    }

    @FXML
    private void handleAddUtilisateur() {
        Dialog<Utilisateur> dialog = buildUserDialog(null);
        Optional<Utilisateur> result = dialog.showAndWait();
        result.ifPresent(user -> {
            if (utilisateurDAO.emailExists(user.getEmail())) {
                showInfo("Erreur", "Cet email existe deja.");
                return;
            }
            user.setMotDePasse(PasswordHasher.hashPassword("TempPass123!"));
            if (utilisateurDAO.create(user)) {
                loadAllData();
                showInfo("Succes", "Utilisateur ajoute (mot de passe temporaire: TempPass123!).");
            }
        });
    }

    @FXML
    private void handleEditUtilisateur() {
        Utilisateur selected = utilisateursTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Erreur", "Selectionnez un utilisateur."); return; }

        Dialog<Utilisateur> dialog = buildUserDialog(selected);
        Optional<Utilisateur> result = dialog.showAndWait();
        result.ifPresent(user -> {
            if (!selected.getEmail().equalsIgnoreCase(user.getEmail()) && utilisateurDAO.emailExists(user.getEmail())) {
                showInfo("Erreur", "Cet email existe deja.");
                return;
            }
            user.setId(selected.getId());
            user.setMotDePasse(selected.getMotDePasse());
            if (utilisateurDAO.update(user)) {
                loadAllData();
                showInfo("Succes", "Utilisateur modifie.");
            }
        });
    }

    @FXML
    private void handleToggleUtilisateurStatus() {
        Utilisateur selected = utilisateursTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Erreur", "Selectionnez un utilisateur."); return; }

        String newStatus = "ACTIF".equals(selected.getStatut()) ? "INACTIF" : "ACTIF";
        if (utilisateurDAO.changeStatut(selected.getId(), newStatus)) {
            loadAllData();
            showInfo("Succes", "Statut utilisateur mis a jour: " + newStatus);
        }
    }

    @FXML
    private void handleDeleteUtilisateur() {
        Utilisateur selected = utilisateursTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Erreur", "Selectionnez un utilisateur."); return; }

        Utilisateur currentUser = Session.getInstance().getCurrentUser();
        if (currentUser != null && selected.getId() == currentUser.getId()) {
            showInfo("Erreur", "Suppression de votre propre compte interdite.");
            return;
        }
        if (!confirm("Supprimer l'utilisateur selectionne ?")) { return; }
        if (utilisateurDAO.delete(selected.getId())) {
            loadAllData();
            showInfo("Succes", "Utilisateur supprime.");
        }
    }

    @FXML
    private void handleExportGlobalReport() {
        File file = chooseFile("Export global", "Text Files", "*.txt", "rapport_global_admin.txt");
        if (file == null) { return; }

        try (FileWriter writer = new FileWriter(file)) {
            writer.write("=== RAPPORT GLOBAL ADMIN ===\n");
            writer.write("Date: " + LocalDate.now() + "\n\n");
            writer.write("Evenements: " + allEvenements.size() + "\n");
            writer.write("Reclamations: " + allReclamations.size() + "\n");
            writer.write("Utilisateurs: " + allUtilisateurs.size() + "\n\n");
            writer.write(statsTextArea.getText());
            showInfo("Succes", "Export global termine.");
        } catch (IOException e) {
            showInfo("Erreur", "Echec export global: " + e.getMessage());
        }
    }

    @FXML
    private void handleExportEvenementsPDF() {
        File file = chooseFile("Export evenements", "Text Files", "*.txt", "evenements_admin.txt");
        if (file != null) {
            PDFService.exportEvenementsToPDF(evenementsPage, file.getAbsolutePath());
            showInfo("Succes", "Export evenements PDF termine.");
        }
    }

    @FXML
    private void handleExportEvenementsCSV() {
        File file = chooseFile("Export evenements", "CSV Files", "*.csv", "evenements_admin.csv");
        if (file != null) {
            CSVService.exportEvenementsToCSV(evenementsPage, file.getAbsolutePath());
            showInfo("Succes", "Export evenements CSV termine.");
        }
    }

    @FXML
    private void handleExportReclamationsPDF() {
        File file = chooseFile("Export reclamations", "Text Files", "*.txt", "reclamations_admin.txt");
        if (file != null) {
            PDFService.exportReclamationsToPDF(reclamationsPage, file.getAbsolutePath());
            showInfo("Succes", "Export reclamations PDF termine.");
        }
    }

    @FXML
    private void handleExportReclamationsCSV() {
        File file = chooseFile("Export reclamations", "CSV Files", "*.csv", "reclamations_admin.csv");
        if (file != null) {
            CSVService.exportReclamationsToCSV(reclamationsPage, file.getAbsolutePath());
            showInfo("Succes", "Export reclamations CSV termine.");
        }
    }

    @FXML
    private void handleLogout() {
        Session.getInstance().logout();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            showInfo("Erreur", "Erreur de deconnexion: " + e.getMessage());
        }
    }

    private Dialog<Evenement> buildEventDialog(Evenement source) {
        boolean edit = source != null;
        Dialog<Evenement> dialog = new Dialog<>();
        dialog.setTitle(edit ? "Modifier evenement" : "Ajouter evenement");

        ButtonType saveButton = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        TextField titleField = new TextField(edit ? source.getTitre() : "");
        TextArea descField = new TextArea(edit ? source.getDescription() : "");
        descField.setPrefRowCount(3);
        TextField lieuField = new TextField(edit ? source.getLieu() : "");
        TextField capaciteField = new TextField(edit ? String.valueOf(source.getCapaciteMax()) : "");
        DatePicker dateDebutPicker = new DatePicker(edit ? source.getDateDebut() : LocalDate.now().plusDays(1));
        DatePicker dateFinPicker = new DatePicker(edit ? source.getDateFin() : LocalDate.now().plusDays(2));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Titre"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Description"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label("Lieu"), 0, 2);
        grid.add(lieuField, 1, 2);
        grid.add(new Label("Capacite"), 0, 3);
        grid.add(capaciteField, 1, 3);
        grid.add(new Label("Date debut"), 0, 4);
        grid.add(dateDebutPicker, 1, 4);
        grid.add(new Label("Date fin"), 0, 5);
        grid.add(dateFinPicker, 1, 5);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != saveButton) { return null; }

            String titleError = ValidationUtils.validateEventTitle(titleField.getText());
            if (titleError != null) { showInfo("Validation", titleError); return null; }
            String descError = ValidationUtils.validateEventDescription(descField.getText());
            if (descError != null) { showInfo("Validation", descError); return null; }
            String locationError = ValidationUtils.validateEventLocation(lieuField.getText());
            if (locationError != null) { showInfo("Validation", locationError); return null; }
            String capacityError = ValidationUtils.validateCapacity(capaciteField.getText());
            if (capacityError != null) { showInfo("Validation", capacityError); return null; }
            String datesError = ValidationUtils.validateEventDates(dateDebutPicker.getValue(), dateFinPicker.getValue());
            if (datesError != null) { showInfo("Validation", datesError); return null; }

            Evenement e = new Evenement();
            e.setTitre(safe(titleField.getText()));
            e.setDescription(safe(descField.getText()));
            e.setLieu(safe(lieuField.getText()));
            e.setCapaciteMax(Integer.parseInt(capaciteField.getText().trim()));
            e.setDateDebut(dateDebutPicker.getValue());
            e.setDateFin(dateFinPicker.getValue());
            e.setOrganisateurId(edit ? source.getOrganisateurId() : Session.getInstance().getCurrentUser().getId());
            e.setImageUrl(edit ? source.getImageUrl() : null);
            return e;
        });

        return dialog;
    }

    private Dialog<Utilisateur> buildUserDialog(Utilisateur source) {
        boolean edit = source != null;
        Dialog<Utilisateur> dialog = new Dialog<>();
        dialog.setTitle(edit ? "Modifier utilisateur" : "Ajouter utilisateur");

        ButtonType saveButton = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        TextField nomField = new TextField(edit ? source.getNom() : "");
        TextField emailField = new TextField(edit ? source.getEmail() : "");
        TextField phoneField = new TextField(edit && source.getTelephone() != null ? source.getTelephone() : "");
        TextField addressField = new TextField(edit && source.getAdresse() != null ? source.getAdresse() : "");
        ComboBox<String> roleCombo = new ComboBox<>(FXCollections.observableArrayList("USER", "ADMIN"));
        roleCombo.setValue(edit ? source.getRole() : "USER");
        ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList("ACTIF", "INACTIF"));
        statusCombo.setValue(edit ? source.getStatut() : "ACTIF");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Nom"), 0, 0);
        grid.add(nomField, 1, 0);
        grid.add(new Label("Email"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("Telephone"), 0, 2);
        grid.add(phoneField, 1, 2);
        grid.add(new Label("Adresse"), 0, 3);
        grid.add(addressField, 1, 3);
        grid.add(new Label("Role"), 0, 4);
        grid.add(roleCombo, 1, 4);
        grid.add(new Label("Statut"), 0, 5);
        grid.add(statusCombo, 1, 5);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != saveButton) { return null; }

            String nameError = ValidationUtils.validateUsername(nomField.getText());
            if (nameError != null) { showInfo("Validation", nameError); return null; }
            String emailError = ValidationUtils.validateEmail(emailField.getText());
            if (emailError != null) { showInfo("Validation", emailError); return null; }
            if (!safe(phoneField.getText()).isBlank()) {
                String phoneError = ValidationUtils.validatePhone(phoneField.getText());
                if (phoneError != null) { showInfo("Validation", phoneError); return null; }
            }
            if (!safe(addressField.getText()).isBlank()) {
                String addrError = ValidationUtils.validateAddress(addressField.getText());
                if (addrError != null) { showInfo("Validation", addrError); return null; }
            }

            Utilisateur u = new Utilisateur();
            u.setNom(safe(nomField.getText()));
            u.setEmail(safe(emailField.getText()));
            u.setTelephone(safe(phoneField.getText()));
            u.setAdresse(safe(addressField.getText()));
            u.setRole(roleCombo.getValue());
            u.setStatut(statusCombo.getValue());
            return u;
        });
        return dialog;
    }

    private File chooseFile(String title, String extLabel, String extension, String defaultName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(extLabel, extension));
        chooser.setInitialFileName(defaultName);
        return chooser.showSaveDialog(welcomeLabel.getScene().getWindow());
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }
}
