package Form;

import Config.Message;
import Database.ApplicationRegistry.ApplicationRegistryDao;
import Database.File.FileDao;
import Database.Form.FormDao;
import Database.User.UserDao;
import Form.Jobs.GetWeeklyApplicationsJob;
import Form.Services.CreateApplicationService;
import Form.Services.DeleteFormService;
import Form.Services.GetApplicationRegistryService;
import Form.Services.GetFormService;
import Form.Services.ManuallyUploadFormService;
import Form.Services.PromoteRegistryService;
import Form.Services.UploadFormService;
import PDF.Services.V2Services.ParsePDFFieldsService;
import Security.EncryptionController;
import Security.FileStorageCryptoPolicy;
import User.User;
import User.UserMessage;
import User.UserType;
import io.javalin.http.Handler;
import io.javalin.http.UploadedFile;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@Slf4j
public class FormController {
  private FormDao formDao;
  private FileDao fileDao;
  private EncryptionController encryptionController;
  private UserDao userDao;
  private ApplicationRegistryDao registryDao;

  public FormController(
      FormDao formDao,
      FileDao fileDao,
      UserDao userDao,
      EncryptionController encryptionController,
      ApplicationRegistryDao registryDao) {
    this.formDao = formDao;
    this.fileDao = fileDao;
    this.userDao = userDao;
    this.encryptionController = encryptionController;
    this.registryDao = registryDao;
  }

  //  public Handler formTest =
  //      ctx -> {
  //        ObjectId formId = new ObjectId("6679fc62948ca978b0d31825");
  //        String username = "SAMPLE-CLIENT";
  //        GetFormService formService =
  //            new GetFormService(formDao, formId, username, UserType.Client, false);
  //        formService.executeAndGetResponse();
  //        System.out.println(formService.getJsonInformation());
  //      };
  public Handler customFormGet =
      ctx -> {
        ManuallyUploadFormService manuallyUploadFormService =
            new ManuallyUploadFormService(formDao);
        Message response = manuallyUploadFormService.executeAndGetResponse();
        ctx.result(response.toResponseString());
      };
  public Handler formDelete =
      ctx -> {
        String username;
        String orgName;
        UserType userType;
        JSONObject req = new JSONObject(ctx.body());
        Optional<User> targetUser = userCheck(ctx.body());
        if (targetUser.isEmpty() && req.has("targetUser")) {
          ctx.result(UserMessage.USER_NOT_FOUND.toJSON().toString());
        } else {
          boolean orgFlag;
          if (targetUser.isPresent() && req.has("targetUser")) {
            log.info("Target form found");
            username = targetUser.get().getUsername();
            orgName = targetUser.get().getOrganization();
            userType = targetUser.get().getUserType();
            orgFlag = orgName.equals(ctx.sessionAttribute("orgName"));
          } else {
            username = ctx.sessionAttribute("username");
            userType = ctx.sessionAttribute("privilegeLevel");
            // User is in same org as themselves
            orgFlag = true;
          }

          if (orgFlag) {
            boolean isTemplate = Boolean.valueOf(req.getString("isTemplate"));
            String fileIDStr = req.getString("fileId");
            ObjectId fileId = new ObjectId(fileIDStr);

            DeleteFormService deleteFormService =
                new DeleteFormService(formDao, fileId, username, userType, isTemplate);
            ctx.result(deleteFormService.executeAndGetResponse().toResponseString());
          } else {
            ctx.result(UserMessage.CROSS_ORG_ACTION_DENIED.toResponseString());
          }
        }
      };

