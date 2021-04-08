package Billing;

import com.stripe.Stripe;
import com.stripe.exception.CardException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.model.Subscription;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.PaymentMethodAttachParams;
import com.stripe.param.SubscriptionCreateParams;
import io.javalin.http.Handler;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class BillingController {
  private String apiKey = System.getenv("STRIPE_TEST_KEY");
  public Handler createCustomer =
      ctx -> {
        ctx.req.getSession().invalidate();
        log.info("Attempting to create a customer");
        Stripe.apiKey = apiKey;
        Map<String, Object> params = new HashMap<>();
        params.put("description", "Test Customer");
        Customer customer = Customer.create(params);
        log.info("Created customer");
        ctx.result(customer.toJson());
      };

  public Handler createSubscription =
      ctx -> {
        ctx.req.getSession().invalidate();
        JSONObject req = new JSONObject(ctx.body());
        String customerId = req.getString("customerId");
        String paymentMethodId = req.getString("paymentMethodId");
        String priceId = req.getString("priceId");
        Stripe.apiKey = apiKey;
        log.info("Retrieving customer");
        Customer customer = Customer.retrieve(customerId);
        log.info("Retrieving payment method");
        try {
          PaymentMethod pm = PaymentMethod.retrieve(paymentMethodId);
          pm.attach(PaymentMethodAttachParams.builder().setCustomer(customer.getId()).build());
          log.info("Successfully retrieved payment method");
        } catch (CardException e) {
          Map<String, String> responseError = new HashMap<>();
          responseError.put("error", e.getLocalizedMessage());
          JSONObject responseErrorJSON = new JSONObject(responseError);
          log.info("Failed in retrieving payment method");
          ctx.result(
              String.valueOf(
                  responseErrorJSON)); // might need to add return statement to break out here
        }
        log.info("Updating customer params");
        CustomerUpdateParams customerUpdateParams =
            CustomerUpdateParams.builder()
                .setInvoiceSettings(
                    CustomerUpdateParams.InvoiceSettings.builder()
                        .setDefaultPaymentMethod(paymentMethodId)
                        .build())
                .build();
        customer.update(customerUpdateParams);
        log.info("Creating subscription");
        SubscriptionCreateParams subCreateParams =
            SubscriptionCreateParams.builder()
                .addItem(
                    SubscriptionCreateParams.Item.builder()
                        .setPrice("price_1IdhgUGrHSIfLf0x2iofVmJV") // found on dashboard in product
                        .build())
                .setCustomer(customer.getId())
                .addAllExpand(Arrays.asList("latest_invoice.payment_intent"))
                .build();
        Subscription subscription = Subscription.create(subCreateParams);
        ctx.result(subscription.toJson());
      };

  public Handler cancelSubscription =
      ctx -> {
        ctx.req.getSession().invalidate();
        JSONObject req = new JSONObject(ctx.body());
        String subscriptionId = req.getString("subscriptionId");
        Stripe.apiKey = apiKey;
        log.info("Retrieving subscription");
        Subscription subscription = Subscription.retrieve(subscriptionId);
        log.info("Deleting subscription");
        Subscription deletedSubscription = subscription.cancel();
        ctx.result(deletedSubscription.toJson());
      };
}
