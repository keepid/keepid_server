package Form.Services;

import Config.Message;
import Config.Service;
import Database.ApplicationRegistry.ApplicationRegistryDao;
import Database.File.FileDao;
import Database.Form.FormDao;
import File.File;
import File.FileType;
import File.IdCategoryType;
import Form.*;
import Form.ApplicationRegistryEntry.OrgMapping;
import Security.EncryptionController;
import Security.FileStorageCryptoPolicy;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

public class CreateApplicationService implements Service {
  private static final String GLOBAL_ORG_MAPPING = "*";
  private final FileDao fileDao;
  private final FormDao formDao;
  private final ApplicationRegistryDao registryDao;
  private final EncryptionController encryptionController;
  private final String username;
  private final String orgName;
  private final String fileName;
  private final InputStream pdfStream;
  private final JSONArray fieldMappings;
  private final JSONObject registryMetadata;
  private JSONObject result;

  public CreateApplicationService(
      FileDao fileDao,
      FormDao formDao,
      ApplicationRegistryDao registryDao,
      EncryptionController encryptionController,
      String username,
      String orgName,
      String fileName,
      InputStream pdfStream,
      JSONArray fieldMappings,
      JSONObject registryMetadata) {
    this.fileDao = fileDao;
    this.formDao = formDao;
    this.registryDao = registryDao;
    this.encryptionController = encryptionController;
    this.username = username;
    this.orgName = orgName;
    this.fileName = fileName;
    this.pdfStream = pdfStream;
    this.fieldMappings = fieldMappings;
    this.registryMetadata = registryMetadata;
  }

  public JSONObject getResult() {
    return result;
  }

  @Override
  public Message executeAndGetResponse() {
    try {
      InputStream storedTemplateStream =
          FileStorageCryptoPolicy.prepareForStorage(
              pdfStream, FileType.FORM, username, encryptionController);

      File file =
          new File(
              username,
              new Date(),
              storedTemplateStream,
              FileType.FORM,
              IdCategoryType.NONE,
              fileName,
              orgName,
              true,
              "application/pdf");
      fileDao.save(file);
      ObjectId fileId = file.getId();

      List<FormQuestion> questions = new ArrayList<>();
      for (int i = 0; i < fieldMappings.length(); i++) {
        JSONObject mapping = fieldMappings.getJSONObject(i);
        String fieldName = mapping.getString("fieldName");
        String displayLabel = mapping.optString("displayLabel", fieldName);
        String directive = mapping.optString("directive", null);
        String fieldType = mapping.optString("fieldType", "textField");
        boolean required = mapping.optBoolean("required", false);

        List<String> options = new ArrayList<>();
        if (mapping.has("options")) {
          JSONArray opts = mapping.getJSONArray("options");
          for (int j = 0; j < opts.length(); j++) {
            options.add(opts.getString(j));
          }
        }

        FormQuestion q =
            new FormQuestion(
                new ObjectId(),
                FieldType.createFromString(fieldType),
                fieldName,
                directive,
                displayLabel,
                "",
                options,
                "",
                required,
                3,
                false,
                new ObjectId(),
                "NONE");
        questions.add(q);
      }

      FormSection body = new FormSection(fileName, "", new LinkedList<>(), questions);
      String metaTitle = registryMetadata.optString("title", fileName);
      String metaState = registryMetadata.optString("state", "");
      FormMetadata metadata =
          new FormMetadata(
              metaTitle,
              metaTitle,
              metaState,
              "",
              new HashSet<>(),
              LocalDateTime.now(),
              new ArrayList<>(),
              0);

      Form form =
          new Form(
              username,
              Optional.of(username),
              LocalDateTime.now(),
              Optional.of(LocalDateTime.now()),
              FormType.FORM,
              true,
              metadata,
              body,
              new ObjectId(),
              "");
      form.setFileId(fileId);
      formDao.save(form);

      String lookupKey = registryMetadata.optString("lookupKey", "").strip();
      if (lookupKey.isEmpty()) {
        throw new IllegalArgumentException("lookupKey is required");
      }
      Optional<ApplicationRegistryEntry> existingEntry = registryDao.findByLookupKey(lookupKey);

      ObjectId registryId;
      if (existingEntry.isPresent()) {
        ApplicationRegistryEntry entry = existingEntry.get();
        registryId = entry.getId();
        if (!metaTitle.isBlank()) {
          entry.setTitle(metaTitle);
          registryDao.update(entry);
        }
        registryDao.addOrgMapping(registryId, new OrgMapping(GLOBAL_ORG_MAPPING, fileId));
      } else {
        List<OrgMapping> mappings = new ArrayList<>();
        mappings.add(new OrgMapping(GLOBAL_ORG_MAPPING, fileId));
        ApplicationRegistryEntry entry =
            new ApplicationRegistryEntry(
                lookupKey,
                metaTitle,
                "GENERIC",
                "NA",
                "STANDARD",
                null,
                java.math.BigDecimal.ZERO,
                1,
                mappings);
        registryDao.save(entry);
        registryId = entry.getId();
      }

      this.result = new JSONObject();
      this.result.put("fileId", fileId.toHexString());
      this.result.put("formId", form.getId().toHexString());
      this.result.put("registryId", registryId.toHexString());
      return FormMessage.SUCCESS;

    } catch (Exception e) {
      this.result = new JSONObject().put("error", e.getMessage());
      return FormMessage.INVALID_FORM;
    }
  }
}