  public Handler formGet =
      ctx -> {
        String username;
        String orgName;
        UserType userType;
        JSONObject req = new JSONObject(ctx.body());
        Optional<User> targetUser = userCheck(ctx.body());
        if (targetUser.isEmpty() && req.has("targetUser")) {
          log.info("Target form not Found");
          ctx.result(UserMessage.USER_NOT_FOUND.toJSON().toString());
        } else {
          boolean orgFlag;
          if (targetUser.isPresent() && req.has("targetUser")) {
            log.info("Target form found");
            username = targetUser.get().getUsername();
            orgName = targetUser.get().getOrganization();
            userType = targetUser.get().getUserType();
            orgFlag = orgName.equals(ctx.sessionAttribute("orgName"));
          } else {
            username = ctx.sessionAttribute("username");
            userType = ctx.sessionAttribute("privilegeLevel");
            orgFlag = true;
          }

          if (orgFlag) {
            String fileIDStr = req.getString("fileId");
            String isTemplateString = req.getString("isTemplate");
            GetFormService getFormService =
                new GetFormService(
                    formDao,
                    new ObjectId(fileIDStr),
                    username,
                    userType,
                    Boolean.valueOf(isTemplateString));
            Message response = getFormService.executeAndGetResponse();
            if (response == FormMessage.SUCCESS) {
              JSONObject result = getFormService.getJsonInformation();
              ctx.header("Content-Type", "application/form");
              ctx.result(result.toString());
            } else {
              ctx.result(response.toResponseString());
            }
          } else {
            ctx.result(UserMessage.CROSS_ORG_ACTION_DENIED.toResponseString());
          }
        }
      };

  public Handler formUpload =
      ctx -> {
        log.info("formUpload");
        String username;
        String organizationName;
        UserType privilegeLevel;
        Message response;
        JSONObject req;
        JSONObject form;
        String body = ctx.body();
        try {
          req = new JSONObject(body);
          form = (JSONObject) req.get("form");
        } catch (Exception e) {
          req = null;
          form = null;
        }
        if (req != null) {
          Optional<User> check = userCheck(body);
          if (req != null && req.has("targetUser") && check.isEmpty()) {
            log.info("Target User could not be found in the database");
            response = UserMessage.USER_NOT_FOUND;
          } else {
            boolean orgFlag;
            if (req != null && req.has("targetUser") && check.isPresent()) {
              log.info("Target User found, setting parameters.");
              username = check.get().getUsername();
              organizationName = check.get().getOrganization();
              privilegeLevel = check.get().getUserType();
              orgFlag = organizationName.equals(ctx.sessionAttribute("orgName"));
            } else {
              username = ctx.sessionAttribute("username");
              privilegeLevel = ctx.sessionAttribute("privilegeLevel");
              orgFlag = true;
            }
            if (orgFlag) {
              if (form == null) {
                log.info("File is null, invalid pdf");
                response = FormMessage.INVALID_FORM;
              } else {
                UploadFormService uploadService =
                    new UploadFormService(formDao, username, privilegeLevel, form);
                response = uploadService.executeAndGetResponse();
              }
            } else {
              response = UserMessage.CROSS_ORG_ACTION_DENIED;
            }
          }
        } else {
          response = FormMessage.INVALID_FORM;
        }

        ctx.result(response.toResponseString());
      };

  public Handler getWeeklyApplications =
      ctx -> {
        GetWeeklyApplicationsJob.run(formDao);
      };
  
  public Handler getAppRegistry =
      ctx -> {
        log.info("Entered getAppRegistry function");
        String body = ctx.body();
        JSONObject req = new JSONObject(body);
        String type = req.getString("type");
        String state = req.getString("state");
        String situation = req.getString("situation");
        String person = req.getString("person");
        String org = ctx.sessionAttribute("orgName");
        if (org == null || org.isBlank()) {
          org = req.optString("org", "");
        }
        GetApplicationRegistryService getAppRegService =
            new GetApplicationRegistryService(registryDao, type, state, situation, person, org);
        Message res = getAppRegService.executeAndGetResponse();
        if (res == FormMessage.SUCCESS) {
          ctx.result(getAppRegService.getJsonInformation());
        } else {
          ctx.result(res.toResponseString());
        }
      };

