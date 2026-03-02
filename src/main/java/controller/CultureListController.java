package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import entities.Culture;
import service.CultureService;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class CultureListController implements Initializable {

    // ══════════════════════════════════════════════════════════════════════
    // FXML
    // ══════════════════════════════════════════════════════════════════════
    @FXML private Pagination       pagination;
    @FXML private TextField        txtRecherche;
    @FXML private Button           btnRefresh;
    @FXML private ComboBox<String> cbType;

    // ══════════════════════════════════════════════════════════════════════
    // ÉTAT INTERNE
    // ══════════════════════════════════════════════════════════════════════
    private final CultureService        service        = new CultureService();
    private ObservableList<Culture>     masterData     = FXCollections.observableArrayList();
    private static final int            ITEMS_PER_PAGE = 6;
    private DashboardAgriculteurController dashboardController;

    // ══════════════════════════════════════════════════════════════════════
    // SETTER
    // ══════════════════════════════════════════════════════════════════════
    public void setDashboardController(DashboardAgriculteurController dashboard) {
        this.dashboardController = dashboard;
        System.out.println("✅ CultureListController — dashboard lié");
    }

    // ══════════════════════════════════════════════════════════════════════
    // INITIALISATION
    // ══════════════════════════════════════════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        masterData.setAll(service.getAll());
        setupPagination(masterData);
        ajouterRecherche();
        setupTypeComboBox();

        if (btnRefresh != null) {
            btnRefresh.setOnAction(e -> handleRefresh());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // COMBOBOX TYPE
    // ══════════════════════════════════════════════════════════════════════
    private void setupTypeComboBox() {
        ObservableList<String> types = FXCollections.observableArrayList();
        for (Culture c : masterData) {
            if (c.getType() != null && !types.contains(c.getType()))
                types.add(c.getType());
        }
        FXCollections.sort(types);
        cbType.getItems().add("Tous les types");
        cbType.getItems().addAll(types);
        cbType.getSelectionModel().selectFirst();
        cbType.valueProperty().addListener((obs, o, n) -> filtrerParType());
    }

    private void filtrerParType() {
        String selectedType = cbType.getSelectionModel().getSelectedItem();
        ObservableList<Culture> filtered = masterData.filtered(c -> {
            boolean matchRecherche = txtRecherche.getText() == null
                    || txtRecherche.getText().isEmpty()
                    || c.getNom().toLowerCase().contains(txtRecherche.getText().toLowerCase());
            boolean matchType = selectedType == null
                    || selectedType.equals("Tous les types")
                    || (c.getType() != null && c.getType().equalsIgnoreCase(selectedType));
            return matchRecherche && matchType;
        });
        setupPagination(filtered);
    }

    // ══════════════════════════════════════════════════════════════════════
    // RECHERCHE
    // ══════════════════════════════════════════════════════════════════════
    private void ajouterRecherche() {
        txtRecherche.textProperty().addListener((obs, o, n) -> filtrerParType());
    }

    // ══════════════════════════════════════════════════════════════════════
    // PAGINATION
    // ══════════════════════════════════════════════════════════════════════
    private void setupPagination(ObservableList<Culture> data) {
        int pageCount = (int) Math.ceil((double) data.size() / ITEMS_PER_PAGE);
        if (pageCount == 0) pageCount = 1;
        pagination.setPageCount(pageCount);
        pagination.setPageFactory(pageIndex -> createPage(pageIndex, data));
    }

    private FlowPane createPage(int pageIndex, ObservableList<Culture> data) {
        FlowPane flow = new FlowPane();
        flow.setHgap(20);
        flow.setVgap(20);
        flow.setPrefWrapLength(850);

        int fromIndex = pageIndex * ITEMS_PER_PAGE;
        int toIndex   = Math.min(fromIndex + ITEMS_PER_PAGE, data.size());

        for (Culture c : data.subList(fromIndex, toIndex)) {
            VBox card = new VBox(10);
            card.setPrefWidth(240);
            card.setStyle("""
                -fx-background-color: white;
                -fx-padding: 20;
                -fx-background-radius: 15;
                -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 15, 0, 0, 5);
                """);

            Label nom        = new Label("🌱 " + c.getNom());
            nom.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
            Label type       = new Label("Type: " + (c.getType() != null ? c.getType() : "Non défini"));
            Label superficie = new Label("Superficie: " + c.getSuperficie() + " ha");

            HBox actions = new HBox(10);

            Button btnEdit       = new Button("✏");
            Button btnDelete     = new Button("🗑");
            Button btnDetails    = new Button("👁");
            Button btnDiagnostic = new Button("🔍");

            String baseStyle = """
                -fx-background-radius: 20;
                -fx-font-weight: bold;
                -fx-font-size: 16px;
                -fx-text-fill: white;
                -fx-pref-width: 45;
                -fx-pref-height: 40;
                -fx-cursor: hand;
                -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 5, 0, 0, 2);
                """;

            btnEdit.setStyle(baseStyle       + "-fx-background-color: #6B8E23;");
            btnDelete.setStyle(baseStyle     + "-fx-background-color: #C0392B;");
            btnDetails.setStyle(baseStyle    + "-fx-background-color: #2E8B57;");
            btnDiagnostic.setStyle(baseStyle + "-fx-background-color: #1565C0;");

            addHoverEffect(btnEdit,       "#556B2F");
            addHoverEffect(btnDelete,     "#922B21");
            addHoverEffect(btnDetails,    "#1E6F4C");
            addHoverEffect(btnDiagnostic, "#0D47A1");

            actions.getChildren().addAll(btnEdit, btnDelete, btnDetails, btnDiagnostic);

            btnEdit.setOnAction(e      -> ouvrirFormulaire(c));
            btnDetails.setOnAction(e   -> afficherDetails(c));
            btnDiagnostic.setOnAction(e-> ouvrirDiagnostic(c));

            btnDelete.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Voulez-vous supprimer " + c.getNom() + " ?");
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    try {
                        service.supprimer(c.getIdCulture());
                        masterData.setAll(service.getAll());
                        filtrerParType();
                    } catch (Exception ex) {
                        new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
                    }
                }
            });

            card.getChildren().addAll(nom, type, superficie, actions);
            flow.getChildren().add(card);
        }
        return flow;
    }

    // ══════════════════════════════════════════════════════════════════════
    // HOVER
    // ══════════════════════════════════════════════════════════════════════
    private void addHoverEffect(Button button, String hoverColor) {
        String normalStyle = button.getStyle();
        button.setOnMouseEntered(e -> button.setStyle(
                normalStyle.replaceFirst("-fx-background-color: #[^;]+;",
                        "-fx-background-color: " + hoverColor + ";")));
        button.setOnMouseExited(e -> button.setStyle(normalStyle));
    }

    // ══════════════════════════════════════════════════════════════════════
    // RAFRAÎCHIR
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    private void handleRefresh() {
        txtRecherche.clear();
        cbType.getSelectionModel().selectFirst();
        masterData.setAll(service.getAll());
        setupPagination(masterData);
        new Alert(Alert.AlertType.INFORMATION, "Liste mise à jour !").showAndWait();
    }

    // ══════════════════════════════════════════════════════════════════════
    // HELPER : charger un FXML dans la zone dynamique du dashboard
    // ══════════════════════════════════════════════════════════════════════
    private void chargerDansContentPane(String fxmlPath, ContentPaneConsumer consumer) {
        if (dashboardController == null) {
            System.err.println("⚠️ dashboardController est null");
            return;
        }
        AnchorPane targetPane = dashboardController.getDynamicContent();
        if (targetPane == null) {
            System.err.println("⚠️ dynamicContent est null");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();

            if (consumer != null) consumer.accept(loader.getController());

            targetPane.getChildren().clear();
            targetPane.getChildren().add(content);
            AnchorPane.setTopAnchor(content,    0.0);
            AnchorPane.setBottomAnchor(content, 0.0);
            AnchorPane.setLeftAnchor(content,   0.0);
            AnchorPane.setRightAnchor(content,  0.0);

            System.out.println("✅ Chargé dans dynamicContent : " + fxmlPath);

        } catch (Exception e) {
            System.err.println("❌ Erreur chargement " + fxmlPath + " : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FunctionalInterface
    private interface ContentPaneConsumer {
        void accept(Object controller);
    }

    // ══════════════════════════════════════════════════════════════════════
    // ✅ AJOUTER — s'ouvre dans le contentPane (pas un nouveau Stage)
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    private void openAddForm() {
        chargerDansContentPane("/CultureForm.fxml", ctrl -> {
            CultureController c = (CultureController) ctrl;
            c.setOnSuccess(() -> dashboardController.handleShowCultureDirect());
            c.setOnCancel(() -> dashboardController.handleShowCultureDirect());
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // ✅ MODIFIER — s'ouvre dans le contentPane (pas un nouveau Stage)
    // ══════════════════════════════════════════════════════════════════════
    private void ouvrirFormulaire(Culture culture) {
        chargerDansContentPane("/CultureForm.fxml", ctrl -> {
            CultureController c = (CultureController) ctrl;
            c.remplirFormulaire(culture);
            c.setOnSuccess(() -> dashboardController.handleShowCultureDirect());
            c.setOnCancel(() -> dashboardController.handleShowCultureDirect());
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // DÉTAILS — reste en popup (fiche de lecture, pas d'édition)
    // ══════════════════════════════════════════════════════════════════════
    private void afficherDetails(Culture c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CultureDetails.fxml"));
            Scene scene = new Scene(loader.load());

            CultureDetailsController controller = loader.getController();
            controller.setDashboardController(dashboardController);
            controller.setCulture(c);

            Stage stage = new Stage();
            stage.setTitle("Détails de " + c.getNom());
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // DIAGNOSTIC — dans le contentPane via dashboard
    // ══════════════════════════════════════════════════════════════════════
    private void ouvrirDiagnostic(Culture culture) {
        if (dashboardController != null) {
            dashboardController.ouvrirDiagnosticPourCulture(culture);
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/DiagnosticForm.fxml"));
                Scene scene = new Scene(loader.load());
                DiagnosticController ctrl = loader.getController();
                ctrl.setCulture(culture);
                Stage stage = new Stage();
                stage.setTitle("🌱 Diagnostic de " + culture.getNom());
                stage.setScene(scene);
                stage.setMaximized(true);
                stage.show();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}