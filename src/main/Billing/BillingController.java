package Billing;

import Billing.Services.*;
import Config.DeploymentLevel;
import Config.MongoConfig;
import Config.Message;
import Organization.Organization;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.stripe.Stripe;
import com.stripe.exception.CardException;
import com.stripe.model.*;
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

    public Handler getOrgEmail =
        ctx -> {
            String orgName = ctx.sessionAttribute("orgName");
            // Session attributes contains the following information: {orgName=Stripe testing, privilegeLevel=Admin, fullName=JASON ZHANG, username=stripetest}
            log.info("The orgName is: " + ctx.sessionAttribute("orgName"));

            GetOrgEmailService getOrgEmailService = new GetOrgEmailService(orgName);
            Message response = getOrgEmailService.executeAndGetResponse();

            if (response == BillingMessage.SUCCESS){
                JSONObject responseJSON = new JSONObject();
                responseJSON.put("orgEmail", getOrgEmailService.getOrgEmail());
                JSONObject mergedInfo = mergeJSON(response.toJSON(), responseJSON);
                ctx.result(mergedInfo.toString());
            }
            else{
                log.info("Error: {}", response.getErrorName());
                ctx.result(response.toResponseString());
            }
        };

    public Handler getCustomer =
        ctx -> {
            Stripe.apiKey = apiKey;
            log.info("Attempting to find a customer");

            JSONObject req = new JSONObject(ctx.body());
            String customerEmail = req.getString("customerEmail");

            GetCustomerService getCustomerService = new GetCustomerService(customerEmail, apiKey);
            Message response = getCustomerService.executeAndGetResponse();

            if (response == BillingMessage.SUCCESS){
                JSONObject responseJSON = new JSONObject();
                responseJSON.put("customer", getCustomerService.getCustomer().toJson());
                JSONObject mergedInfo = mergeJSON(response.toJSON(), responseJSON);
                ctx.result(mergedInfo.toString());
            }
            else{
                log.info("Error: {}", response.getErrorName());
                ctx.result(response.toResponseString());
            }
        };

  public Handler createSubscription =
      ctx -> {
        log.info("createSubscription endpoint hit");
        Stripe.apiKey = apiKey;
        JSONObject req = new JSONObject(ctx.body());
        String customerId = req.getString("customerId");
        String paymentMethodId = req.getString("paymentMethodId");
        String priceId = req.getString("priceId");

        CreateSubscriptionService subscriptionService = new CreateSubscriptionService(customerId, paymentMethodId, priceId, apiKey);
        Message response = subscriptionService.executeAndGetResponse();

        if (response == BillingMessage.SUCCESS){
            JSONObject responseJSON = new JSONObject();
            responseJSON.put("subscription", subscriptionService.getSubscriptionWithSecret());
            JSONObject mergedInfo = mergeJSON(response.toJSON(), responseJSON);
            ctx.result(mergedInfo.toString());
        }
        else{
            log.info("Error: {}", response.getErrorName());
            ctx.result(response.toResponseString());
        }
      };

  public Handler cancelSubscription =
      ctx -> {
        JSONObject req = new JSONObject(ctx.body());
        String subscriptionId = req.getString("subscriptionId");
        Stripe.apiKey = apiKey;
        log.info("Retrieving subscription");
        Subscription subscription = Subscription.retrieve(subscriptionId);
        log.info("Deleting subscription");
        Subscription deletedSubscription = subscription.cancel();
        ctx.result(deletedSubscription.toJson());
      };

    public Handler getSubscription =
        ctx -> {
            Stripe.apiKey = apiKey;
            log.info("Attempting to find a subscription");

            JSONObject req = new JSONObject(ctx.body());
            String subscriptionId = req.getString("subscriptionId");

            GetSubscriptionService getSubscriptionService = new GetSubscriptionService(subscriptionId, apiKey);
            Message response = getSubscriptionService.executeAndGetResponse();

            if (response == BillingMessage.SUCCESS){
                JSONObject responseJSON = new JSONObject();
                responseJSON.put("subscription", getSubscriptionService.getSubscription().toJson());
                JSONObject mergedInfo = mergeJSON(response.toJSON(), responseJSON);
                ctx.result(mergedInfo.toString());
            }
            else{
                log.info("Error: {}", response.getErrorName());
                ctx.result(response.toResponseString());
            }
        };
    public Handler getPrice =
        ctx -> {
            Stripe.apiKey = apiKey;
            log.info("Attempting to find a price object");

            JSONObject req = new JSONObject(ctx.body());
            String priceId = req.getString("priceId");

            GetPriceService getPriceService = new GetPriceService(priceId);
            Message response = getPriceService.executeAndGetResponse();

            if (response == BillingMessage.SUCCESS){
                JSONObject responseJSON = new JSONObject();
                responseJSON.put("price", getPriceService.getPrice().toJson());
                JSONObject mergedInfo = mergeJSON(response.toJSON(), responseJSON);
                ctx.result(mergedInfo.toString());
            }
            else{
                log.info("Error: {}", response.getErrorName());
                ctx.result(response.toResponseString());
            }
        };
    public Handler getProduct =
        ctx -> {
            Stripe.apiKey = apiKey;
            log.info("Attempting to find a product object");

            JSONObject req = new JSONObject(ctx.body());
            String productId = req.getString("productId");

            GetProductService getProductService = new GetProductService(productId);
            Message response = getProductService.executeAndGetResponse();

            System.out.println("The response is " + response.toResponseString());
            if (response == BillingMessage.SUCCESS){
                JSONObject responseJSON = new JSONObject();
                responseJSON.put("product", getProductService.getProduct().toJson());
                JSONObject mergedInfo = mergeJSON(response.toJSON(), responseJSON);
                ctx.result(mergedInfo.toString());
            }
            else{
                log.info("Error: {}", response.getErrorName());
                ctx.result(response.toResponseString());
            }
        };
    public static JSONObject mergeJSON(JSONObject object1, JSONObject object2) {
        // helper function to merge 2 json objects
        JSONObject merged = new JSONObject(object1, JSONObject.getNames(object1));
        for (String key : JSONObject.getNames(object2)) {
            merged.put(key, object2.get(key));
        }
        return merged;
    }
}