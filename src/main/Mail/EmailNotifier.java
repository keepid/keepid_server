package Mail;

import Activity.Activity;
import Mail.Services.SendgridService;

public class EmailNotifier {

    public static void handle(Activity activity) {
        switch (activity.getType()) {
            case "CreateClientActivity":
                SendgridService.sendWelcomeWithQuickStart(activity.getUsername(), activity.getNonprofitState());
                break;
            case "UploadFileActivity":
                SendgridService.sendUploadReminder(activity.getUsername(), activity.getUploadedDocType());
                break;
            case "StartApplicationActivity":
                SendgridService.sendApplicationReminder(activity.getUsername());
                break;
            case "MailApplicationActivity":
                SendgridService.sendPickupInfo(activity.getUsername(), activity.getNonprofit());
                break;
            default:
                // log or skip
        }
    }
}