  public Handler getAvailableApplicationOptions =
      ctx -> {
        String sessionOrg = ctx.sessionAttribute("orgName");
        JSONArray arr = new JSONArray();
        for (ApplicationRegistryEntry entry : registryDao.getAll()) {
          if (entry.getLookupKey() == null || entry.getLookupKey().isBlank()) {
            continue;
          }
          if (sessionOrg != null && !sessionOrg.isBlank() && entry.getFileIdForOrg(sessionOrg) == null) {
            continue;
          }
          String[] parts = entry.getLookupKey().split("\\$", 3);
          if (parts.length < 3) {
            continue;
          }
          arr.put(
              new JSONObject()
                  .put("type", parts[0])
                  .put("state", parts[1])
                  .put("situation", parts[2])
                  .put("lookupKey", entry.getLookupKey()));
        }
        ctx.header("Content-Type", "application/json");
        ctx.result(arr.toString());
      };

  public Handler listRegistry =
      ctx -> {
        List<ApplicationRegistryEntry> entries = registryDao.getAll();
        JSONArray arr = new JSONArray();
        for (ApplicationRegistryEntry entry : entries) {
          JSONObject obj = new JSONObject();
          obj.put("id", entry.getId().toHexString());
          obj.put("lookupKey", entry.getLookupKey());
          obj.put("idCategoryType", entry.getIdCategoryType());
          obj.put("state", entry.getState());
          obj.put("applicationSubtype", entry.getApplicationSubtype());
          obj.put("pidlSubtype", entry.getPidlSubtype());
          obj.put("amount", entry.getAmount());
          obj.put("numWeeks", entry.getNumWeeks());
          JSONArray mappings = new JSONArray();
          if (entry.getOrgMappings() != null) {
            for (ApplicationRegistryEntry.OrgMapping m : entry.getOrgMappings()) {
              mappings.put(
                  new JSONObject()
                      .put("orgName", m.getOrgName())
                      .put("fileId", m.getFileId().toHexString()));
            }
          }
          obj.put("orgMappings", mappings);
          arr.put(obj);
        }
        ctx.header("Content-Type", "application/json");
        ctx.result(arr.toString());
      };

  public Handler listOrgsForDev =
      ctx -> {
        HashSet<String> uniqueNames = new HashSet<>();
        JSONArray orgs = new JSONArray();
        for (User user : userDao.getAll()) {
          String orgName = user.getOrganization();
          if (orgName == null || orgName.isBlank() || uniqueNames.contains(orgName)) {
            continue;
          }
          uniqueNames.add(orgName);
          orgs.put(orgName);
        }
        ctx.header("Content-Type", "application/json");
        ctx.result(orgs.toString());
      };

  public Handler upsertOrgMapping =
      ctx -> {
        String id = ctx.pathParam("id");
        JSONObject req = new JSONObject(ctx.body());
        String orgName = req.optString("orgName", "").strip();
        String fileId = req.optString("fileId", "").strip();
        if (orgName.isEmpty() || fileId.isEmpty()) {
          ctx.status(400).result(FormMessage.INVALID_PARAMETER.toResponseString());
          return;
        }

        ObjectId registryId;
        ObjectId targetFileId;
        try {
          registryId = new ObjectId(id);
          targetFileId = new ObjectId(fileId);
        } catch (Exception e) {
          ctx.status(400).result(FormMessage.INVALID_PARAMETER.toResponseString());
          return;
        }

        if (registryDao.get(registryId).isEmpty() || fileDao.get(targetFileId).isEmpty()) {
          ctx.status(404).result(FormMessage.INVALID_PARAMETER.toResponseString());
          return;
        }

        // Replace any existing mapping for this org with the new file target.
        registryDao.removeOrgMapping(registryId, orgName);
        registryDao.addOrgMapping(registryId, new ApplicationRegistryEntry.OrgMapping(orgName, targetFileId));
        ctx.header("Content-Type", "application/json");
        ctx.result(
            new JSONObject()
                .put("status", "updated")
                .put("registryId", registryId.toHexString())
                .put("orgName", orgName)
                .put("fileId", targetFileId.toHexString())
                .toString());
      };

