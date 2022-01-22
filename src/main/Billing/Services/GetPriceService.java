package Billing.Services;

import Billing.BillingMessage;
import Config.Message;
import Config.Service;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;

public class GetPriceService implements Service {
    private String priceId;
    Price price = null;

    public GetPriceService(String priceId) {
        this.priceId = priceId;
    }

    @Override
    public Message executeAndGetResponse() throws StripeException {
        // checking input types
        if (priceId == null || (priceId.strip()).equals("")){
            return BillingMessage.INVALID_PRICE_ID;
        }

        price = Price.retrieve(priceId);

        if (price == null){
            return BillingMessage.PRICE_NULL;
        }

        return BillingMessage.SUCCESS;
    }

    public Price getPrice(){
        System.out.println("Price is " + price);
        return price;
    }
}