package services;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class WhatsAppService {

    private static final String ACCOUNT_SID = "AC4c0a9f4ad765c48a91de3638a437efa7";
    private static final String AUTH_TOKEN   = "95da7e44094b5667ccc5980d45fac649";
    private static final String FROM         = "whatsapp:+14155238886";
    private static final String TO           = "whatsapp:+21652137911";

    public static void envoyerPromo(String nomProduit, double prix, double remise) {
        try {
            Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

            String message = "🌾 *AgriConnect - Nouvelle Promo !*\n\n"
                    + "📦 Produit : *" + nomProduit + "*\n"
                    + "💰 Prix : " + String.format("%.2f", prix) + " DT\n"
                    + "🔥 Remise : " + remise + "%\n"
                    + "✅ Commandez maintenant sur AgriConnect !";

            Message.creator(
                    new PhoneNumber(TO),
                    new PhoneNumber(FROM),
                    message
            ).create();

            System.out.println("✅ WhatsApp envoyé avec succès !");

        } catch (Exception e) {
            System.err.println("❌ Erreur WhatsApp : " + e.getMessage());
        }
    }

    public static void envoyerStatutCommande(String nomProduit, String statut) {
        try {
            Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

            String emoji = switch (statut) {
                case "VALIDEE"  -> "✅";
                case "EXPEDIEE" -> "🚚";
                case "LIVREE"   -> "📦";
                case "ANNULEE"  -> "❌";
                case "PAYEE"    -> "💳";
                default         -> "📋";
            };

            String message = "🌾 *AgriConnect - Suivi Commande*\n\n"
                    + "📦 Produit : *" + nomProduit + "*\n"
                    + emoji + " Statut : *" + statut + "*\n"
                    + "Merci de votre confiance !";

            Message.creator(
                    new PhoneNumber(TO),
                    new PhoneNumber(FROM),
                    message
            ).create();

            System.out.println("✅ WhatsApp statut envoyé !");

        } catch (Exception e) {
            System.err.println("❌ Erreur WhatsApp : " + e.getMessage());
        }
    }
}