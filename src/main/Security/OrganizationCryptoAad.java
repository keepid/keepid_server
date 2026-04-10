package Security;

import java.util.Objects;
import org.bson.types.ObjectId;

/** AEAD string for org-scoped PDF encryption (ORG_DOCUMENT, FORM). */
public final class OrganizationCryptoAad {
  private OrganizationCryptoAad() {}

  public static String fromOrganizationId(ObjectId organizationId) {
    Objects.requireNonNull(organizationId, "organizationId");
    return organizationId.toHexString();
  }
}
