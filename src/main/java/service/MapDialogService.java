package service;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Optional;

public final class MapDialogService {

    private MapDialogService() {}

    public static Optional<GeoWeatherService.LocationResult> pickLocation(Window owner, GeoWeatherService geoWeatherService, String initialQuery) {
        Dialog<GeoWeatherService.LocationResult> dialog = new Dialog<>();
        dialog.setTitle("Choisir une localisation sur la carte");
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.initModality(Modality.APPLICATION_MODAL);

        ButtonType useBtn = new ButtonType("Utiliser cette localisation", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(useBtn, ButtonType.CANCEL);

        TextField searchField = new TextField(initialQuery == null ? "" : initialQuery);
        searchField.setPromptText("Rechercher une adresse ou une ville...");
        Button searchBtn = new Button("Rechercher");
        searchBtn.getStyleClass().add("search-button");

        WebView webView = new WebView();
        webView.setPrefSize(860, 520);
        WebEngine engine = webView.getEngine();
        engine.loadContent(INTERACTIVE_MAP_HTML, "text/html");

        Label statusLabel = new Label("Cliquez sur la carte ou utilisez la recherche.");

        Runnable searchAction = () -> {
            String query = safe(searchField.getText());
            if (query.isBlank()) {
                return;
            }
            Optional<GeoWeatherService.LocationResult> geo = geoWeatherService.geocode(query);
            if (geo.isEmpty()) {
                statusLabel.setText("Adresse introuvable.");
                return;
            }
            GeoWeatherService.LocationResult loc = geo.get();
            setMapPoint(engine, loc.lat(), loc.lon(), loc.displayName());
            statusLabel.setText("Adresse trouvee: " + loc.displayName());
        };

        searchBtn.setOnAction(e -> searchAction.run());
        searchField.setOnAction(e -> searchAction.run());

        HBox topBar = new HBox(8, searchField, searchBtn);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        VBox content = new VBox(10, topBar, webView, statusLabel);
        content.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefSize(900, 700);

        if (!safe(initialQuery).isBlank()) {
            searchAction.run();
        }

        dialog.setResultConverter(btn -> {
            if (btn != useBtn) {
                return null;
            }
            Object latObj = engine.executeScript("window.selectedLat");
            Object lonObj = engine.executeScript("window.selectedLon");
            if (!(latObj instanceof Number) || !(lonObj instanceof Number)) {
                statusLabel.setText("Selection invalide. Cliquez sur la carte d'abord.");
                return null;
            }
            double lat = ((Number) latObj).doubleValue();
            double lon = ((Number) lonObj).doubleValue();
            String label = String.valueOf(engine.executeScript("window.selectedAddress || ''"));
            String displayName = safe(label).isBlank() ? ("lat=" + lat + ", lon=" + lon) : label;
            return new GeoWeatherService.LocationResult(displayName, displayName, lat, lon);
        });

        return dialog.showAndWait();
    }

    public static void openMapViewer(Window owner, String title, double lat, double lon, String label) {
        Stage popup = new Stage();
        if (owner != null) {
            popup.initOwner(owner);
        }
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle(title);

        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();
        engine.loadContent(INTERACTIVE_MAP_HTML, "text/html");
        setMapPoint(engine, lat, lon, label);

        BorderPane root = new BorderPane(webView);
        root.setPadding(new Insets(8));
        popup.setScene(new Scene(root, 900, 620));
        popup.showAndWait();
    }

    private static void setMapPoint(WebEngine engine, double lat, double lon, String label) {
        String js = "window.setMapPoint(" + lat + "," + lon + ",'" + escapeJs(label) + "')";
        try {
            engine.executeScript(js);
        } catch (Exception ignored) {
            // If page isn't ready yet, retry quickly.
            new Thread(() -> {
                try {
                    Thread.sleep(300);
                    javafx.application.Platform.runLater(() -> {
                        try {
                            engine.executeScript(js);
                        } catch (Exception ignored2) {
                        }
                    });
                } catch (InterruptedException ignored2) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String escapeJs(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\").replace("'", "\\'");
    }

    private static final String INTERACTIVE_MAP_HTML = """
        <!doctype html>
        <html>
        <head>
          <meta charset="utf-8"/>
          <meta name="viewport" content="width=device-width,initial-scale=1"/>
          <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
          <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
          <style>
            html, body, #map { width:100%; height:100%; margin:0; padding:0; }
            .leaflet-container { font: 12px "Segoe UI", sans-serif; }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <script>
            const map = L.map('map').setView([36.8065, 10.1815], 6);
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
              maxZoom: 19,
              attribution: '&copy; OpenStreetMap contributors'
            }).addTo(map);

            let marker = null;
            window.selectedLat = null;
            window.selectedLon = null;
            window.selectedAddress = '';

            async function reverseGeocode(lat, lon) {
              try {
                const url = `https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lon}&format=json`;
                const res = await fetch(url, {headers: {'Accept':'application/json'}});
                const data = await res.json();
                return data.display_name || `lat=${lat.toFixed(6)}, lon=${lon.toFixed(6)}`;
              } catch (e) {
                return `lat=${lat.toFixed(6)}, lon=${lon.toFixed(6)}`;
              }
            }

            window.setMapPoint = async function(lat, lon, label) {
              if (marker) { map.removeLayer(marker); }
              marker = L.marker([lat, lon]).addTo(map);
              map.setView([lat, lon], 14);
              const finalLabel = label && label.length > 0 ? label : await reverseGeocode(lat, lon);
              marker.bindPopup(finalLabel).openPopup();
              window.selectedLat = lat;
              window.selectedLon = lon;
              window.selectedAddress = finalLabel;
            }

            map.on('click', async function(e) {
              await window.setMapPoint(e.latlng.lat, e.latlng.lng, '');
            });
          </script>
        </body>
        </html>
        """;
}