  public Handler deleteOrgMapping =
      ctx -> {
        String id = ctx.pathParam("id");
        String rawOrgName = ctx.pathParam("orgName");
        String orgName = URLDecoder.decode(rawOrgName, StandardCharsets.UTF_8);
        if (orgName.isBlank()) {
          ctx.status(400).result(FormMessage.INVALID_PARAMETER.toResponseString());
          return;
        }

        ObjectId registryId;
        try {
          registryId = new ObjectId(id);
        } catch (Exception e) {
          ctx.status(400).result(FormMessage.INVALID_PARAMETER.toResponseString());
          return;
        }

        if (registryDao.get(registryId).isEmpty()) {
          ctx.status(404).result(FormMessage.INVALID_PARAMETER.toResponseString());
          return;
        }
        registryDao.removeOrgMapping(registryId, orgName);
        ctx.header("Content-Type", "application/json");
        ctx.result(
            new JSONObject()
                .put("status", "removed")
                .put("registryId", registryId.toHexString())
                .put("orgName", orgName)
                .toString());
      };

  public Handler deleteRegistryEntry =
      ctx -> {
        String id = ctx.pathParam("id");
        registryDao.delete(new ObjectId(id));
        ctx.result(new JSONObject().put("status", "deleted").toString());
      };

  public Handler createApplication =
      ctx -> {
        UploadedFile uploadedFile = ctx.uploadedFile("file");
        if (uploadedFile == null) {
          ctx.status(400).result(FormMessage.INVALID_FORM.toResponseString());
          return;
        }
        String fieldMappingsStr = ctx.formParam("fieldMappings");
        String registryMetadataStr = ctx.formParam("registryMetadata");
        if (fieldMappingsStr == null || registryMetadataStr == null) {
          ctx.status(400).result(FormMessage.INVALID_PARAMETER.toResponseString());
          return;
        }
        String username = ctx.sessionAttribute("username");
        String orgName = ctx.formParam("orgName");
        if (orgName == null || orgName.isBlank()) {
          orgName = ctx.sessionAttribute("orgName");
        }
        if (orgName == null || orgName.isBlank()) {
          ctx.status(400).result(FormMessage.INVALID_PARAMETER.toResponseString());
          return;
        }
        CreateApplicationService service =
            new CreateApplicationService(
                fileDao,
                formDao,
                registryDao,
                encryptionController,
                username,
                orgName,
                uploadedFile.getFilename(),
                uploadedFile.getContent(),
                new JSONArray(fieldMappingsStr),
                new JSONObject(registryMetadataStr));
        Message res = service.executeAndGetResponse();
        if (res == FormMessage.SUCCESS) {
          ctx.header("Content-Type", "application/json");
          ctx.result(service.getResult().toString());
        } else {
          ctx.status(500).result(service.getResult().toString());
        }
      };

  public Handler promoteRegistry =
      ctx -> {
        JSONObject req = new JSONObject(ctx.body());
        String registryId = req.getString("registryId");
        PromoteRegistryService service =
            new PromoteRegistryService(registryDao, fileDao, formDao, new ObjectId(registryId));
        Message res = service.executeAndGetResponse();
        if (res == FormMessage.SUCCESS) {
          ctx.header("Content-Type", "application/json");
          ctx.result(service.getResult().toString());
        } else {
          ctx.status(500).result(service.getResult().toString());
        }
      };

