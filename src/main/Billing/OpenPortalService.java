package Billing;

import Config.Message;
import Config.Service;
import User.UserMessage;
import com.stripe.exception.StripeException;
import com.stripe.model.billingportal.Session;
import com.stripe.param.billingportal.SessionCreateParams;

public class OpenPortalService implements Service {
  String username;
  String redirectURL;

  public OpenPortalService(String username) {
    this.username = username;
  }

  @Override
  public Message executeAndGetResponse() throws StripeException {
    SessionCreateParams params =
        SessionCreateParams.builder()
            .setCustomer(username)
            .setReturnUrl("https://keep.id/home")
            .build();

    Session session = Session.create(params);
    this.redirectURL = session.getUrl();
    return UserMessage.SUCCESS;
  }

  public String getRedirectURL() {
    return redirectURL;
  }
}
