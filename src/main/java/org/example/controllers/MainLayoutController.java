package org.example.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;

public class MainLayoutController {

    @FXML
    private StackPane contentArea;

    // ================= INITIAL LOAD =================
    @FXML
    public void initialize() {
        loadPage("/CreerCultureList.fxml"); // Page par défaut
    }

    // ================= NAVIGATION =================
    @FXML
    public void goDashboard() {
        loadPage("/DashboardAgriculteur.fxml");
    }

    @FXML
    public void goCultures() {
        loadPage("/CreerCultureList.fxml");
    }


    @FXML
    public void goDiagnostics() {
        loadPage("/DiagnosticForm.fxml");
    }

    @FXML
    public void goIrrigation() {
        loadPage("/Irrigation.fxml");
    }

    @FXML
    public void goParametres() {
        loadPage("/Parametres.fxml");
    }

    // ================= LOAD METHOD =================
    private void loadPage(String path) {

        try {

            URL resource = getClass().getResource(path);

            if (resource == null) {
                System.out.println("FXML non trouvé : " + path);
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent page = loader.load();

            // Animation setup
            page.setOpacity(0);
            page.setTranslateX(30);

            contentArea.getChildren().setAll(page);

            // Fade animation
            FadeTransition fade = new FadeTransition(Duration.millis(350), page);
            fade.setFromValue(0);
            fade.setToValue(1);

            // Slide animation
            TranslateTransition slide = new TranslateTransition(Duration.millis(350), page);
            slide.setFromX(30);
            slide.setToX(0);

            fade.play();
            slide.play();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
