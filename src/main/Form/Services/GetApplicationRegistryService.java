package Form.Services;

import Config.Message;
import Config.Service;
import Database.ApplicationRegistry.ApplicationRegistryDao;
import Form.ApplicationRegistryEntry;
import Form.FormMessage;
import java.util.Objects;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.json.JSONObject;

public class GetApplicationRegistryService implements Service {
  private final ApplicationRegistryDao registryDao;
  private final String type;
  private final String state;
  private final String situation;
  private final String person;
  private final String org;
  private String applicationRegistry;

  public GetApplicationRegistryService(
      ApplicationRegistryDao registryDao,
      String type,
      String state,
      String situation,
      String person,
      String org) {
    this.registryDao = registryDao;
    this.type = type;
    this.state = state;
    this.situation = situation;
    this.person = person;
    this.org = org;
  }

  public String getJsonInformation() {
    Objects.requireNonNull(this.applicationRegistry);
    return this.applicationRegistry;
  }

  @Override
  public Message executeAndGetResponse() {
    String lookupKey = type + "$" + state + "$" + situation;
    Optional<ApplicationRegistryEntry> entryOpt = registryDao.findByLookupKey(lookupKey);
    if (entryOpt.isEmpty()) {
      // Backward compatibility for legacy/non-standard keys persisted with "#"
      String legacyLookupKey = type + "#" + state + "#" + situation;
      entryOpt = registryDao.findByLookupKey(legacyLookupKey);
    }
    if (entryOpt.isEmpty()) {
      return FormMessage.INVALID_PARAMETER;
    }

    ApplicationRegistryEntry entry = entryOpt.get();
    ObjectId fileId = entry.getFileIdForOrg(org);
    if (fileId == null) {
      return FormMessage.INVALID_PARAMETER;
    }

    JSONObject json = new JSONObject();
    json.put("idCategoryType", entry.getIdCategoryType());
    json.put("usState", entry.getState());
    json.put("applicationSubtype", entry.getApplicationSubtype());
    json.put("pidlSubtype", entry.getPidlSubtype() != null ? entry.getPidlSubtype() : "");
    json.put("amount", entry.getAmount());
    json.put("numWeeks", entry.getNumWeeks());
    json.put("blankFormId", fileId.toHexString());

    this.applicationRegistry = json.toString();
    return FormMessage.SUCCESS;
  }
}
