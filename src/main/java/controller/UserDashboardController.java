
package controller;

import dao.EvenementDAO;
import dao.ReclamationDAO;
import entities.Evenement;
import entities.Reclamation;
import service.AssistantService;
import service.CSVService;
import service.CalendarService;
import service.GeoWeatherService;
import service.MapDialogService;
import service.PDFService;
import utils.Session;
import utils.ValidationUtils;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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

    private enum EventWizardState { IDLE, TITLE, LIEU, DATE_DEBUT, DATE_FIN, CAPACITE, DESCRIPTION, CONFIRM }
    private enum ClaimWizardState { IDLE, OBJET, DESCRIPTION, PRIORITE, TYPE, CONFIRM }

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
    @FXML private TextArea eventExternalInfoAreaUser;

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

    @FXML private TextArea chatAreaUser;
    @FXML private TextField chatInputUser;

    private final EvenementDAO evenementDAO = new EvenementDAO();
    private final ReclamationDAO reclamationDAO = new ReclamationDAO();
    private final AssistantService assistantService = new AssistantService();
    private final GeoWeatherService geoWeatherService = new GeoWeatherService();
    private final CalendarService calendarService = new CalendarService();

    private EventWizardState eventWizardState = EventWizardState.IDLE;
    private ClaimWizardState claimWizardState = ClaimWizardState.IDLE;
    private Evenement draftEvent;
    private Reclamation draftClaim;

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
        initChat();
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
        Button mapPickBtn = new Button("Choisir sur carte");
        mapPickBtn.getStyleClass().add("secondary-button");
        TextField capField = new TextField(edit ? String.valueOf(source.getCapaciteMax()) : "");
        DatePicker startPicker = new DatePicker(edit ? source.getDateDebut() : LocalDate.now().plusDays(1));
        DatePicker endPicker = new DatePicker(edit ? source.getDateFin() : LocalDate.now().plusDays(2));
        HBox lieuBox = new HBox(8, lieuField, mapPickBtn);
        HBox.setHgrow(lieuField, Priority.ALWAYS);

        mapPickBtn.setOnAction(e -> {
            Optional<GeoWeatherService.LocationResult> selected = MapDialogService.pickLocation(
                dialog.getDialogPane().getScene() != null ? dialog.getDialogPane().getScene().getWindow() : null,
                geoWeatherService,
                lieuField.getText()
            );
            selected.ifPresent(loc -> lieuField.setText(loc.displayName()));
        });

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Titre"), 0, 0); grid.add(titleField, 1, 0);
        grid.add(new Label("Description"), 0, 1); grid.add(descField, 1, 1);
        grid.add(new Label("Lieu"), 0, 2); grid.add(lieuBox, 1, 2);
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

    private void initChat() {
        appendUserBot("Assistant pret. Tu peux demander: evenements en attente, creation evenement, raison rejet, creation reclamation.");
    }

    @FXML
    private void handleExternalValidateAddress() {
        Evenement selected = evenementsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            eventExternalInfoAreaUser.setText("Selectionnez un evenement pour valider l'adresse.");
            return;
        }
        Optional<GeoWeatherService.LocationResult> location = geoWeatherService.geocode(selected.getLieu());
        if (location.isEmpty()) {
            eventExternalInfoAreaUser.setText("Adresse non validee: " + selected.getLieu());
            return;
        }
        GeoWeatherService.LocationResult loc = location.get();
        showMapPopup(loc.lat(), loc.lon(), "Carte - " + safe(selected.getTitre()));
        eventExternalInfoAreaUser.setText(
            "Adresse validee: " + loc.displayName()
                + "\nCoordonnees: lat=" + loc.lat() + ", lon=" + loc.lon()
                + "\nCarte affichee dans une popup."
        );
    }

    @FXML
    private void handleExternalWeather() {
        Evenement selected = evenementsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            eventExternalInfoAreaUser.setText("Selectionnez un evenement pour afficher la meteo.");
            return;
        }
        Optional<GeoWeatherService.LocationResult> location = geoWeatherService.geocode(selected.getLieu());
        if (location.isEmpty()) {
            eventExternalInfoAreaUser.setText("Impossible de recuperer la meteo: adresse invalide.");
            return;
        }
        GeoWeatherService.LocationResult loc = location.get();
        String weather = geoWeatherService.weatherForDate(loc.lat(), loc.lon(), selected.getDateDebut());
        eventExternalInfoAreaUser.setText(
            "Evenement: " + safe(selected.getTitre())
                + "\nLieu: " + loc.displayName()
                + "\nDate debut: " + selected.getDateDebut()
                + "\n" + weather
        );
    }

    @FXML
    private void handleExternalExportIcs() {
        Evenement selected = evenementsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            eventExternalInfoAreaUser.setText("Selectionnez un evenement pour exporter ICS.");
            return;
        }
        File file = pickFile("Export agenda ICS", "ICS Files", "*.ics", "event_" + selected.getIdEvenement() + ".ics");
        if (file == null) {
            eventExternalInfoAreaUser.setText("Export ICS annule.");
            return;
        }
        try {
            calendarService.exportEventAsIcs(selected, file.toPath());
            eventExternalInfoAreaUser.setText("Agenda ICS genere:\n" + file.getAbsolutePath());
        } catch (Exception e) {
            eventExternalInfoAreaUser.setText("Echec export ICS: " + e.getMessage());
        }
    }

    @FXML
    private void handleSendUserChat() {
        String message = safe(chatInputUser.getText());
        if (message.isBlank()) {
            return;
        }
        chatInputUser.clear();
        appendUser("Vous: " + message);
        processUserChatMessage(message);
    }

    @FXML
    private void handleClearUserChat() {
        chatAreaUser.clear();
        appendUserBot("Conversation reinitialisee.");
    }

    @FXML
    private void handleQuickUserPendingEvents() {
        appendUser("Vous: Quels sont mes evenements en attente ?");
        appendUserBot(assistantService.summarizeUserPendingEvents(allEvenements));
    }

    @FXML
    private void handleQuickUserRejectedReasons() {
        appendUser("Vous: Pourquoi mon evenement est rejete ?");
        appendUserBot(assistantService.explainRejectedEvents(allEvenements));
    }

    @FXML
    private void handleQuickStartEventWizard() {
        startEventWizard();
    }

    @FXML
    private void handleQuickStartClaimWizard() {
        startClaimWizard();
    }

    @FXML
    private void handleQuickValidateAndWeather() {
        Evenement selected = evenementsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            appendUserBot("Selectionne un evenement dans le tableau pour valider l'adresse et voir la meteo.");
            return;
        }
        appendUser("Vous: Valider lieu + meteo de l'evenement #" + selected.getIdEvenement());
        appendUserBot(validateLocationAndWeather(selected));
    }

    @FXML
    private void handleQuickExportSelectedEventIcs() {
        Evenement selected = evenementsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            appendUserBot("Selectionne un evenement pour generer le fichier ICS.");
            return;
        }
        File file = pickFile("Export agenda ICS", "ICS Files", "*.ics", "event_" + selected.getIdEvenement() + ".ics");
        if (file == null) {
            appendUserBot("Export ICS annule.");
            return;
        }
        try {
            calendarService.exportEventAsIcs(selected, file.toPath());
            appendUserBot("ICS genere: " + file.getAbsolutePath());
        } catch (Exception e) {
            appendUserBot("Echec export ICS: " + e.getMessage());
        }
    }

    private void processUserChatMessage(String message) {
        String lower = message.toLowerCase();

        if (eventWizardState != EventWizardState.IDLE) {
            continueEventWizard(message);
            return;
        }
        if (claimWizardState != ClaimWizardState.IDLE) {
            continueClaimWizard(message);
            return;
        }

        if (lower.contains("en attente") && lower.contains("evenement")) {
            appendUserBot(assistantService.summarizeUserPendingEvents(allEvenements));
            return;
        }
        if ((lower.contains("pourquoi") || lower.contains("raison")) && (lower.contains("rejet") || lower.contains("rejete"))) {
            appendUserBot(assistantService.explainRejectedEvents(allEvenements));
            return;
        }
        if (lower.contains("aide") && lower.contains("creer") && lower.contains("evenement")) {
            startEventWizard();
            return;
        }
        if (lower.contains("cree") && lower.contains("reclamation")) {
            startClaimWizard();
            return;
        }
        if (lower.contains("meteo") || lower.contains("adresse") || lower.contains("lieu")) {
            Evenement selected = evenementsTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                appendUserBot("Selectionne un evenement puis redemande meteo/adresse.");
            } else {
                appendUserBot(validateLocationAndWeather(selected));
            }
            return;
        }
        if (lower.contains("agenda") || lower.contains("calendar") || lower.contains("ics")) {
            handleQuickExportSelectedEventIcs();
            return;
        }

        appendUserBot("Commande non reconnue. Essaie: 'mes evenements en attente', 'aide creer evenement', 'creer reclamation', 'pourquoi rejete'.");
    }

    private void startEventWizard() {
        draftEvent = new Evenement();
        draftEvent.setOrganisateurId(Session.getInstance().getCurrentUser().getId());
        draftEvent.setStatut("EN_ATTENTE");
        eventWizardState = EventWizardState.TITLE;
        appendUserBot("Creation evenement assistee demarree. Donne le titre.");
    }

    private void continueEventWizard(String input) {
        switch (eventWizardState) {
            case TITLE -> {
                String err = ValidationUtils.validateEventTitle(input);
                if (err != null) { appendUserBot(err); return; }
                draftEvent.setTitre(safe(input));
                eventWizardState = EventWizardState.LIEU;
                appendUserBot("Lieu ?");
            }
            case LIEU -> {
                String err = ValidationUtils.validateEventLocation(input);
                if (err != null) { appendUserBot(err); return; }
                draftEvent.setLieu(safe(input));
                geoWeatherService.geocode(draftEvent.getLieu())
                    .ifPresentOrElse(
                        loc -> appendUserBot("Adresse validee: " + loc.displayName() + " (lat=" + loc.lat() + ", lon=" + loc.lon() + ")"),
                        () -> appendUserBot("Adresse non validee via API. Tu peux continuer, mais verifie le lieu.")
                    );
                eventWizardState = EventWizardState.DATE_DEBUT;
                appendUserBot("Date debut (format AAAA-MM-JJ) ?");
            }
            case DATE_DEBUT -> {
                try {
                    draftEvent.setDateDebut(LocalDate.parse(safe(input)));
                    eventWizardState = EventWizardState.DATE_FIN;
                    appendUserBot("Date fin (format AAAA-MM-JJ) ?");
                } catch (Exception e) {
                    appendUserBot("Format date invalide. Exemple: 2026-04-25");
                }
            }
            case DATE_FIN -> {
                try {
                    LocalDate fin = LocalDate.parse(safe(input));
                    String err = ValidationUtils.validateEventDates(draftEvent.getDateDebut(), fin);
                    if (err != null) { appendUserBot(err); return; }
                    draftEvent.setDateFin(fin);
                    eventWizardState = EventWizardState.CAPACITE;
                    appendUserBot("Capacite maximale ?");
                } catch (Exception e) {
                    appendUserBot("Format date invalide. Exemple: 2026-04-26");
                }
            }
            case CAPACITE -> {
                String err = ValidationUtils.validateCapacity(input);
                if (err != null) { appendUserBot(err); return; }
                draftEvent.setCapaciteMax(Integer.parseInt(safe(input)));
                eventWizardState = EventWizardState.DESCRIPTION;
                appendUserBot("Description de l'evenement ?");
            }
            case DESCRIPTION -> {
                String err = ValidationUtils.validateEventDescription(input);
                if (err != null) { appendUserBot(err); return; }
                draftEvent.setDescription(safe(input));
                eventWizardState = EventWizardState.CONFIRM;
                appendUserBot("Confirmer la creation ? reponds 'oui' ou 'non'.");
            }
            case CONFIRM -> {
                if ("oui".equalsIgnoreCase(safe(input))) {
                    if (evenementDAO.create(draftEvent)) {
                        loadData();
                        appendUserBot("Evenement cree avec succes et en statut EN_ATTENTE.");
                    } else {
                        appendUserBot("Echec creation evenement.");
                    }
                } else {
                    appendUserBot("Creation evenement annulee.");
                }
                eventWizardState = EventWizardState.IDLE;
                draftEvent = null;
            }
            default -> {
                eventWizardState = EventWizardState.IDLE;
                draftEvent = null;
            }
        }
    }

    private void startClaimWizard() {
        draftClaim = new Reclamation();
        draftClaim.setIdUtilisateur(Session.getInstance().getCurrentUser().getId());
        draftClaim.setDateCreation(LocalDate.now());
        draftClaim.setStatut("EN_ATTENTE");
        claimWizardState = ClaimWizardState.OBJET;
        appendUserBot("Creation reclamation assistee demarree. Donne l'objet.");
    }

    private void continueClaimWizard(String input) {
        switch (claimWizardState) {
            case OBJET -> {
                String err = ValidationUtils.validateComplaintSubject(input);
                if (err != null) { appendUserBot(err); return; }
                draftClaim.setObjet(safe(input));
                claimWizardState = ClaimWizardState.DESCRIPTION;
                appendUserBot("Description ?");
            }
            case DESCRIPTION -> {
                String err = ValidationUtils.validateComplaintDescription(input);
                if (err != null) { appendUserBot(err); return; }
                draftClaim.setDescription(safe(input));
                claimWizardState = ClaimWizardState.PRIORITE;
                appendUserBot("Priorite (BASSE/MOYENNE/HAUTE) ?");
            }
            case PRIORITE -> {
                String p = safe(input).toUpperCase();
                if (!List.of("BASSE", "MOYENNE", "HAUTE").contains(p)) {
                    appendUserBot("Priorite invalide. Valeurs: BASSE, MOYENNE, HAUTE.");
                    return;
                }
                draftClaim.setPriorite(p);
                claimWizardState = ClaimWizardState.TYPE;
                appendUserBot("Type de reclamation ?");
            }
            case TYPE -> {
                if (safe(input).length() < 3) {
                    appendUserBot("Le type doit contenir au moins 3 caracteres.");
                    return;
                }
                draftClaim.setType(safe(input));
                claimWizardState = ClaimWizardState.CONFIRM;
                appendUserBot("Confirmer la creation ? reponds 'oui' ou 'non'.");
            }
            case CONFIRM -> {
                if ("oui".equalsIgnoreCase(safe(input))) {
                    if (reclamationDAO.create(draftClaim)) {
                        loadData();
                        appendUserBot("Reclamation creee avec succes.");
                    } else {
                        appendUserBot("Echec creation reclamation.");
                    }
                } else {
                    appendUserBot("Creation reclamation annulee.");
                }
                claimWizardState = ClaimWizardState.IDLE;
                draftClaim = null;
            }
            default -> {
                claimWizardState = ClaimWizardState.IDLE;
                draftClaim = null;
            }
        }
    }

    private String validateLocationAndWeather(Evenement event) {
        Optional<GeoWeatherService.LocationResult> location = geoWeatherService.geocode(event.getLieu());
        if (location.isEmpty()) {
            return "Adresse non validee via geocodage pour '" + event.getLieu() + "'.";
        }
        GeoWeatherService.LocationResult loc = location.get();
        String weather = geoWeatherService.weatherForDate(loc.lat(), loc.lon(), event.getDateDebut());
        return "Adresse validee: " + loc.displayName()
            + "\nCoordonnees: lat=" + loc.lat() + ", lon=" + loc.lon()
            + "\n" + weather;
    }

    private void showMapPopup(double lat, double lon, String title) {
        MapDialogService.openMapViewer(
            welcomeLabel.getScene() != null ? welcomeLabel.getScene().getWindow() : null,
            title,
            lat,
            lon,
            ""
        );
    }

    private void appendUser(String text) {
        if (chatAreaUser.getText().isEmpty()) {
            chatAreaUser.appendText(text);
        } else {
            chatAreaUser.appendText("\n\n" + text);
        }
    }

    private void appendUserBot(String text) {
        appendUser("Assistant: " + text);
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
