package Mail;

import Activity.Activity;
import Mail.Services.SendgridService;

import java.util.List;

public class EmailNotifier {

    public static void handle(Activity activity) {
        List<String> types = activity.getType();
        if (types == null || types.isEmpty()) return;

        String type = types.get(types.size() - 1); // get the most specific activity type
        System.out.println("Handling activity type: " + type + " for user: " + activity.getUsername());
        switch (type) {
            case "CreateClientActivity":
                SendgridService.sendWelcomeWithQuickStart(activity.getUsername(), "PA"); // placeholder
                break;
            case "UploadFileActivity":
                SendgridService.sendUploadReminder(activity.getUsername(), "DocumentType"); // placeholder
                break;
            case "StartApplicationActivity":
                SendgridService.sendApplicationReminder(activity.getUsername());
                break;
            case "MailApplicationActivity":
                SendgridService.sendPickupInfo(activity.getUsername(), "NonprofitName"); // placeholder
                break;
            default:
                // Optional: log unknown type
        }
    }
}