  public Handler getRegistryDetail =
      ctx -> {
        String id = ctx.pathParam("id");
        Optional<ApplicationRegistryEntry> entryOpt = registryDao.get(new ObjectId(id));
        if (entryOpt.isEmpty()) {
          ctx.status(404).result("{\"error\":\"Registry entry not found\"}");
          return;
        }
        ApplicationRegistryEntry entry = entryOpt.get();

        JSONObject registryJson = new JSONObject();
        registryJson.put("id", entry.getId().toHexString());
        registryJson.put("lookupKey", entry.getLookupKey());
        registryJson.put("idCategoryType", entry.getIdCategoryType());
        registryJson.put("state", entry.getState());
        registryJson.put("applicationSubtype", entry.getApplicationSubtype());
        registryJson.put("pidlSubtype", entry.getPidlSubtype());
        registryJson.put("amount", entry.getAmount());
        registryJson.put("numWeeks", entry.getNumWeeks());

        String fileIdHex = null;
        JSONArray fieldsArr = new JSONArray();
        if (entry.getOrgMappings() != null && !entry.getOrgMappings().isEmpty()) {
          ObjectId fileId = entry.getOrgMappings().get(0).getFileId();
          fileIdHex = fileId.toHexString();
          Optional<Form> formOpt = formDao.getByFileId(fileId);
          if (formOpt.isPresent()) {
            Form form = formOpt.get();
            for (FormQuestion q : form.getBody().getQuestions()) {
              JSONObject fObj = new JSONObject();
              fObj.put("fieldName", q.getQuestionName());
              fObj.put("displayLabel", q.getQuestionText());
              fObj.put("directive", q.getDirective());
              fObj.put("fieldType", q.getType().toString());
              fObj.put("required", q.isRequired());
              fieldsArr.put(fObj);
            }
          }

          // Parse the stored PDF to get bounding-box data for the field overlays
          try {
            Optional<File.File> fileObj = fileDao.get(fileId);
            Optional<InputStream> pdfStream = fileDao.getStream(fileId);
            if (fileObj.isPresent() && pdfStream.isPresent()) {
              File.File file = fileObj.get();
              byte[] fileBytes = pdfStream.get().readAllBytes();
              InputStream readablePdfStream =
                  FileStorageCryptoPolicy.openForRead(
                      fileBytes, file.getFileType(), file.getUsername(), encryptionController);
              ParsePDFFieldsService parser = new ParsePDFFieldsService(readablePdfStream);
              if (parser.execute()) {
                Map<String, JSONObject> rectMap = new HashMap<>();
                JSONArray parsed = parser.getExtractedFields();
                for (int i = 0; i < parsed.length(); i++) {
                  JSONObject p = parsed.getJSONObject(i);
                  rectMap.put(p.getString("fieldName"), p);
                }
                for (int i = 0; i < fieldsArr.length(); i++) {
                  JSONObject fObj = fieldsArr.getJSONObject(i);
                  JSONObject pdfField = rectMap.get(fObj.getString("fieldName"));
                  if (pdfField != null) {
                    fObj.put("page", pdfField.optInt("page", 0));
                    if (pdfField.has("rect")) {
                      fObj.put("rect", pdfField.getJSONArray("rect"));
                    }
                    if (pdfField.has("widgetRects")) {
                      fObj.put("widgetRects", pdfField.getJSONArray("widgetRects"));
                    }
                  }
                }
              }
            }
          } catch (Exception e) {
            log.warn("Could not parse PDF for rect data in detail endpoint", e);
          }
        }

        JSONObject result = new JSONObject();
        result.put("registry", registryJson);
        result.put("fileId", fileIdHex);
        result.put("fields", fieldsArr);
        ctx.header("Content-Type", "application/json");
        ctx.result(result.toString());
      };

