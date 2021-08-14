package Billing;

import Config.Message;
import org.json.JSONObject;

public enum BillingMessage implements Message {
    INVALID_CUSTOMER_ID("INVALID_CUSTOMER_ID:Invalid Stripe customerId."),
    INVALID_PAYMENT_METHOD_ID("INVALID_PAYMENT_METHOD_ID:Invalid Stripe paymentMethodId."),
    INVALID_PRICE_ID("INVALID_PRICE_ID:Invalid Stripe priceId."),
    CUSTOMER_RETRIEVAL_FAILED("CUSTOMER_RETRIEVAL_FAILED:Stripe Customer object could not be found."),
    PAYMENT_METHOD_RETRIEVAL_FAILED("PAYMENT_METHOD_RETRIEVAL_FAILED:Stripe Payment Method object could not be found."),
    SUBSCRIPTION_NULL("SUBSCRIPTION_NULL:Subscription can't be null."),
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
