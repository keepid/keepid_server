package User;

import OptionalUserInformation.*;
import lombok.Getter;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.io.Serializable;

/**
 * Wrapper class for optional user information.
 * Contains nested objects: person, basicInfo, demographicInfo, familyInfo, veteranStatus.
 * All fields are optional to maintain backward compatibility.
 */
@Getter
@Setter
public class OptionalInformation implements Serializable {
  private static final long serialVersionUID = 1L;

  @BsonProperty(value = "person")
  private Person person;

  @BsonProperty(value = "basicInfo")
  private BasicInfo basicInfo;

  @BsonProperty(value = "demographicInfo")
  private DemographicInfo demographicInfo;

  @BsonProperty(value = "familyInfo")
  private FamilyInfo familyInfo;

  @BsonProperty(value = "veteranStatus")
  private VeteranStatus veteranStatus;
}
