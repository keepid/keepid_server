package Billing.Services;

import Config.Message;
import Config.Service;
import Billing.BillingMessage;
import com.stripe.Stripe;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.PaymentMethodAttachParams;
import com.stripe.param.SubscriptionCreateParams;
import org.json.JSONObject;

import java.util.*;

public class CreateSubscriptionService implements Service {
    private String customerId;
    private String paymentMethodId;
    private String priceId;
    Subscription subscription = null;
    String subscriptionWithSecret = null;
    private String apiKey;

    public CreateSubscriptionService(String customerId, String paymentMethodId, String priceId, String apiKey) {
        this.customerId = customerId;
        this.paymentMethodId = paymentMethodId;
        this.priceId = priceId;
        this.apiKey = apiKey;
    }

    @Override
    public Message executeAndGetResponse() throws StripeException {
        // checking input types
        if (customerId == null || (customerId.strip()).equals("")){
            return BillingMessage.INVALID_CUSTOMER_ID;
        }
        if (paymentMethodId == null || (paymentMethodId.strip()).equals("")){
            return BillingMessage.INVALID_PAYMENT_METHOD_ID;
        }
        if (priceId == null || (priceId.strip()).equals("")){
            return BillingMessage.INVALID_PRICE_ID;
        }
        Customer customer = null;
        try{
            customer = Customer.retrieve(customerId);
        }
        catch (Exception e){
            return BillingMessage.CUSTOMER_RETRIEVAL_FAILED;
        }

        try {
            PaymentMethod pm = PaymentMethod.retrieve(paymentMethodId);
            pm.attach(PaymentMethodAttachParams.builder().setCustomer(customer.getId()).build());
        } catch (InvalidRequestException e) {
            Map<String, String> responseError = new HashMap<>();
            responseError.put("error", e.getLocalizedMessage());
            JSONObject responseErrorJSON = new JSONObject(responseError);
            System.out.println("Payment method error: " + String.valueOf(responseErrorJSON));
            return BillingMessage.PAYMENT_METHOD_RETRIEVAL_FAILED;
        }

        CustomerUpdateParams customerUpdateParams =
                CustomerUpdateParams.builder()
                        .setInvoiceSettings(
                                CustomerUpdateParams.InvoiceSettings.builder()
                                        .setDefaultPaymentMethod(paymentMethodId)
                                        .build())
                        .build();
        customer.update(customerUpdateParams);

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
        subscription = Subscription.create(subCreateParams);

        if (subscription == null){
            return BillingMessage.SUBSCRIPTION_NULL;
        }

        // creating object to be returned
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("id", subscription.getId());
        responseData.put(
                "clientSecret",
                subscription.getLatestInvoiceObject().getPaymentIntentObject().getClientSecret());
        subscriptionWithSecret = (StripeObject.PRETTY_PRINT_GSON.toJson(responseData));

        return BillingMessage.SUCCESS;
    }

    public Subscription getSubscription(){
        return subscription;
    }

    public String getSubscriptionWithSecret(){
        return subscriptionWithSecret;
    }
}