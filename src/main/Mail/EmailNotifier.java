package Mail;

import Activity.Activity;
import Mail.Services.SendgridService;

import java.util.List;

public class EmailNotifier {

    public static void handle(Activity activity) {
        List<String> types = activity.getType();
        if (types == null || types.isEmpty()) return;

        String type = types.get(types.size() - 1); // get the most specific activity type
        // System.out.println("Handling activity type: " + type + " for user: " + activity.getUsername());
        switch (type) {
            case "CreateClientActivity":
                SendgridService.handleCreateClientActivity(activity.getUsername(), "PA"); // placeholder
                break;
            case "UploadFileActivity":
                SendgridService.handleUploadFileActivity(activity.getUsername(), "DocumentType"); // placeholder
                break;
            case "StartApplicationActivity":
                SendgridService.handleMailApplicationActivity(activity.getUsername());
                break;
            case "MailApplicationActivity":
                SendgridService.handleSubmitApplicationActivity(activity.getUsername(), "NonprofitName"); // placeholder
                break;
//            case "SubmitApplicationActivity":
//                SendgridService.sendSubmissionConfirmation(user, "ApplicationName");
//                break;
//            case "RecoverPasswordActivity":
//                SendgridService.sendPasswordRecoveryAlert(user);
//                break;
//            case "ChangePasswordActivity":
//                SendgridService.sendPasswordChangeConfirmation(user);
//                break;
//            case "Change2FAActivity":
//                SendgridService.send2FAUpdateNotice(user, activity.getObjectName()); // "on"/"off"
//                break;
            default:
                // Optional: log unknown type
        }
    }
}
