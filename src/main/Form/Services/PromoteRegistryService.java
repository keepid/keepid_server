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
import Database.InteractiveFormConfig.InteractiveFormConfigDao;
import Database.InteractiveFormConfig.InteractiveFormConfigDaoImpl;
import File.File;
import Form.ApplicationRegistryEntry;
import Form.Form;
import Form.InteractiveFormConfig;
import Form.FormMessage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.json.JSONObject;

@Slf4j
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
      List<ApplicationRegistryEntry.OrgMapping> mappingsToPromote =
          stagingEntry.getOrgMappings() == null ? new ArrayList<>() : stagingEntry.getOrgMappings();
      if (mappingsToPromote.isEmpty()) {
        this.result =
            new JSONObject()
                .put("error", "Registry entry has no orgMappings to promote")
                .put("registryId", registryId.toHexString());
        return FormMessage.INVALID_PARAMETER;
      }

      ApplicationRegistryDao prodRegistryDao =
          new ApplicationRegistryDaoImpl(DeploymentLevel.PRODUCTION);
      FileDao prodFileDao = new FileDaoImpl(DeploymentLevel.PRODUCTION);
      FormDao prodFormDao = new FormDaoImpl(DeploymentLevel.PRODUCTION);
      InteractiveFormConfigDao stagingConfigDao =
          new InteractiveFormConfigDaoImpl(DeploymentLevel.STAGING);
      InteractiveFormConfigDao prodConfigDao =
          new InteractiveFormConfigDaoImpl(DeploymentLevel.PRODUCTION);

      for (ApplicationRegistryEntry.OrgMapping mapping : mappingsToPromote) {
        ObjectId stagingFileId = mapping.getFileId();
        Optional<File> fileOpt = stagingFileDao.get(stagingFileId);
        if (fileOpt.isPresent()) {
          File stagingFile = fileOpt.get();
          Optional<InputStream> stagingStreamOpt = stagingFileDao.getStream(stagingFileId);
          if (stagingStreamOpt.isEmpty()) {
            this.result =
                new JSONObject()
                    .put(
                        "error",
                        "Unable to promote: file bytes missing for staging fileId "
                            + stagingFileId.toHexString());
            return FormMessage.INVALID_FORM;
          }
          stagingFile.setFileStream(stagingStreamOpt.get());
          if (prodFileDao.get(stagingFile.getId()).isPresent()) {
            prodFileDao.update(stagingFile);
            log.info("Updated file {} in production", stagingFileId);
          } else {
            prodFileDao.save(stagingFile);
            log.info("Promoted file {} to production", stagingFileId);
          }
        }
        Optional<Form> formOpt = stagingFormDao.getByFileId(stagingFileId);
        if (formOpt.isPresent()) {
          Form stagingForm = formOpt.get();
          Optional<Form> prodFormByFileOpt = prodFormDao.getByFileId(stagingFileId);
          if (prodFormByFileOpt.isPresent()) {
            // Preserve production _id to avoid immutable _id replacement failures.
            stagingForm.setId(prodFormByFileOpt.get().getId());
            prodFormDao.update(stagingForm);
          } else {
            prodFormDao.save(stagingForm);
          }
        }

        Optional<InteractiveFormConfig> stagingConfigOpt = stagingConfigDao.getByFileId(stagingFileId);
        Optional<InteractiveFormConfig> prodConfigBeforeOpt =
            prodConfigDao.getByFileId(stagingFileId);
        if (stagingConfigOpt.isPresent()) {
          InteractiveFormConfig stagingConfig = stagingConfigOpt.get();
          stagingConfig.setFileId(stagingFileId);
          if (prodConfigBeforeOpt.isPresent()) {
            // Preserve production _id to avoid immutable _id replacement failures.
            stagingConfig.setId(prodConfigBeforeOpt.get().getId());
            prodConfigDao.update(stagingConfig);
          } else {
            stagingConfig.setId(new ObjectId());
            prodConfigDao.save(stagingConfig);
          }
        }
      }

      String lookupKey = stagingEntry.getLookupKey();
      Optional<ApplicationRegistryEntry> prodExistingByLookup =
          (lookupKey != null && !lookupKey.isEmpty())
              ? prodRegistryDao.findByLookupKey(lookupKey)
              : Optional.empty();
      Optional<ApplicationRegistryEntry> prodExistingByCompound =
          prodRegistryDao.find(
              stagingEntry.getIdCategoryType(),
              stagingEntry.getState(),
              stagingEntry.getApplicationSubtype(),
              stagingEntry.getPidlSubtype());
      Optional<ApplicationRegistryEntry> prodExisting =
          prodExistingByLookup.isPresent() ? prodExistingByLookup : prodExistingByCompound;

      if (prodExisting.isPresent()) {
        ApplicationRegistryEntry prodEntry = prodExisting.get();
        prodEntry.setLookupKey(stagingEntry.getLookupKey());
        prodEntry.setTitle(stagingEntry.getTitle());
        prodEntry.setIdCategoryType(stagingEntry.getIdCategoryType());
        prodEntry.setState(stagingEntry.getState());
        prodEntry.setApplicationSubtype(stagingEntry.getApplicationSubtype());
        prodEntry.setPidlSubtype(stagingEntry.getPidlSubtype());
        prodEntry.setAmount(stagingEntry.getAmount());
        prodEntry.setNumWeeks(stagingEntry.getNumWeeks());
        prodRegistryDao.update(prodEntry);
        for (ApplicationRegistryEntry.OrgMapping mapping : mappingsToPromote) {
          ObjectId existingFileId = prodEntry.getFileIdForOrg(mapping.getOrgName());
          boolean shouldReplaceMapping =
              existingFileId != null && !existingFileId.equals(mapping.getFileId());
          if (existingFileId == null) {
            prodRegistryDao.addOrgMapping(prodEntry.getId(), mapping);
          } else if (shouldReplaceMapping) {
            prodRegistryDao.removeOrgMapping(prodEntry.getId(), mapping.getOrgName());
            prodRegistryDao.addOrgMapping(prodEntry.getId(), mapping);
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
