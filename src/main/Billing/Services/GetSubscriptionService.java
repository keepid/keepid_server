package Billing.Services;

import Billing.BillingMessage;
import Config.DeploymentLevel;
import Config.Message;
import Config.MongoConfig;
import Config.Service;
import Organization.Organization;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;

import static com.mongodb.client.model.Filters.eq;

public class GetSubscriptionService implements Service {
    private String subscriptionId;
    Subscription subscription = null;
    private String apiKey;

    public GetSubscriptionService(String subscriptionId, String apiKey) {
        this.subscriptionId = subscriptionId;
        this.apiKey = apiKey;
    }

    @Override
    public Message executeAndGetResponse() throws StripeException {
        // checking input types
        if (subscriptionId == null || (subscriptionId.strip()).equals("")){
            return BillingMessage.INVALID_SUBSCRIPTION_ID;
        }

        subscription = Subscription.retrieve(subscriptionId);

        if (subscription == null){
            return BillingMessage.SUBSCRIPTION_NULL;
        }

        return BillingMessage.SUCCESS;
    }

    public Subscription getSubscription(){
        return subscription;
    }
}