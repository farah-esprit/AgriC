package services;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

public class StripePayment {

    // REMPLACE PAR TA CLÉ SECRÈTE DE TEST (sk_test_xxx)
    private static final String STRIPE_SECRET_KEY = System.getenv("STRIPE_SECRET_KEY");

    static {
        Stripe.apiKey = STRIPE_SECRET_KEY;
    }

    /**
     * Crée une session Stripe Checkout et retourne l'URL à ouvrir
     * @param amountInCents montant en centimes (ex: 2500 = 25.00 €)
     * @param productDescription nom/description du produit
     * @param successUrl URL où rediriger après paiement réussi
     * @param cancelUrl URL où rediriger si annulation
     * @return URL de paiement Stripe
     */
    public static String createCheckoutSession(long amountInCents, String productDescription,
                                               String successUrl, String cancelUrl) throws StripeException {

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("eur") // change en "tnd" si besoin (Stripe supporte TND)
                                                .setUnitAmount(amountInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(productDescription)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }
}