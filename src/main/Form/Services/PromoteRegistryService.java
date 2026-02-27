package Form.Services;

import Config.DeploymentLevel;
import Config.Message;
import Config.Service;
import Database.ApplicationRegistry.ApplicationRegistryDao;
import Database.ApplicationRegistry.ApplicationRegistryDaoImpl;
import Database.File.FileDao;
import Database.File.FileDaoImpl;
import Database.Form.FormDao;
import Database.Form.FormDaoImpl;
import File.File;
import Form.ApplicationRegistryEntry;
import Form.Form;
import Form.FormMessage;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.json.JSONObject;

public class PromoteRegistryService implements Service {
  private final ApplicationRegistryDao stagingRegistryDao;
  private final FileDao stagingFileDao;
  private final FormDao stagingFormDao;
  private final ObjectId registryId;
  private JSONObject result;

  public PromoteRegistryService(
      ApplicationRegistryDao stagingRegistryDao,
      FileDao stagingFileDao,
      FormDao stagingFormDao,
      ObjectId registryId) {
    this.stagingRegistryDao = stagingRegistryDao;
    this.stagingFileDao = stagingFileDao;
    this.stagingFormDao = stagingFormDao;
    this.registryId = registryId;
  }

  public JSONObject getResult() {
    return result;
  }

  @Override
  public Message executeAndGetResponse() {
    try {
      Optional<ApplicationRegistryEntry> entryOpt = stagingRegistryDao.get(registryId);
      if (entryOpt.isEmpty()) {
        this.result = new JSONObject().put("error", "Registry entry not found in staging");
        return FormMessage.INVALID_PARAMETER;
      }

      ApplicationRegistryEntry stagingEntry = entryOpt.get();

      ApplicationRegistryDao prodRegistryDao =
          new ApplicationRegistryDaoImpl(DeploymentLevel.PRODUCTION);
      FileDao prodFileDao = new FileDaoImpl(DeploymentLevel.PRODUCTION);
      FormDao prodFormDao = new FormDaoImpl(DeploymentLevel.PRODUCTION);

      for (ApplicationRegistryEntry.OrgMapping mapping : stagingEntry.getOrgMappings()) {
        ObjectId stagingFileId = mapping.getFileId();
        Optional<File> fileOpt = stagingFileDao.get(stagingFileId);
        if (fileOpt.isPresent()) {
          File stagingFile = fileOpt.get();
          if (prodFileDao.get(stagingFile.getId()).isEmpty()) {
            prodFileDao.save(stagingFile);
          }
        }
        Optional<Form> formOpt = stagingFormDao.getByFileId(stagingFileId);
        if (formOpt.isPresent()) {
          Form stagingForm = formOpt.get();
          if (prodFormDao.get(stagingForm.getId()).isEmpty()) {
            prodFormDao.save(stagingForm);
          }
        }
      }

      String lookupKey = stagingEntry.getLookupKey();
      Optional<ApplicationRegistryEntry> prodExisting =
          (lookupKey != null && !lookupKey.isEmpty())
              ? prodRegistryDao.findByLookupKey(lookupKey)
              : prodRegistryDao.find(
                  stagingEntry.getIdCategoryType(),
                  stagingEntry.getState(),
                  stagingEntry.getApplicationSubtype(),
                  stagingEntry.getPidlSubtype());

      if (prodExisting.isPresent()) {
        for (ApplicationRegistryEntry.OrgMapping mapping : stagingEntry.getOrgMappings()) {
          ObjectId existingFileId = prodExisting.get().getFileIdForOrg(mapping.getOrgName());
          if (existingFileId == null) {
            prodRegistryDao.addOrgMapping(prodExisting.get().getId(), mapping);
          }
        }
      } else {
        stagingEntry.setId(new ObjectId());
        prodRegistryDao.save(stagingEntry);
      }

      this.result = new JSONObject().put("status", "promoted").put("lookupKey", lookupKey);
      return FormMessage.SUCCESS;
    } catch (Exception e) {
      this.result = new JSONObject().put("error", e.getMessage());
      return FormMessage.INVALID_FORM;
    }
  }
}
