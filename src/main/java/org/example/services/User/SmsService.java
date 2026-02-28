package org.example.services.User;
import com.vonage.client.sms.messages.TextMessage;
import org.example.utils.ConfigLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import com.vonage.client.VonageClient;
import com.vonage.client.sms.SmsSubmissionResponse;
import com.vonage.client.sms.MessageStatus;
public class SmsService {

    // ✅ Remplace par tes vraies clés Vonage
    private static final String API_KEY = ConfigLoader.get("vonage.api.key");
    private static final String API_SECRET = ConfigLoader.get("vonage.api.secret");
    private static final String SENDER_NAME = "AgriConnect"; // Nom affiché (max 11 caractères)

    // ✅ Stockage temporaire des codes (userId -> code)
    private static final Map<Integer, String> verificationCodes = new HashMap<>();

    private static  VonageClient client;

    static {
        try {
            client = VonageClient.builder()
                    .apiKey(API_KEY)
                    .apiSecret(API_SECRET)
                    .build();
            System.out.println("✅ Vonage client initialisé");
        } catch (Exception e) {
            System.err.println("❌ Erreur initialisation Vonage : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Génère un code à 6 chiffres
     */
    public String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Envoie un SMS avec le code de vérification
     */
    public boolean sendVerificationCode(String phoneNumber, int userId) {
        try {
            String code = generateCode();
            verificationCodes.put(userId, code);

            // ✅ Enlever le + si présent (Vonage n'aime pas le +)
            String cleanPhone = phoneNumber.startsWith("+") ? phoneNumber.substring(1) : phoneNumber;

            String messageText = "Votre code de verification AgriConnect est : " + code;

            System.out.println("📤 Envoi SMS vers : " + cleanPhone);
            System.out.println("🔐 Code généré : " + code);

            TextMessage message = new TextMessage(SENDER_NAME, cleanPhone, messageText);

            SmsSubmissionResponse response = client.getSmsClient().submitMessage(message);

            if (response.getMessages().get(0).getStatus() == MessageStatus.OK) {
                System.out.println("✅ SMS envoyé avec succès !");
                System.out.println("📱 Message ID : " + response.getMessages().get(0).getId());
                return true;
            } else {
                System.err.println("❌ Échec envoi SMS : " + response.getMessages().get(0).getErrorText());

                // ✅ Afficher le code quand même pour debug
                System.out.println("\n⚠️  Code généré quand même :");
                System.out.println("🔐 CODE : " + code);
                System.out.println("👤 User ID : " + userId);
                System.out.println("\n");

                return false;
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi SMS : " + e.getMessage());
            e.printStackTrace();

            // ✅ Afficher le code quand même pour debug
            String storedCode = verificationCodes.get(userId);
            if (storedCode != null) {
                System.out.println("\n⚠️  En cas d'erreur, voici le code :");
                System.out.println("🔐 CODE : " + storedCode);
                System.out.println("👤 User ID : " + userId);
                System.out.println("\n");
            }

            return false;
        }
    }

    /**
     * Vérifie si le code saisi est correct
     */
    public boolean verifyCode(int userId, String enteredCode) {
        String storedCode = verificationCodes.get(userId);

        if (storedCode == null) {
            System.err.println("❌ Aucun code trouvé pour user " + userId);
            return false;
        }

        boolean isValid = storedCode.equals(enteredCode.trim());

        if (isValid) {
            verificationCodes.remove(userId);
            System.out.println("✅ Code valide pour user " + userId);
        } else {
            System.err.println("❌ Code invalide pour user " + userId);
            System.err.println("   Attendu : " + storedCode);
            System.err.println("   Reçu : " + enteredCode);
        }

        return isValid;
    }

    /**
     * Supprime le code stocké
     */
    public void clearCode(int userId) {
        verificationCodes.remove(userId);
        System.out.println("🗑️  Code supprimé pour user " + userId);
    }
}
