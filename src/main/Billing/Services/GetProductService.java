package Billing.Services;

import Billing.BillingMessage;
import Config.Message;
import Config.Service;
import com.stripe.exception.StripeException;
import com.stripe.model.Product;

public class GetProductService implements Service {
    private String productId;
    Product product = null;

    public GetProductService(String productId) {
        this.productId = productId;
    }

    @Override
    public Message executeAndGetResponse() throws StripeException {
        // checking input types
        if (productId == null || (productId.strip()).equals("")){
            return BillingMessage.INVALID_PRODUCT_ID;
        }

        try{
            product = Product.retrieve(productId);
        }
        catch (Exception e){
            return BillingMessage.PRODUCT_NULL;
        }

        return BillingMessage.SUCCESS;
    }

    public Product getProduct(){
        return product;
    }
}