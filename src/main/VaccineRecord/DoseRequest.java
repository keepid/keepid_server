package VaccineRecord;

import Database.VaccineRecord.VaccineRecord;
import lombok.*;
import org.bson.types.ObjectId;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class DoseRequest {

  private ObjectId userId;
  private String orgName;
  private String provider;
  private VaccineRecord.Manufacturer manufacturer;
  private Date date;
  private VaccineRecord.Dose dose;
  private String providerAddress;
}
