package Billing;

import Config.Message;
import Config.Service;
import User.UserMessage;
import com.google.gson.JsonSyntaxException;
import com.stripe.model.*;
import com.stripe.net.ApiResource;

public class HandleWebhooksService implements Service {
  String payload;

  public HandleWebhooksService(String payload) {
    this.payload = payload;
  }

  @Override
  public Message executeAndGetResponse() {
    Event event = null;
    try {
      event = ApiResource.GSON.fromJson(payload, Event.class);
    } catch (JsonSyntaxException e) {
      // Invalid payload
      System.out.println("Webhook error while parsing basic request.");
      return UserMessage.HASH_FAILURE;
    }
    // Deserialize the nested object inside the event
    EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
    StripeObject stripeObject = null;
    if (dataObjectDeserializer.getObject().isPresent()) {
      stripeObject = dataObjectDeserializer.getObject().get();
    } else {
      // Deserialization failed, probably due to an API version mismatch.
      // Refer to the Javadoc documentation on `EventDataObjectDeserializer` for
      // instructions on how to handle this case, or return an error here.
    }
    // Handle the event
    switch (event.getType()) {
      case "payment_intent.succeeded":
        PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
        System.out.println("Payment for " + paymentIntent.getAmount() + " succeeded.");
        // Then define and call a method to handle the successful payment intent.
        // handlePaymentIntentSucceeded(paymentIntent);
        break;
      case "payment_method.attached":
        PaymentMethod paymentMethod = (PaymentMethod) stripeObject;
        // Then define and call a method to handle the successful attachment of a PaymentMethod.
        // handlePaymentMethodAttached(paymentMethod);

        break;
      default:
        System.out.println("Unhandled event type: " + event.getType());
        break;
    }
    System.out.println(event.getType());
    return UserMessage.SUCCESS;
  }
}
