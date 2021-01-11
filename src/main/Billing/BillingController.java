package Billing;

import Config.Message;
import Logger.LogFactory;
import User.UserMessage;
import io.javalin.http.Handler;
import org.slf4j.Logger;

public class BillingController {
  Logger logger;

  public BillingController() {
    LogFactory l = new LogFactory();
    logger = l.createLogger("BillingController");
  }

  public Handler startSession =
      ctx -> {
        StartSubscriptionService sss = new StartSubscriptionService();
        ctx.result(sss.executeAndGetResponse().toResponseString());
      };

  public Handler openPortal =
      ctx -> {
        OpenPortalService ops = new OpenPortalService(ctx.sessionAttribute("username"));
        Message ms = ops.executeAndGetResponse();
        if (ms.equals(UserMessage.SUCCESS)) {
          ctx.redirect(ops.redirectURL);
        } else {
          // TODO handle this
        }
      };
  public Handler handleWebhook =
      ctx -> {
        HandleWebhooksService hws = new HandleWebhooksService(ctx.body());
        Message ms = hws.executeAndGetResponse();
        if (ms.equals(UserMessage.HASH_FAILURE)) {
          ctx.status(400);
        } else {
          ctx.status(200);
        }
      };
}
