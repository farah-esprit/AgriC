package org.example.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent; // CORRECT IMPORT
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
        // Assurez-vous que ce fichier existe à la racine de vos resources
        loadPage("/DashboardAgriculteur.fxml");
    }

    // ================= NAVIGATION =================
    @FXML
    public void goDashboard(ActionEvent event) {
        loadPage("/DashboardAgriculteur.fxml");
    }

    @FXML
    public void goCultures(ActionEvent event) {
        loadPage("/CreerCultureList.fxml");
    }

    @FXML
    private void handleWeather(ActionEvent event) {
        System.out.println("Navigation : Météo");
        // loadPage("/Weather.fxml");
    }

    @FXML
    public void goDiagnostics(ActionEvent event) {
        loadPage("/DiagnosticForm.fxml");
    }

    @FXML
    public void handleScanPlant(ActionEvent event) {
        System.out.println("Lancement du scanner...");
         loadPage("/PlantNetView.fxml");
    }

    @FXML
    public void goIrrigation(ActionEvent event) {
        loadPage("/Irrigation.fxml");
    }

    @FXML
    public void goParametres(ActionEvent event) {
        loadPage("/Parametres.fxml");
    }

    // ================= LOAD METHOD AVEC ANIMATION =================
    private void loadPage(String path) {
        try {
            URL resource = getClass().getResource(path);

            if (resource == null) {
                System.err.println("ERREUR : FXML non trouvé au chemin : " + path);
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent page = loader.load();

            // Préparation pour l'animation
            page.setOpacity(0);
            page.setTranslateX(30);

            contentArea.getChildren().setAll(page);

            // Animation de fondu
            FadeTransition fade = new FadeTransition(Duration.millis(350), page);
            fade.setFromValue(0);
            fade.setToValue(1);

            // Animation de glissement
            TranslateTransition slide = new TranslateTransition(Duration.millis(350), page);
            slide.setFromX(30);
            slide.setToX(0);

            fade.play();
            slide.play();

        } catch (IOException e) {
            System.err.println("Erreur de chargement de la page : " + path);
            e.printStackTrace();
        }
    }
}