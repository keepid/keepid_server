package Organization.Services;

import Config.Message;
import Config.Service;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.Map;
import User.UserMessage;

@Slf4j
public class CreateCustomerService implements Service {
    String customerName;
    String customerEmail;
    Customer customer = null;
    private String apiKey = System.getenv("STRIPE_TEST_KEY");

    public CreateCustomerService(String customerName, String customerEmail) {
        this.customerName = customerName;
        this.customerEmail = customerEmail;
    }

    @Override
    public Message executeAndGetResponse() throws StripeException {
        Stripe.apiKey = apiKey;

        log.info("Attempting to create a customer");
        Map<String, Object> params = new HashMap<>();
        params.put("name", customerName);
        params.put("email", customerEmail);

        customer = Customer.create(params);
        log.info("Created customer");
        return UserMessage.SUCCESS;
    }

    public Customer getCustomer() {
        return customer;
    }
}
