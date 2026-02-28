package org.example.services.User;
import org.example.utils.ConfigLoader;
import org.example.utils.ConfigLoader;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;

import java.io.StringReader;
import java.util.Collections;


public class GoogleAuthService {

    private static final String APPLICATION_NAME = "AgriConnect";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    // 🔥 REMPLACEZ PAR VOS PROPRES IDENTIFIANTS
    private static final String CLIENT_ID = ConfigLoader.get("google.client.id");
    private static final String CLIENT_SECRET = ConfigLoader.get("google.client.secret");
    private static final String REDIRECT_URI = "http://localhost:8888";

    /**
     * Authentifie l'utilisateur avec Google et retourne ses informations
     */
    public GoogleUserInfo authenticate() {
        try {
            System.out.println("🔐 Démarrage de l'authentification Google...");

            // Configuration du client OAuth
            final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            // Créer les secrets client
            String clientSecretsJson = String.format(
                    "{\"installed\":{\"client_id\":\"%s\",\"client_secret\":\"%s\",\"redirect_uris\":[\"%s\"],\"auth_uri\":\"https://accounts.google.com/o/oauth2/auth\",\"token_uri\":\"https://oauth2.googleapis.com/token\"}}",
                    CLIENT_ID, CLIENT_SECRET, REDIRECT_URI
            );

            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                    JSON_FACTORY,
                    new StringReader(clientSecretsJson)
            );

            // Configuration du flux OAuth
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport,
                    JSON_FACTORY,
                    clientSecrets,
                    Collections.singletonList("https://www.googleapis.com/auth/userinfo.profile " +
                            "https://www.googleapis.com/auth/userinfo.email")
            )
                    .setAccessType("offline")
                    .build();

            // Ouvrir le navigateur pour l'authentification
            LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                    .setPort(8888)
                    .build();

            Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

            System.out.println("✅ Authentification réussie !");

            // Récupérer les informations de l'utilisateur
            Oauth2 oauth2 = new Oauth2.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            Userinfo userInfo = oauth2.userinfo().get().execute();

            GoogleUserInfo googleUser = new GoogleUserInfo();
            googleUser.setEmail(userInfo.getEmail());
            googleUser.setName(userInfo.getName());
            googleUser.setGoogleId(userInfo.getId());
            googleUser.setPicture(userInfo.getPicture());

            System.out.println("✅ Utilisateur : " + googleUser.getName());
            System.out.println("✅ Email : " + googleUser.getEmail());

            return googleUser;

        } catch (Exception e) {
            System.err.println("❌ Erreur authentification Google : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Classe pour stocker les informations de l'utilisateur Google
     */
    public static class GoogleUserInfo {
        private String googleId;
        private String email;
        private String name;
        private String picture;

        public String getGoogleId() { return googleId; }
        public void setGoogleId(String googleId) { this.googleId = googleId; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getPicture() { return picture; }
        public void setPicture(String picture) { this.picture = picture; }
    }
}