  public Handler servePdf =
      ctx -> {
        String fileIdStr = ctx.pathParam("fileId");
        ObjectId fileId = new ObjectId(fileIdStr);
        Optional<File.File> fileOpt = fileDao.get(fileId);
        if (fileOpt.isEmpty()) {
          ctx.status(404).result("{\"error\":\"File not found\"}");
          return;
        }
        File.File file = fileOpt.get();
        Optional<InputStream> streamOpt = fileDao.getStream(fileId);
        if (streamOpt.isEmpty()) {
          ctx.status(404).result("{\"error\":\"File stream not found\"}");
          return;
        }
        byte[] fileBytes = streamOpt.get().readAllBytes();
        try {
          InputStream decrypted =
              FileStorageCryptoPolicy.openForRead(
                  fileBytes, file.getFileType(), file.getUsername(), encryptionController);
          ctx.header("Content-Type", "application/pdf");
          ctx.result(decrypted);
        } catch (Exception e) {
          ctx.status(500).result("{\"error\":\"Failed to open PDF\"}");
        }
      };

  public Handler updateApplication =
      ctx -> {
        String id = ctx.pathParam("id");
        JSONObject req = new JSONObject(ctx.body());

        Optional<ApplicationRegistryEntry> entryOpt = registryDao.get(new ObjectId(id));
        if (entryOpt.isEmpty()) {
          ctx.status(404).result("{\"error\":\"Registry entry not found\"}");
          return;
        }
        ApplicationRegistryEntry entry = entryOpt.get();

        if (req.has("registryMetadata")) {
          JSONObject meta = req.getJSONObject("registryMetadata");
          if (meta.has("lookupKey")) entry.setLookupKey(meta.getString("lookupKey"));
          if (meta.has("idCategoryType"))
            entry.setIdCategoryType(meta.getString("idCategoryType"));
          if (meta.has("state")) entry.setState(meta.getString("state"));
          if (meta.has("applicationSubtype"))
            entry.setApplicationSubtype(meta.getString("applicationSubtype"));
          if (meta.has("pidlSubtype")) entry.setPidlSubtype(meta.optString("pidlSubtype", null));
          if (meta.has("amount")) entry.setAmount(meta.getString("amount"));
          if (meta.has("numWeeks")) entry.setNumWeeks(meta.getInt("numWeeks"));
          registryDao.update(entry);
        }

        if (req.has("fieldMappings") && entry.getOrgMappings() != null && !entry.getOrgMappings().isEmpty()) {
          ObjectId fileId = entry.getOrgMappings().get(0).getFileId();
          Optional<Form> formOpt = formDao.getByFileId(fileId);
          if (formOpt.isPresent()) {
            Form form = formOpt.get();
            JSONArray mappings = req.getJSONArray("fieldMappings");
            List<FormQuestion> questions = new ArrayList<>();
            for (int i = 0; i < mappings.length(); i++) {
              JSONObject m = mappings.getJSONObject(i);
              FormQuestion q =
                  new FormQuestion(
                      new ObjectId(),
                      FieldType.createFromString(m.optString("fieldType", "textField")),
                      m.getString("fieldName"),
                      m.optString("directive", null),
                      m.optString("displayLabel", m.getString("fieldName")),
                      "",
                      new ArrayList<>(),
                      "",
                      m.optBoolean("required", false),
                      3,
                      false,
                      new ObjectId(),
                      "NONE");
              questions.add(q);
            }
            FormSection newBody =
                new FormSection(
                    form.getBody().getTitle(),
                    form.getBody().getDescription(),
                    new LinkedList<>(),
                    questions);
            form.setBody(newBody);
            formDao.update(form);
          }
        }

        ctx.header("Content-Type", "application/json");
        ctx.result(new JSONObject().put("status", "updated").toString());
      };

  public Optional<User> userCheck(String req) {
    log.info("userCheck Helper started");
    try {
      JSONObject reqJson = new JSONObject(req);
      if (reqJson.has("targetUser")) {
        return this.userDao.get(reqJson.getString("targetUser"));
      }
    } catch (JSONException e) {
      System.out.println(e);
    }
    return Optional.empty();
  }
}
