package Billing;

import Config.DeploymentLevel;
import Config.MongoConfig;
import Organization.Organization;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.stripe.Stripe;
import com.stripe.exception.CardException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.PaymentMethodAttachParams;
import com.stripe.param.SubscriptionCreateParams;
import io.javalin.http.Handler;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import static com.mongodb.client.model.Filters.eq;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class BillingController {
  private String apiKey = System.getenv("STRIPE_TEST_KEY");
  private MongoDatabase db;
  private MongoCollection<Organization> orgCollection;

  public Handler createSubscription =
      ctx -> {
        ctx.req.getSession().invalidate();
        Stripe.apiKey = apiKey;
        JSONObject req = new JSONObject(ctx.body());
        String customerId = req.getString("customerId");
        String paymentMethodId = req.getString("paymentMethodId");
        String priceId = req.getString("priceId");
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
                        .setPrice(priceId) // found on dashboard in product
                        .build())
                .setCustomer(customer.getId())
                .setCollectionMethod(SubscriptionCreateParams.CollectionMethod.CHARGE_AUTOMATICALLY)
                .addAllExpand(Arrays.asList("latest_invoice.payment_intent"))
                .build();
        Subscription subscription = Subscription.create(subCreateParams);

        // creating object to be returned
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("subscriptionId", subscription.getId());
        responseData.put(
            "clientSecret",
            subscription.getLatestInvoiceObject().getPaymentIntentObject().getClientSecret());
        ctx.result(StripeObject.PRETTY_PRINT_GSON.toJson(responseData));
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

    public Handler getCustomer =
        ctx -> {
            ctx.req.getSession().invalidate();
            Stripe.apiKey = apiKey;
            log.info("Attempting to find a customer");

            JSONObject req = new JSONObject(ctx.body());
            String customerEmail = req.getString("customerEmail");

            // query mongoDB under organization for the field customerId that matches with the customerEmail
            log.info("database found");
            db = MongoConfig.getDatabase(DeploymentLevel.STAGING);

            if (db == null) {
                throw new IllegalStateException("DB cannot be null");
            }

            orgCollection = db.getCollection("organization", Organization.class);
            if (orgCollection == null){
                throw new IllegalStateException("Org collection cannot be null");
            }
            log.info("Collection found");

            Organization org = orgCollection.find(eq("email", customerEmail)).first();
            if (org == null){
                throw new IllegalStateException("Org document cannot be null");
            }
            log.info("Organization found");

            // get customerId from org and retrieve corresponding customer obj from stripe
            String customerId = org.getCustomerId();
            Customer customer = Customer.retrieve(customerId);
            log.info("Found customer: {}", customer);
            ctx.result(customer.toJson());
        };
        public Handler getSubscription =
            ctx -> {
                ctx.req.getSession().invalidate();
                Stripe.apiKey = apiKey;
                log.info("Attempting to find a subscription");

                JSONObject req = new JSONObject(ctx.body());
                String subscriptionId = req.getString("subscriptionId");

                Subscription subscription = Subscription.retrieve(subscriptionId);
                log.info("Found subscription: {}", subscription);
                ctx.result(subscription.toJson());
            };

}