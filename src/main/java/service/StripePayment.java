package service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

public class StripePayment {

    // ✅ Clé directement en dur (pour le développement)
    private static final String STRIPE_SECRET_KEY = "sk_test_REPLACE_WITH_STRIPE_SECRET";

    static {
        Stripe.apiKey = STRIPE_SECRET_KEY;
    }

    public static String createCheckoutSession(long amountInCents, String productDescription,
                                               String successUrl, String cancelUrl) throws StripeException {

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(
                                SessionCreateParams.LineItem.PriceData.builder()
                                        .setCurrency("eur")
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
