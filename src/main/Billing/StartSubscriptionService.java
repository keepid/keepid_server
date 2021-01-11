package Billing;

import Config.Message;
import Config.Service;
import User.UserMessage;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import java.util.HashMap;
import java.util.Map;

public class StartSubscriptionService implements Service {

  @Override
  public Message executeAndGetResponse() {
    Stripe.apiKey =
        "sk_test_51I6VPaKeRS9YzLMIpHMl2QREUf3JFmyUXvHreOlRwChrLo9qVzR3DNWTGsw5sgfUCbBywqINKGmbGYB1Iq1GAEy5005cODVrXB";

    SessionCreateParams params =
        new SessionCreateParams.Builder()
            .setSuccessUrl("https://keep.id/home?session_id={CHECKOUT_SESSION_ID}")
            .setCancelUrl("https://keep.id/home")
            .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
            .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
            .addLineItem(
                new SessionCreateParams.LineItem.Builder()
                    .setQuantity(1L)
                    .setPrice("price_1I6WEzKeRS9YzLMIWkY0hVfA")
                    .build())
            .build();

    try {
      Session session = Session.create(params);
      Map<String, Object> responseData = new HashMap<>();
      responseData.put("sessionId", session.getId());
      return UserMessage.SUCCESS;
    } catch (Exception e) {
      Map<String, Object> messageData = new HashMap<>();
      messageData.put("message", e.getMessage());
      return UserMessage.AUTH_FAILURE;
    }
  }
}
