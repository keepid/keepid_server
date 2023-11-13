package Form.Services;

import Config.Message;
import Config.Service;
import Database.Form.FormDao;
import Form.Form;
import Form.FormMessage;
import User.UserType;
import java.time.LocalDateTime;
import java.util.Optional;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;
import Issue.IssueReportMessage;

import org.bson.types.ObjectId;
import static Issue.IssueController.issueReportActualURL;

public class MailFormService implements Service {

    private ObjectId formId;
    private String username;
    private UserType privilegeLevel;
    private FormDao formDao;

    public MailFormService(FormDao formDao, ObjectId formId, String username, UserType privilegeLevel) {
        this.formDao = formDao;
        this.formId = formId;
        this.username = username;
        this.privilegeLevel = privilegeLevel;
    }

    @Override
    public Message executeAndGetResponse() {
        if (formId == null) {
            return FormMessage.INVALID_PARAMETER;
        } else {
            // Check for sufficient privileges
            if (privilegeLevel == UserType.Admin || privilegeLevel == UserType.Director || privilegeLevel == UserType.Developer) {
                try {
                    Optional<Form> maybeForm = formDao.get(formId);
                    if (maybeForm.isPresent()) {
                        Form form = maybeForm.get();
                        if (shouldMail(form)) {
                            // Add logic to mail the form
                            Message slackResponse = sendSlackNotification("Form Mailing Request", "A form is requested to be mailed.");

                            // Handle the Slack response
                            if (slackResponse != IssueReportMessage.SUCCESS) {
                                return slackResponse;
                            }

                            form.setLastMailedAt(LocalDateTime.now());
                            formDao.update(form); // Assuming FormDao has an update method
                            return FormMessage.SUCCESS;
                        } else {
                            return FormMessage.ALREADY_MAILED_RECENTLY;
                        }
                    } else {
                        return FormMessage.INVALID_FORM;
                    }
                } catch (Exception e) {
                    return FormMessage.SERVER_ERROR;
                }
            } else {
                return FormMessage.INSUFFICIENT_PRIVILEGE;
            }
        }
    }

    //want to remove this for the future
    private Message sendSlackNotification(String title, String description) {
        JSONArray blocks = new JSONArray();
        JSONObject titleJson = new JSONObject();
        titleJson.put("type", "section");
        titleJson.put("text", new JSONObject().put("type", "mrkdwn").put("text", "*Issue Title: * " + title));
        blocks.put(titleJson);

        JSONObject desJson = new JSONObject();
        desJson.put("type", "section");
        desJson.put("text", new JSONObject().put("type", "mrkdwn").put("text", "*Issue Description: * " + description));
        blocks.put(desJson);

        JSONObject input = new JSONObject();
        input.put("blocks", blocks);

        HttpResponse<?> posted = Unirest.post(issueReportActualURL)
                .header("accept", "application/json")
                .body(input.toString())
                .asEmpty();

        if (!posted.isSuccess()) {
            return IssueReportMessage.SLACK_FAILED;
        }
        return IssueReportMessage.SUCCESS;
    }

    private boolean shouldMail(Form form) {
        LocalDateTime lastMailedAt = form.getLastMailedAt();
        // Logic to decide if the form should be mailed again
        // Example: mail if lastMailedAt is null or more than 1 day ago
        return lastMailedAt == null || LocalDateTime.now().isAfter(lastMailedAt.plusDays(1));
    }
}
