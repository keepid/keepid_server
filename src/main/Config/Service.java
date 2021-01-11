package Config;

import com.stripe.exception.StripeException;

public interface Service {
  Message executeAndGetResponse() throws StripeException;
}
