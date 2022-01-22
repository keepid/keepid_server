package Billing.Services;

import Billing.BillingMessage;
import Config.DeploymentLevel;
import Config.Message;
import Config.MongoConfig;
import Config.Service;
import Organization.Organization;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.PaymentMethodAttachParams;
import com.stripe.param.SubscriptionCreateParams;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.*;

import static com.mongodb.client.model.Filters.eq;

public class GetCustomerService implements Service {
    private final String customerEmail;
    private MongoDatabase db;
    private MongoCollection<Organization> orgCollection;
    Customer customer = null;

    public GetCustomerService(String customerEmail, String apiKey) {
        this.customerEmail = customerEmail;
    }

    @Override
    public Message executeAndGetResponse() throws StripeException {
        // checking input types
        if (customerEmail == null || (customerEmail.strip()).equals("")){
            return BillingMessage.INVALID_CUSTOMER_EMAIL;
        }

        try{
            db = MongoConfig.getDatabase(DeploymentLevel.STAGING);
        }
        catch (Exception e){
            return BillingMessage.DB_NULL;
        }

        // query mongoDB under organization for the field customerId that matches with the customerEmail
        try{
            orgCollection = db.getCollection("organization", Organization.class);
        }
        catch (Exception e){
            return BillingMessage.ORG_COLLECTION_NULL;
        }

        Organization org = null;

        try{
            org = orgCollection.find(eq("email", customerEmail)).first();
        }
        catch (Exception e){
            return BillingMessage.INVALID_CUSTOMER_EMAIL;
        }

        // get customerId from org and retrieve corresponding customer obj from stripe
        String customerId = org.getCustomerId();

        try{
            customer = Customer.retrieve(customerId);
        }
        catch (Exception e){
            return BillingMessage.CUSTOMER_NULL;
        }

        return BillingMessage.SUCCESS;
    }

    public Customer getCustomer(){
        return customer;
    }
}