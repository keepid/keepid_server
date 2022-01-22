package Billing;

import Config.Message;
import org.json.JSONObject;

public enum BillingMessage implements Message {
    INVALID_CUSTOMER_ID("INVALID_CUSTOMER_ID:Invalid Stripe customerId."),
    INVALID_SUBSCRIPTION_ID("INVALID_SUBSCRIPTION_ID:Invalid Stripe subscriptionId."),
    INVALID_PAYMENT_METHOD_ID("INVALID_PAYMENT_METHOD_ID:Invalid Stripe paymentMethodId."),
    INVALID_PRICE_ID("INVALID_PRICE_ID:Invalid Stripe priceId."),
    INVALID_PRODUCT_ID("INVALID_PRODUCT_ID:Invalid Stripe productId."),
    INVALID_ORG_NAME("INVALID_ORG_NAME:Invalid orgName, please check the session tokens."),
    INVALID_CUSTOMER_EMAIL("INVALID_CUSTOMER_EMAIL: Invalid email fetched from DB."),
    INVALID_DB_ORGANIZATION_NAME("INVALID_DB_ORGANIZATION_NAME:Document with current organization name cannot be found"),
    CUSTOMER_RETRIEVAL_FAILED("CUSTOMER_RETRIEVAL_FAILED:Stripe Customer object could not be found."),
    PAYMENT_METHOD_RETRIEVAL_FAILED("PAYMENT_METHOD_RETRIEVAL_FAILED:Stripe Payment Method object could not be found."),
    DB_NULL("DB_NULL:Database can't be found."),
    ORG_COLLECTION_NULL("ORG_COLLECTION_NULL:Org Collection can't be null."),
    SUBSCRIPTION_NULL("SUBSCRIPTION_NULL:Stripe Subscription can't be null."),
    CUSTOMER_NULL("CUSTOMER_NULL:Stripe Customer can't be null."),
    PRICE_NULL("PRICE_NULL:Stripe Price object can't be null."),
    PRODUCT_NULL("PRODUCT_NULL:Stripe Product object can't be null."),
    SUCCESS("SUCCESS:Success.");

    private String errorMessage;

    BillingMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String toResponseString() {
        return toJSON().toString();
    }

    public String getErrorName() {
        return this.errorMessage.split(":")[0];
    }

    public String getErrorDescription() {
        return this.errorMessage.split(":")[1];
    }

    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("status", getErrorName());
        res.put("message", getErrorDescription());
        return res;
    }

    public JSONObject toJSON(String message) {
        JSONObject res = new JSONObject();
        res.put("status", getErrorName());
        res.put("message", message);
        return res;
    }
}
