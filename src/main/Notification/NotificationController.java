package Notification;

import Config.Message;
import Database.Activity.ActivityDao;
import Notification.Services.NotifyIdPickupService;
import User.UserMessage;
import io.javalin.http.Handler;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

@Slf4j
public class NotificationController {
    private ActivityDao activityDao;
    private WindmillNotificationClient notificationClient;

    public NotificationController(
            ActivityDao activityDao, WindmillNotificationClient notificationClient) {
        this.activityDao = activityDao;
        this.notificationClient = notificationClient;
    }

    public Handler notifyIdPickup =
            ctx -> {
                JSONObject req = new JSONObject(ctx.body());

                String sessionUsername = ctx.sessionAttribute("username");
                if (sessionUsername == null) {
                    ctx.result(UserMessage.SESSION_TOKEN_FAILURE.toResponseString());
                    return;
                }

                String workerUsername = req.getString("workerUsername");

                if (!sessionUsername.equals(workerUsername)) {
                    ctx.result(
                            UserMessage.INVALID_PARAMETER
                                    .withMessage("Worker username does not match authenticated session")
                                    .toResponseString());
                    return;
                }

                String clientUsername = req.getString("clientUsername");
                String idToPickup = req.getString("idToPickup");
                String clientPhoneNumber = req.getString("clientPhoneNumber");
                String message = req.getString("message");

                NotifyIdPickupService service =
                        new NotifyIdPickupService(
                                activityDao,
                                notificationClient,
                                workerUsername,
                                clientUsername,
                                idToPickup,
                                clientPhoneNumber,
                                message);
                Message responseMessage = service.executeAndGetResponse();
                ctx.result(responseMessage.toResponseString());
            };
}
