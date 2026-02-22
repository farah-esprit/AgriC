package service;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
public class CaptchaService {
    private static final String SECRET_KEY = "6Leks3MsAAAAAGZlCm8MkN9--ee3jQ4QoZpcrxgJ";
    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    private static HttpServer server;
    private static int captchaPort = 8765;

    public static void startServer() {
        if (server != null) {
            System.out.println("⚠️ Serveur déjà démarré");
            return;
        }

        try {
            server = HttpServer.create(new InetSocketAddress(captchaPort), 0);

            server.createContext("/captcha", exchange -> {
                String html = getRecaptchaHTML();
                byte[] response = html.getBytes("UTF-8");

                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);

                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();
            });

            server.setExecutor(null);
            server.start();

            System.out.println("✅ Serveur CAPTCHA démarré sur http://localhost:" + captchaPort + "/captcha");

        } catch (Exception e) {
            System.err.println("❌ Erreur démarrage serveur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void stopServer() {
        if (server != null) {
            server.stop(0);
            server = null;
            System.out.println("🛑 Serveur CAPTCHA arrêté");
        }
    }

    private static String getRecaptchaHTML() {
        return "<!DOCTYPE html><html><head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<script src='https://www.google.com/recaptcha/api.js?onload=onloadCallback' async defer></script>" +
                "<style>" +
                "body { " +
                "  margin: 0; " +
                "  padding: 10px; " +
                "  background: #f9f9f9; " +
                "  display: flex; " +
                "  justify-content: center; " +
                "  align-items: center; " +
                "  height: 78px; " +
                "}" +
                "</style>" +
                "</head><body>" +
                "<div id='recaptcha-container'></div>" +
                "<script>" +
                "console.log('🔧 Script CAPTCHA chargé');" +
                // ✅ ATTENDRE QUE JAVACONNECTOR SOIT PRÊT
                "function waitForJavaConnector(callback) {" +
                "  var checkInterval = setInterval(function() {" +
                "    console.log('⏳ Attente de javaConnector...');" +
                "    if (typeof window.javaConnector !== 'undefined') {" +
                "      console.log('✅ JavaConnector détecté !');" +
                "      clearInterval(checkInterval);" +
                "      callback();" +
                "    }" +
                "  }, 100);" +  // Vérifier toutes les 100ms
                "}" +

                "var onloadCallback = function() {" +
                "  console.log('✅ reCAPTCHA API chargée');" +
                "  " +
        "  waitForJavaConnector(function() {" +
                "    console.log('🎨 Rendu du CAPTCHA...');" +
                "    try {" +
                "      grecaptcha.render('recaptcha-container', {" +
                "        'sitekey': '6Leks3MsAAAAAOjxw7wjedQl8FsjjhPRYojX4w0v'," +
                "        'callback': onSuccess," +
                "        'error-callback': onError" +
                "      });" +
                "      console.log('✅ Widget CAPTCHA rendu');" +
                "    } catch(e) {" +
                "      console.error('❌ Erreur render:', e);" +
                "    }" +
                "  });" +
                "};" +

                "function onSuccess(token) {" +
                "  console.log('✅✅✅ CAPTCHA RÉSOLU !');" +
                "  console.log('📦 Token reçu:', token.substring(0, 30) + '...');" +
                "  " +
                "  if (typeof window.javaConnector !== 'undefined') {" +
                "    console.log('📤 Appel de javaConnector.captchaVerified()');" +
                "    try {" +
                "      window.javaConnector.captchaVerified(token);" +
                "      console.log('✅ Callback Java appelé avec succès');" +
                "    } catch(e) {" +
                "      console.error('❌ Erreur lors de l\\'appel Java:', e);" +
                "    }" +
                "  } else {" +
                "    console.error('❌❌❌ javaConnector TOUJOURS introuvable !');" +
                "  }" +
                "}" +

                "function onError(error) {" +
                "  console.error('❌ Erreur CAPTCHA:', error);" +
                "}" +
                "</script>" +
                "</body></html>";
    }

    public boolean verifyToken(String token) {
        try {
            System.out.println("🔍 Vérification du token : " + token.substring(0, 30) + "...");

            URL url = new URL(VERIFY_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            String body = "secret=" + URLEncoder.encode(SECRET_KEY, "UTF-8")
                    + "&response=" + URLEncoder.encode(token, "UTF-8");

            conn.getOutputStream().write(body.getBytes());

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);

            String json = response.toString();
            System.out.println("📥 Réponse Google : " + json);

            boolean success = json.contains("\"success\": true")
                    || json.contains("\"success\":true");

            if (success) {
                System.out.println("✅✅✅ CAPTCHA VALIDE !");
            } else {
                System.out.println("❌❌❌ CAPTCHA INVALIDE !");
            }

            return success;

        } catch (Exception e) {
            System.err.println("❌ Erreur vérification CAPTCHA : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}