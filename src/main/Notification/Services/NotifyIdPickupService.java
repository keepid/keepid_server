package Notification.Services;

import Activity.UserActivity.NotifyIdPickupActivity;
import Config.Message;
import Config.Service;
import Database.Activity.ActivityDao;
import Notification.WindmillNotificationClient;
import User.UserMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NotifyIdPickupService implements Service {
    private final ActivityDao activityDao;
    private final WindmillNotificationClient notificationClient;
    private final String workerUsername;
    private final String clientUsername;
    private final String idToPickup;
    private final String clientPhoneNumber;
    private final String message;

    public NotifyIdPickupService(
            ActivityDao activityDao,
            WindmillNotificationClient notificationClient,
            String workerUsername,
            String clientUsername,
            String idToPickup,
            String clientPhoneNumber,
            String message) {
        this.activityDao = activityDao;
        this.notificationClient = notificationClient;
        this.workerUsername = workerUsername;
        this.clientUsername = clientUsername;
        this.idToPickup = idToPickup;
        this.clientPhoneNumber = clientPhoneNumber;
        this.message = message;
    }

    @Override
    public Message executeAndGetResponse() {
        if (workerUsername == null || workerUsername.isBlank()) {
            return UserMessage.INVALID_PARAMETER.withMessage("Worker username is required");
        }
        if (clientUsername == null || clientUsername.isBlank()) {
            return UserMessage.INVALID_PARAMETER.withMessage("Client username is required");
        }
        if (idToPickup == null || idToPickup.isBlank()) {
            return UserMessage.INVALID_PARAMETER.withMessage("ID to pickup is required");
        }
        if (!notificationClient.isValidPhoneNumber(clientPhoneNumber)) {
            return UserMessage.INVALID_PARAMETER.withMessage(
                    "Invalid phone number format. Expected +1XXXXXXXXXX");
        }
        if (message == null || message.isBlank()) {
            return UserMessage.INVALID_PARAMETER.withMessage("Message is required");
        }

        notificationClient.sendSms(clientPhoneNumber, message);
        recordNotifyIdPickupActivity();

        log.info(
                "ID pickup notification sent from {} to {} for ID: {}",
                workerUsername,
                clientUsername,
                idToPickup);
        return UserMessage.SUCCESS;
    }

    private void recordNotifyIdPickupActivity() {
        NotifyIdPickupActivity activity =
                new NotifyIdPickupActivity(workerUsername, clientUsername, idToPickup);
        activityDao.save(activity);
    }
}
