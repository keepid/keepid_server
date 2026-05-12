package Notification.Services;

import Activity.UserActivity.NotifyIdPickupActivity;
import Config.Message;
import Config.Service;
import Database.Activity.ActivityDao;
import Database.Notification.NotificationDao;
import Notification.Notification;
import Notification.WindmillNotificationClient;
import User.UserMessage;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NotifyIdPickupService implements Service {
    private final ActivityDao activityDao;
    private final NotificationDao notificationDao;
    private final WindmillNotificationClient notificationClient;
    private final String workerUsername;
    private final String clientUsername;
    private final String idToPickup;
    private final String clientPhoneNumber;
    private final String message;
    // Email channel inputs are optional. When all three (email + subject + body)
    // are valid the service will additionally send a templated email via the
    // notification client. Any blank or invalid email skips the email send
    // silently — same posture as SMS validation but on the optional channel.
    private final String clientEmail;
    private final String emailSubject;
    private final String emailBody;
    private final String emailHtml;

    public NotifyIdPickupService(
            ActivityDao activityDao,
            NotificationDao notificationDao,
            WindmillNotificationClient notificationClient,
            String workerUsername,
            String clientUsername,
            String idToPickup,
            String clientPhoneNumber,
            String message) {
        this(activityDao, notificationDao, notificationClient,
                workerUsername, clientUsername, idToPickup, clientPhoneNumber, message,
                null, null, null, null);
    }

    public NotifyIdPickupService(
            ActivityDao activityDao,
            NotificationDao notificationDao,
            WindmillNotificationClient notificationClient,
            String workerUsername,
            String clientUsername,
            String idToPickup,
            String clientPhoneNumber,
            String message,
            String clientEmail,
            String emailSubject,
            String emailBody,
            String emailHtml) {
        this.activityDao = activityDao;
        this.notificationDao = notificationDao;
        this.notificationClient = notificationClient;
        this.workerUsername = workerUsername;
        this.clientUsername = clientUsername;
        this.idToPickup = idToPickup;
        this.clientPhoneNumber = clientPhoneNumber;
        this.message = message;
        this.clientEmail = clientEmail;
        this.emailSubject = emailSubject;
        this.emailBody = emailBody;
        this.emailHtml = emailHtml;
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

        // Each channel is independently optional — but at least one must be active.
        boolean smsEligible = isSmsEligible();
        boolean emailEligible = isEmailEligible();

        if (!smsEligible && !emailEligible) {
            return UserMessage.INVALID_PARAMETER.withMessage(
                    "At least one notification channel (SMS or email) must be provided");
        }

        // Validate SMS fields only when the SMS channel is being used.
        if (smsEligible) {
            if (!notificationClient.isValidPhoneNumber(clientPhoneNumber)) {
                return UserMessage.INVALID_PARAMETER.withMessage(
                        "Invalid phone number format. Expected +1XXXXXXXXXX");
            }
            if (message == null || message.isBlank()) {
                return UserMessage.INVALID_PARAMETER.withMessage("Message is required for SMS");
            }
            notificationClient.sendSms(clientPhoneNumber, message);
        }

        if (emailEligible) {
            notificationClient.sendEmail(
                    clientEmail.trim(),
                    emailSubject,
                    emailBody,
                    emailHtml == null || emailHtml.isBlank()
                            ? Optional.empty()
                            : Optional.of(emailHtml));
        }

        recordNotifyIdPickupActivity();
        persistNotification(emailEligible);

        log.info(
                "ID pickup notification sent from {} to {} for ID: {} (sms={}, email={})",
                workerUsername,
                clientUsername,
                idToPickup,
                smsEligible,
                emailEligible);
        return UserMessage.SUCCESS;
    }

    private boolean isSmsEligible() {
        return clientPhoneNumber != null && !clientPhoneNumber.isBlank();
    }

    private boolean isEmailEligible() {
        if (clientEmail == null || clientEmail.isBlank()) return false;
        if (emailSubject == null || emailSubject.isBlank()) return false;
        if (emailBody == null || emailBody.isBlank()) return false;
        return WindmillNotificationClient.isValidEmail(clientEmail.trim());
    }

    private void recordNotifyIdPickupActivity() {
        NotifyIdPickupActivity activity =
                new NotifyIdPickupActivity(workerUsername, clientUsername, idToPickup);
        activityDao.save(activity);
    }

    private void persistNotification(boolean emailWasSent) {
        Notification notification = emailWasSent
                ? new Notification(
                        workerUsername,
                        clientUsername,
                        clientPhoneNumber != null ? clientPhoneNumber : "",
                        message != null ? message : "",
                        clientEmail.trim(),
                        emailSubject,
                        emailBody)
                : new Notification(workerUsername, clientUsername, clientPhoneNumber, message);
        notificationDao.save(notification);
    }
}
