package service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeoWeatherService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8))
        .build();

    public record LocationResult(String query, String displayName, double lat, double lon) {}

    public Optional<LocationResult> geocode(String address) {
        if (address == null || address.isBlank()) {
            return Optional.empty();
        }
        try {
            String encoded = URLEncoder.encode(address.trim(), StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search?q=" + encoded + "&format=json&limit=1";

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("User-Agent", "EventManagementSystem/1.0")
                .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }

            String body = response.body();
            String lat = extract(body, "\"lat\"\\s*:\\s*\"([^\"]+)\"");
            String lon = extract(body, "\"lon\"\\s*:\\s*\"([^\"]+)\"");
            String displayName = extract(body, "\"display_name\"\\s*:\\s*\"([^\"]+)\"");

            if (lat == null || lon == null) {
                return Optional.empty();
            }
            return Optional.of(new LocationResult(
                address.trim(),
                displayName != null ? displayName : address.trim(),
                Double.parseDouble(lat),
                Double.parseDouble(lon)
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public String weatherForDate(double lat, double lon, LocalDate date) {
        if (date == null) {
            return "Date meteo invalide.";
        }
        try {
            String latStr = String.format(Locale.US, "%.6f", lat);
            String lonStr = String.format(Locale.US, "%.6f", lon);
            String dateStr = date.toString();

            String url = "https://api.open-meteo.com/v1/forecast?latitude=" + latStr
                + "&longitude=" + lonStr
                + "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,weathercode"
                + "&timezone=auto"
                + "&start_date=" + dateStr
                + "&end_date=" + dateStr;

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return "Meteo non disponible pour le moment.";
            }

            String body = response.body();
            String max = extractFirstArrayValue(body, "temperature_2m_max");
            String min = extractFirstArrayValue(body, "temperature_2m_min");
            String rain = extractFirstArrayValue(body, "precipitation_probability_max");

            if (max == null || min == null) {
                return "Meteo non disponible pour cette date.";
            }
            return "Meteo prevue le " + dateStr + ": min " + min + " C, max " + max + " C, pluie " + (rain != null ? rain : "?") + "%.";
        } catch (Exception e) {
            return "Meteo non disponible (erreur reseau/API).";
        }
    }

    public String mapLink(double lat, double lon) {
        String latStr = String.format(Locale.US, "%.6f", lat);
        String lonStr = String.format(Locale.US, "%.6f", lon);
        return "https://www.openstreetmap.org/?mlat=" + latStr + "&mlon=" + lonStr + "#map=13/" + latStr + "/" + lonStr;
    }

    public Optional<byte[]> downloadStaticMap(double lat, double lon, int width, int height) {
        String latStr = String.format(Locale.US, "%.6f", lat);
        String lonStr = String.format(Locale.US, "%.6f", lon);

        List<String> urls = List.of(
            "https://staticmap.openstreetmap.de/staticmap.php?center="
                + latStr + "," + lonStr
                + "&zoom=14&size=" + width + "x" + height
                + "&markers=" + latStr + "," + lonStr + ",red-pushpin",
            "https://static-maps.yandex.ru/1.x/?ll="
                + lonStr + "," + latStr
                + "&size=" + Math.min(width, 650) + "," + Math.min(height, 450)
                + "&z=14&l=map&pt=" + lonStr + "," + latStr + ",pm2rdm"
        );

        for (String url : urls) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(12))
                    .header("Accept", "image/*")
                    .header("User-Agent", "EventManagementSystem/1.0")
                    .build();
                HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
                String contentType = response.headers().firstValue("Content-Type").orElse("");
                if (response.statusCode() == 200 && contentType.toLowerCase().contains("image")) {
                    return Optional.of(response.body());
                }
            } catch (Exception ignored) {
                // Try next provider
            }
        }
        return Optional.empty();
    }

    private String extract(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String extractFirstArrayValue(String text, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\[([^\\]]+)]").matcher(text);
        if (!matcher.find()) {
            return null;
        }
        String values = matcher.group(1).trim();
        int comma = values.indexOf(',');
        return comma > -1 ? values.substring(0, comma).trim() : values;
    }
}
