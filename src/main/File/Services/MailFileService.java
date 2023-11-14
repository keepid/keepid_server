package File.Services;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import File.File;
import File.FileMessage;
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
import static io.javalin.Javalin.log;

public class MailFileService implements Service {

    private ObjectId fileId;
    private String username;
    private UserType privilegeLevel;
    private FileDao fileDao;
    private String description;

    public MailFileService(FileDao fileDao, ObjectId fileId, String username, UserType privilegeLevel, String description) {
        this.fileDao = fileDao;
        this.fileId = fileId;
        this.username = username;
        this.privilegeLevel = privilegeLevel;
        this.description = description;
    }

    @Override
    public Message executeAndGetResponse() {
        //fileDao was not used for the ViewDocuments, so seeing completed applications is difficult. Add these checks when they work
//        try {
//            Optional<File> maybefile = fileDao.get(fileId);
//            if (maybefile.isPresent()) {
//                File file = maybefile.get();
//                if (shouldMail(file)) {
                    Message slackResponse = sendSlackNotification("file Mailing Request", this.description);
                    if (slackResponse != IssueReportMessage.SUCCESS) {
                        return slackResponse;
                    }
//                    file.setLastMailedAt(LocalDateTime.now());
//                    fileDao.update(file); // Assuming fileDao has an update method
                    return FileMessage.SUCCESS;
//                } else {
//                    return FileMessage.ALREADY_MAILED_RECENTLY;
//                }
//            } else {
//                return FileMessage.INVALID_FILE;
//            }
//        } catch (Exception e) {
//            return FileMessage.SERVER_ERROR;
//        }
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

    private boolean shouldMail(File file) {
        LocalDateTime lastMailedAt = file.getLastMailedAt();
        // Logic to decide if the file should be mailed again
        // Example: mail if lastMailedAt is null or more than 1 day ago
        return lastMailedAt == null || LocalDateTime.now().isAfter(lastMailedAt.plusDays(1));
    }
}
