package org.example.services.User;


import com.sun.net.httpserver.HttpServer;
import org.example.utils.ConfigLoader;

import java.io.*;
import java.net.*;

public class CaptchaService {

    // ✅ Toutes les constantes via ConfigLoader
    private static final String SECRET_KEY = ConfigLoader.get("recaptcha.secret.key");
    private static final String SITE_KEY = ConfigLoader.get("recaptcha.site.key");
    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify"; // ✅ static
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
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<script src='https://www.google.com/recaptcha/api.js?onload=onloadCallback&render=explicit' defer></script>");
        html.append("<style>");
        html.append("body { margin: 0; padding: 10px; background: #f9f9f9; display: flex; justify-content: center; align-items: center; height: 78px; }");
        html.append("#status { font-size: 12px; color: #666; margin-bottom: 10px; }");
        html.append("</style>");
        html.append("</head><body>");
        html.append("<div id='status'>⏳ Initialisation...</div>");
        html.append("<div id='recaptcha-container'></div>");
        html.append("<script>");
        html.append("console.log('🔧 Script CAPTCHA chargé');");

        html.append("function waitForJavaConnector(callback, maxAttempts) {");
        html.append("  maxAttempts = maxAttempts || 50;");
        html.append("  var attempts = 0;");
        html.append("  var checkInterval = setInterval(function() {");
        html.append("    attempts++;");
        html.append("    if (typeof window.javaConnector !== 'undefined') {");
        html.append("      document.getElementById('status').innerHTML = '✅ Connecteur prêt';");
        html.append("      clearInterval(checkInterval);");
        html.append("      callback(true);");
        html.append("    } else if (attempts >= maxAttempts) {");
        html.append("      document.getElementById('status').innerHTML = '❌ Erreur';");
        html.append("      document.getElementById('status').style.color = 'red';");
        html.append("      clearInterval(checkInterval);");
        html.append("      callback(false);");
        html.append("    }");
        html.append("  }, 100);");
        html.append("}");

        html.append("var onloadCallback = function() {");
        html.append("  document.getElementById('status').innerHTML = '⏳ Connexion...';");
        html.append("  waitForJavaConnector(function(success) {");
        html.append("    if (success) {");
        html.append("      document.getElementById('status').style.display = 'none';");
        html.append("      try {");
        html.append("        grecaptcha.render('recaptcha-container', {");
        // ✅ SITE_KEY depuis ConfigLoader
        html.append("          'sitekey': '" + SITE_KEY + "',");
        html.append("          'callback': onSuccess,");
        html.append("          'error-callback': onError,");
        html.append("          'expired-callback': onExpired");
        html.append("        });");
        html.append("      } catch(e) {");
        html.append("        document.getElementById('status').innerHTML = '❌ Erreur: ' + e.message;");
        html.append("        document.getElementById('status').style.display = 'block';");
        html.append("      }");
        html.append("    }");
        html.append("  }, 50);");
        html.append("};");

        html.append("function onSuccess(token) {");
        html.append("  console.log('🎉 CAPTCHA RÉSOLU !');");
        html.append("  var maxRetry = 10;");
        html.append("  var retry = 0;");
        html.append("  var tryCall = function() {");
        html.append("    if (typeof window.javaConnector !== 'undefined' && ");
        html.append("        typeof window.javaConnector.captchaVerified === 'function') {");
        html.append("      try {");
        html.append("        window.javaConnector.captchaVerified(token);");
        html.append("        console.log('✅ Token envoyé à Java !');");
        html.append("      } catch(e) {");
        html.append("        console.error('❌ Erreur appel Java:', e);");
        html.append("      }");
        html.append("    } else if (retry < maxRetry) {");
        html.append("      retry++;");
        html.append("      setTimeout(tryCall, 300);");
        html.append("    }");
        html.append("  };");
        html.append("  tryCall();");
        html.append("}");

        html.append("function onError(error) {");
        html.append("  document.getElementById('status').innerHTML = '❌ Erreur';");
        html.append("  document.getElementById('status').style.display = 'block';");
        html.append("  document.getElementById('status').style.color = 'red';");
        html.append("}");

        html.append("function onExpired() {");
        html.append("  document.getElementById('status').innerHTML = '⚠️ Expiré';");
        html.append("  document.getElementById('status').style.display = 'block';");
        html.append("  document.getElementById('status').style.color = 'orange';");
        html.append("}");

        html.append("</script>");
        html.append("</body></html>");
        return html.toString();
    }

    // ✅ VERIFY_URL est static donc accessible ici
    public boolean verifyToken(String token) {
        try {
            System.out.println("🔍 Vérification du token...");
            URL url = new URL(VERIFY_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            String body = "secret=" + URLEncoder.encode(SECRET_KEY, "UTF-8")
                    + "&response=" + URLEncoder.encode(token, "UTF-8");

            OutputStream out = conn.getOutputStream();
            out.write(body.getBytes("UTF-8"));
            out.flush();
            out.close();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();

            String json = response.toString();
            System.out.println("📥 Réponse Google : " + json);

            boolean success = json.contains("\"success\": true") || json.contains("\"success\":true");
            System.out.println(success ? "✅ CAPTCHA VALIDE !" : "❌ CAPTCHA INVALIDE !");
            return success;

        } catch (Exception e) {
            System.err.println("❌ Exception : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
