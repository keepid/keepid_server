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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

public class CreateApplicationService implements Service {
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

      String lookupKey = registryMetadata.optString("lookupKey", "");
      String idCategoryType = registryMetadata.getString("idCategoryType");
      String state = registryMetadata.getString("state");
      String applicationSubtype = registryMetadata.getString("applicationSubtype");
      String pidlSubtype = registryMetadata.optString("pidlSubtype", null);
      if (pidlSubtype != null && pidlSubtype.isEmpty()) pidlSubtype = null;
      BigDecimal amount = new BigDecimal(registryMetadata.optString("amount", "0"));
      int numWeeks = registryMetadata.optInt("numWeeks", 1);

      Optional<ApplicationRegistryEntry> existingEntry =
          lookupKey.isEmpty()
              ? registryDao.find(idCategoryType, state, applicationSubtype, pidlSubtype)
              : registryDao.findByLookupKey(lookupKey);

      ObjectId registryId;
      if (existingEntry.isPresent()) {
        registryId = existingEntry.get().getId();
        registryDao.addOrgMapping(registryId, new OrgMapping(orgName, fileId));
      } else {
        List<OrgMapping> mappings = new ArrayList<>();
        mappings.add(new OrgMapping(orgName, fileId));
        ApplicationRegistryEntry entry =
            new ApplicationRegistryEntry(
                lookupKey, idCategoryType, state, applicationSubtype, pidlSubtype, amount,
                numWeeks, mappings);
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
