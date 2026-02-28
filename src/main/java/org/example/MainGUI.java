package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MainGUI extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Charger le layout principal avec sidebar
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/User/login.fxml"));
            BorderPane root = loader.load();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

            primaryStage.setTitle("🌱 Gestion des Cultures - Agriconnect");
            primaryStage.setScene(scene);

            // 🔹 Fenêtre maximisée pour full screen (barre de tâches visible)
            primaryStage.setMaximized(true);

            // Si tu veux le vrai full screen sans barre de tâches
            // primaryStage.setFullScreen(true);

            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showErrorDialog("Erreur", "Impossible de démarrer l'application : " + e.getMessage());
        }
    }

    /**
     * Affiche une boîte de dialogue d'erreur
     */
    private void showErrorDialog(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR
        );
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        // 🔗 Test rapide de la connexion à la base de données
        try {
            System.out.println("🔗 Test de connexion à la base de données...");
            org.example.utils.MyDatabase db = org.example.utils.MyDatabase.getInstance();
            if (db.getConnection() != null && !db.getConnection().isClosed()) {
                System.out.println("✅ Connexion à la base de données établie!");
            }
        } catch (Exception e) {
            System.err.println("❌ Échec de connexion à la base de données: " + e.getMessage());
        }

        // 🚀 Lancer l'application JavaFX
        System.out.println("🚀 Démarrage de l'application JavaFX...");
        launch(args);
    }
}
