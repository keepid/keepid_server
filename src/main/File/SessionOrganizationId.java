package File;

import io.javalin.http.Context;
import java.util.Optional;
import org.bson.types.ObjectId;

public final class SessionOrganizationId {
  private SessionOrganizationId() {}

  public static Optional<ObjectId> fromContext(Context ctx) {
    String hex = ctx.sessionAttribute("organizationId");
    if (hex == null || hex.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(new ObjectId(hex));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
