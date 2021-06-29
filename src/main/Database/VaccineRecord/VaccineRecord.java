package Database.VaccineRecord;

import lombok.*;
import org.bson.types.ObjectId;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class VaccineRecord {

  public enum Manufacturer {
    PFIZER,
    MODERNA,
    UNKNOWN
  }

  public enum Dose {
    NONE,
    FIRST,
    SECOND,
    UNKNOWN
  }

  private ObjectId id;
  @NonNull private ObjectId userId;
  @NonNull private String orgName;
  @NonNull private long dateOfDose;
  @NonNull private long dateOfNextDose;
  @NonNull private String provider;
  @NonNull private Manufacturer manufacturer;
  @NonNull private Dose dose;
  String providerAddress;
}
