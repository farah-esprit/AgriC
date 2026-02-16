
package com.eventmanagement.controllers;

import com.eventmanagement.dao.EvenementDAO;
import com.eventmanagement.dao.ReclamationDAO;
import com.eventmanagement.models.Evenement;
import com.eventmanagement.models.Reclamation;
import com.eventmanagement.services.CSVService;
import com.eventmanagement.services.PDFService;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserDashboardController {

    private static final int PAGE_SIZE = 10;

    @FXML private Label welcomeLabel;
    @FXML private VBox evenementsSection;
    @FXML private VBox reclamationsSection;

    @FXML private TableView<Evenement> evenementsTable;
    @FXML private TableColumn<Evenement, Integer> evtIdCol;
    @FXML private TableColumn<Evenement, String> evtTitreCol;
    @FXML private TableColumn<Evenement, String> evtLieuCol;
    @FXML private TableColumn<Evenement, LocalDate> evtDateDebutCol;
    @FXML private TableColumn<Evenement, LocalDate> evtDateFinCol;
    @FXML private TableColumn<Evenement, Integer> evtCapaciteCol;
    @FXML private TableColumn<Evenement, String> evtStatutCol;
    @FXML private TextField evtSearchField;
    @FXML private ComboBox<String> evtStatusFilter;
    @FXML private Pagination evtPagination;

    @FXML private TableView<Reclamation> reclamationsTable;
    @FXML private TableColumn<Reclamation, Integer> reclIdCol;
    @FXML private TableColumn<Reclamation, String> reclObjetCol;
    @FXML private TableColumn<Reclamation, LocalDate> reclDateCol;
    @FXML private TableColumn<Reclamation, String> reclStatutCol;
    @FXML private TableColumn<Reclamation, String> reclPrioriteCol;
    @FXML private TableColumn<Reclamation, String> reclTypeCol;
    @FXML private TextField reclSearchField;
    @FXML private ComboBox<String> reclStatusFilter;
    @FXML private Pagination reclPagination;

    private final EvenementDAO evenementDAO = new EvenementDAO();
    private final ReclamationDAO reclamationDAO = new ReclamationDAO();

    private final ObservableList<Evenement> evenementsPage = FXCollections.observableArrayList();
    private final ObservableList<Reclamation> reclamationsPage = FXCollections.observableArrayList();

    private List<Evenement> allEvenements = new ArrayList<>();
    private List<Evenement> filteredEvenements = new ArrayList<>();
    private List<Reclamation> allReclamations = new ArrayList<>();
    private List<Reclamation> filteredReclamations = new ArrayList<>();

    @FXML
    public void initialize() {
        welcomeLabel.setText("Bienvenue, " + Session.getInstance().getCurrentUser().getNom());

        setupTables();
        setupFilters();
        setupPagination();
        loadData();
        showEvenementsSection();
    }

    private void setupTables() {
        evtIdCol.setCellValueFactory(new PropertyValueFactory<>("idEvenement"));
        evtTitreCol.setCellValueFactory(new PropertyValueFactory<>("titre"));
        evtLieuCol.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        evtDateDebutCol.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        evtDateFinCol.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        evtCapaciteCol.setCellValueFactory(new PropertyValueFactory<>("capaciteMax"));
        evtStatutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));
        evtStatutCol.setCellFactory(col -> badgeCell());
        evenementsTable.setItems(evenementsPage);

        reclIdCol.setCellValueFactory(new PropertyValueFactory<>("idReclamation"));
        reclObjetCol.setCellValueFactory(new PropertyValueFactory<>("objet"));
        reclDateCol.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
        reclStatutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));
        reclPrioriteCol.setCellValueFactory(new PropertyValueFactory<>("priorite"));
        reclTypeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        reclStatutCol.setCellFactory(col -> badgeCell());
        reclamationsTable.setItems(reclamationsPage);
    }

    private <T> TableCell<T, String> badgeCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("badge-approuve", "badge-en-attente", "badge-rejete", "badge-traite", "badge-en-cours", "badge-cloturee");
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("-fx-alignment: CENTER;");
                switch (item) {
                    case "APPROUVE" -> getStyleClass().add("badge-approuve");
                    case "EN_ATTENTE" -> getStyleClass().add("badge-en-attente");
                    case "REJETE" -> getStyleClass().add("badge-rejete");
                    case "TRAITEE" -> getStyleClass().add("badge-traite");
                    case "EN_COURS" -> getStyleClass().add("badge-en-cours");
                    case "CLOTUREE" -> getStyleClass().add("badge-cloturee");
                    default -> setStyle("-fx-alignment: CENTER; -fx-font-weight: 700;");
                }
            }
        };
    }

    private void setupFilters() {
        evtStatusFilter.setItems(FXCollections.observableArrayList("TOUS", "APPROUVE", "EN_ATTENTE", "REJETE"));
        evtStatusFilter.setValue("TOUS");
        reclStatusFilter.setItems(FXCollections.observableArrayList("TOUS", "EN_ATTENTE", "EN_COURS", "TRAITEE", "CLOTUREE"));
        reclStatusFilter.setValue("TOUS");
        evtStatusFilter.setOnAction(e -> handleSearchEvenements());
        reclStatusFilter.setOnAction(e -> handleSearchReclamations());
    }

    private void setupPagination() {
        evtPagination.setPageFactory(pageIndex -> { refreshEvenementsPage(pageIndex); return new Region(); });
        reclPagination.setPageFactory(pageIndex -> { refreshReclamationsPage(pageIndex); return new Region(); });
    }

    private void loadData() {
        int userId = Session.getInstance().getCurrentUser().getId();
        allEvenements = evenementDAO.getByOrganisateur(userId);
        allReclamations = reclamationDAO.getByUtilisateur(userId);
        applyEvenementFilters();
        applyReclamationFilters();
    }

    private void applyEvenementFilters() {
        String search = safe(evtSearchField.getText()).toLowerCase();
        String status = evtStatusFilter.getValue();
        filteredEvenements = allEvenements.stream().filter(e -> {
            boolean m1 = search.isBlank() || safe(e.getTitre()).toLowerCase().contains(search) || safe(e.getLieu()).toLowerCase().contains(search);
            boolean m2 = status == null || "TOUS".equals(status) || status.equals(e.getStatut());
            return m1 && m2;
        }).collect(Collectors.toList());

        int pageCount = Math.max(1, (int) Math.ceil((double) filteredEvenements.size() / PAGE_SIZE));
        evtPagination.setPageCount(pageCount);
        evtPagination.setCurrentPageIndex(0);
        refreshEvenementsPage(0);
    }

    private void applyReclamationFilters() {
        String search = safe(reclSearchField.getText()).toLowerCase();
        String status = reclStatusFilter.getValue();
        filteredReclamations = allReclamations.stream().filter(r -> {
            boolean m1 = search.isBlank() || safe(r.getObjet()).toLowerCase().contains(search) || safe(r.getType()).toLowerCase().contains(search);
            boolean m2 = status == null || "TOUS".equals(status) || status.equals(r.getStatut());
            return m1 && m2;
        }).collect(Collectors.toList());

        int pageCount = Math.max(1, (int) Math.ceil((double) filteredReclamations.size() / PAGE_SIZE));
        reclPagination.setPageCount(pageCount);
        reclPagination.setCurrentPageIndex(0);
        refreshReclamationsPage(0);
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

    @FXML
    private void showEvenementsSection() {
        evenementsSection.setManaged(true);
        evenementsSection.setVisible(true);
        reclamationsSection.setManaged(false);
        reclamationsSection.setVisible(false);
        FadeTransition fade = new FadeTransition(Duration.millis(220), evenementsSection);
        fade.setFromValue(0.2);
        fade.setToValue(1.0);
        fade.play();
    }

    @FXML
    private void showReclamationsSection() {
        evenementsSection.setManaged(false);
        evenementsSection.setVisible(false);
        reclamationsSection.setManaged(true);
        reclamationsSection.setVisible(true);
        FadeTransition fade = new FadeTransition(Duration.millis(220), reclamationsSection);
        fade.setFromValue(0.2);
        fade.setToValue(1.0);
        fade.play();
    }

    @FXML private void handleSearchEvenements() { applyEvenementFilters(); }
    @FXML private void handleSearchReclamations() { applyReclamationFilters(); }

    @FXML
    private void handleResetEvenementFilters() {
        evtSearchField.clear();
        evtStatusFilter.setValue("TOUS");
        applyEvenementFilters();
    }

    @FXML
    private void handleResetReclamationFilters() {
        reclSearchField.clear();
        reclStatusFilter.setValue("TOUS");
        applyReclamationFilters();
    }

    @FXML
    private void handleAddEvenement() {
        Dialog<Evenement> dialog = buildEventDialog(null);
        Optional<Evenement> result = dialog.showAndWait();
        result.ifPresent(evt -> {
            evt.setOrganisateurId(Session.getInstance().getCurrentUser().getId());
            evt.setStatut("EN_ATTENTE");
            if (evenementDAO.create(evt)) {
                loadData();
                showAlert("Succes", "Evenement cree avec succes.");
            }
        });
    }

    @FXML
    private void handleEditEvenement() {
        Evenement selected = evenementsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Erreur", "Selectionnez un evenement."); return; }

        Dialog<Evenement> dialog = buildEventDialog(selected);
        Optional<Evenement> result = dialog.showAndWait();
        result.ifPresent(evt -> {
            evt.setIdEvenement(selected.getIdEvenement());
            evt.setOrganisateurId(selected.getOrganisateurId());
            evt.setStatut("EN_ATTENTE");
            evt.setRaisonRejet(null);
            evt.setImageUrl(selected.getImageUrl());
            if (evenementDAO.update(evt)) {
                loadData();
                showAlert("Succes", "Evenement modifie. Le statut repasse en EN_ATTENTE.");
            }
        });
    }

    @FXML
    private void handleDeleteEvenement() {
        Evenement selected = evenementsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Erreur", "Selectionnez un evenement."); return; }
        if (!confirm("Supprimer l'evenement selectionne ?")) { return; }
        if (evenementDAO.delete(selected.getIdEvenement())) {
            loadData();
            showAlert("Succes", "Evenement supprime.");
        }
    }

    @FXML
    private void handleAddReclamation() {
        Dialog<Reclamation> dialog = buildReclamationDialog(null, false);
        Optional<Reclamation> result = dialog.showAndWait();
        result.ifPresent(rec -> {
            rec.setDateCreation(LocalDate.now());
            rec.setStatut("EN_ATTENTE");
            rec.setIdUtilisateur(Session.getInstance().getCurrentUser().getId());
            if (reclamationDAO.create(rec)) {
                loadData();
                showAlert("Succes", "Reclamation creee avec succes.");
            }
        });
    }

    @FXML
    private void handleEditReclamation() {
        Reclamation selected = reclamationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Erreur", "Selectionnez une reclamation."); return; }

        Dialog<Reclamation> dialog = buildReclamationDialog(selected, true);
        Optional<Reclamation> result = dialog.showAndWait();
        result.ifPresent(rec -> {
            selected.setObjet(rec.getObjet());
            selected.setDescription(rec.getDescription());
            selected.setPriorite(rec.getPriorite());
            selected.setType(rec.getType());
            if (reclamationDAO.update(selected)) {
                loadData();
                showAlert("Succes", "Reclamation modifiee.");
            }
        });
    }

    @FXML
    private void handleDeleteReclamation() {
        Reclamation selected = reclamationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Erreur", "Selectionnez une reclamation."); return; }
        if (!confirm("Supprimer la reclamation selectionnee ?")) { return; }
        if (reclamationDAO.delete(selected.getIdReclamation())) {
            loadData();
            showAlert("Succes", "Reclamation supprimee.");
        }
    }

    @FXML
    private void handleExportEvenementsPDF() {
        File file = pickFile("Export evenements", "Text Files", "*.txt", "evenements_user.txt");
        if (file != null) {
            PDFService.exportEvenementsToPDF(evenementsPage, file.getAbsolutePath());
            showAlert("Succes", "Export PDF termine.");
        }
    }

    @FXML
    private void handleExportEvenementsCSV() {
        File file = pickFile("Export evenements", "CSV Files", "*.csv", "evenements_user.csv");
        if (file != null) {
            CSVService.exportEvenementsToCSV(evenementsPage, file.getAbsolutePath());
            showAlert("Succes", "Export CSV termine.");
        }
    }

    @FXML
    private void handleExportReclamationsPDF() {
        File file = pickFile("Export reclamations", "Text Files", "*.txt", "reclamations_user.txt");
        if (file != null) {
            PDFService.exportReclamationsToPDF(reclamationsPage, file.getAbsolutePath());
            showAlert("Succes", "Export PDF termine.");
        }
    }

    @FXML
    private void handleExportReclamationsCSV() {
        File file = pickFile("Export reclamations", "CSV Files", "*.csv", "reclamations_user.csv");
        if (file != null) {
            CSVService.exportReclamationsToCSV(reclamationsPage, file.getAbsolutePath());
            showAlert("Succes", "Export CSV termine.");
        }
    }

    @FXML
    private void handleLogout() {
        Session.getInstance().logout();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            showAlert("Erreur", "Erreur de deconnexion: " + e.getMessage());
        }
    }

    private Dialog<Evenement> buildEventDialog(Evenement source) {
        boolean edit = source != null;
        Dialog<Evenement> dialog = new Dialog<>();
        dialog.setTitle(edit ? "Modifier evenement" : "Ajouter evenement");
        ButtonType save = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        TextField titleField = new TextField(edit ? source.getTitre() : "");
        TextArea descField = new TextArea(edit ? source.getDescription() : "");
        descField.setPrefRowCount(3);
        TextField lieuField = new TextField(edit ? source.getLieu() : "");
        TextField capField = new TextField(edit ? String.valueOf(source.getCapaciteMax()) : "");
        DatePicker startPicker = new DatePicker(edit ? source.getDateDebut() : LocalDate.now().plusDays(1));
        DatePicker endPicker = new DatePicker(edit ? source.getDateFin() : LocalDate.now().plusDays(2));

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Titre"), 0, 0); grid.add(titleField, 1, 0);
        grid.add(new Label("Description"), 0, 1); grid.add(descField, 1, 1);
        grid.add(new Label("Lieu"), 0, 2); grid.add(lieuField, 1, 2);
        grid.add(new Label("Capacite"), 0, 3); grid.add(capField, 1, 3);
        grid.add(new Label("Date debut"), 0, 4); grid.add(startPicker, 1, 4);
        grid.add(new Label("Date fin"), 0, 5); grid.add(endPicker, 1, 5);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != save) { return null; }
            String err;
            err = ValidationUtils.validateEventTitle(titleField.getText()); if (err != null) { showAlert("Validation", err); return null; }
            err = ValidationUtils.validateEventDescription(descField.getText()); if (err != null) { showAlert("Validation", err); return null; }
            err = ValidationUtils.validateEventLocation(lieuField.getText()); if (err != null) { showAlert("Validation", err); return null; }
            err = ValidationUtils.validateCapacity(capField.getText()); if (err != null) { showAlert("Validation", err); return null; }
            err = ValidationUtils.validateEventDates(startPicker.getValue(), endPicker.getValue()); if (err != null) { showAlert("Validation", err); return null; }

            Evenement evt = new Evenement();
            evt.setTitre(safe(titleField.getText()));
            evt.setDescription(safe(descField.getText()));
            evt.setLieu(safe(lieuField.getText()));
            evt.setCapaciteMax(Integer.parseInt(capField.getText().trim()));
            evt.setDateDebut(startPicker.getValue());
            evt.setDateFin(endPicker.getValue());
            return evt;
        });
        return dialog;
    }

    private Dialog<Reclamation> buildReclamationDialog(Reclamation source, boolean isEdit) {
        Dialog<Reclamation> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Modifier reclamation" : "Ajouter reclamation");
        ButtonType save = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        TextField objetField = new TextField(isEdit ? source.getObjet() : "");
        TextArea descField = new TextArea(isEdit ? source.getDescription() : "");
        descField.setPrefRowCount(3);
        ComboBox<String> prioriteCombo = new ComboBox<>(FXCollections.observableArrayList("BASSE", "MOYENNE", "HAUTE"));
        prioriteCombo.setValue(isEdit ? source.getPriorite() : "MOYENNE");
        TextField typeField = new TextField(isEdit ? source.getType() : "");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Objet"), 0, 0); grid.add(objetField, 1, 0);
        grid.add(new Label("Description"), 0, 1); grid.add(descField, 1, 1);
        grid.add(new Label("Priorite"), 0, 2); grid.add(prioriteCombo, 1, 2);
        grid.add(new Label("Type"), 0, 3); grid.add(typeField, 1, 3);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != save) { return null; }
            String err;
            err = ValidationUtils.validateComplaintSubject(objetField.getText()); if (err != null) { showAlert("Validation", err); return null; }
            err = ValidationUtils.validateComplaintDescription(descField.getText()); if (err != null) { showAlert("Validation", err); return null; }
            if (safe(typeField.getText()).length() < 3) { showAlert("Validation", "Le type doit contenir au moins 3 caracteres."); return null; }

            Reclamation r = new Reclamation();
            r.setObjet(safe(objetField.getText()));
            r.setDescription(safe(descField.getText()));
            r.setPriorite(prioriteCombo.getValue());
            r.setType(safe(typeField.getText()));
            return r;
        });
        return dialog;
    }

    private File pickFile(String title, String extLabel, String extension, String defaultName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(extLabel, extension));
        chooser.setInitialFileName(defaultName);
        return chooser.showSaveDialog(welcomeLabel.getScene().getWindow());
    }

    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private String safe(String value) { return value == null ? "" : value.trim(); }
}
