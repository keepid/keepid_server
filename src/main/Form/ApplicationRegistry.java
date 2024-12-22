package Form;

import File.IdCategoryType;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Optional;

public enum ApplicationRegistry {
  SS$FED$INITIAL(
      IdCategoryType.SOCIAL_SECURITY_CARD,
      USStateOrCity.FED,
      Optional.empty(),
      ApplicationSubtype.INITIAL,
      BigDecimal.ZERO,
      1,
      new ObjectId("123400000000000000000000")),

  BC$PA$INITIAL(
      IdCategoryType.BIRTH_CERTIFICATE,
      USStateOrCity.PA,
      Optional.empty(),
      ApplicationSubtype.INITIAL,
      BigDecimal.ZERO,
      1,
      new ObjectId("123400000000000000000000")),
  BC$PA$DUPLICATE(
      IdCategoryType.BIRTH_CERTIFICATE,
      USStateOrCity.PA,
      Optional.empty(),
      ApplicationSubtype.DUPLICATE,
      new BigDecimal("20.0"),
      1,
      new ObjectId("123400000000000000000000")),
  BC$PA$HOMELESS(
      IdCategoryType.BIRTH_CERTIFICATE,
      USStateOrCity.PA,
      Optional.empty(),
      ApplicationSubtype.HOMELESS,
      BigDecimal.ZERO,
      1,
      new ObjectId("123400000000000000000000")),
  BC$PA$JUVENILE_JUSTICE_INVOLVED(
      IdCategoryType.BIRTH_CERTIFICATE,
      USStateOrCity.PA,
      Optional.empty(),
      ApplicationSubtype.JUVENILE_JUSTICE_INVOLVED,
      BigDecimal.ZERO,
      1,
      new ObjectId("123400000000000000000000")),
  BC$PA$VETERANS(
      IdCategoryType.BIRTH_CERTIFICATE,
      USStateOrCity.PA,
      Optional.empty(),
      ApplicationSubtype.VETERANS,
      BigDecimal.ZERO,
      1,
      new ObjectId("123400000000000000000000")),
  PIDL$PA$PI$INITIAL(
      IdCategoryType.DRIVERS_LICENSE_PHOTO_ID,
      USStateOrCity.PA,
      Optional.of(PIDLSubtype.PI),
      ApplicationSubtype.INITIAL,
      BigDecimal.ZERO,
      1,
      new ObjectId("123400000000000000000000")),
  PIDL$PA$PI$DUPLICATE(
      IdCategoryType.DRIVERS_LICENSE_PHOTO_ID,
      USStateOrCity.PA,
      Optional.of(PIDLSubtype.PI),
      ApplicationSubtype.DUPLICATE,
      BigDecimal.ZERO,
      1,
      new ObjectId("123400000000000000000000")),
  PIDL$PA$PI$RENEWAL(
      IdCategoryType.DRIVERS_LICENSE_PHOTO_ID,
      USStateOrCity.PA,
      Optional.of(PIDLSubtype.PI),
      ApplicationSubtype.RENEWAL,
      BigDecimal.ZERO,
      1,
      new ObjectId("123400000000000000000000")),
  PIDL$PA$PI$CHANGE_OF_ADDRESS(
      IdCategoryType.DRIVERS_LICENSE_PHOTO_ID,
      USStateOrCity.PA,
      Optional.of(PIDLSubtype.PI),
      ApplicationSubtype.CHANGE_OF_ADDRESS,
      BigDecimal.ZERO,
      1,
      new ObjectId("123400000000000000000000")),
  PIDL$PA$DL$INITIAL(
      IdCategoryType.DRIVERS_LICENSE_PHOTO_ID,
      USStateOrCity.PA,
      Optional.of(PIDLSubtype.DL),
      ApplicationSubtype.INITIAL,
      BigDecimal.ZERO,
      1,
      new ObjectId("123400000000000000000000")),
  PIDL$PA$DL$DUPLICATE(
      IdCategoryType.DRIVERS_LICENSE_PHOTO_ID,
      USStateOrCity.PA,
      Optional.of(PIDLSubtype.DL),
      ApplicationSubtype.DUPLICATE,
      BigDecimal.ZERO,
      1,
      new ObjectId("123400000000000000000000")),
  PIDL$PA$DL$RENEWAL(
      IdCategoryType.DRIVERS_LICENSE_PHOTO_ID,
      USStateOrCity.PA,
      Optional.of(PIDLSubtype.DL),
      ApplicationSubtype.RENEWAL,
      BigDecimal.ZERO,
      1,
      new ObjectId("123400000000000000000000")),
  PIDL$PA$DL$CHANGE_OF_ADDRESS(
      IdCategoryType.DRIVERS_LICENSE_PHOTO_ID,
      USStateOrCity.PA,
      Optional.of(PIDLSubtype.DL),
      ApplicationSubtype.CHANGE_OF_ADDRESS,
      BigDecimal.ZERO,
      1,
      new ObjectId("123400000000000000000000"));

  private final IdCategoryType idCategoryType;
  private final USStateOrCity usState;
  private final ApplicationSubtype applicationSubtype;
  private final Optional<PIDLSubtype> pidlSubtype;
  private final BigDecimal amount;
  private final int numWeeks;
  private final ObjectId blankFormId;

  ApplicationRegistry(
      IdCategoryType idCategoryType,
      USStateOrCity usState,
      Optional<PIDLSubtype> pidlSubtype,
      ApplicationSubtype applicationSubtype,
      BigDecimal amount,
      int numWeeks,
      ObjectId blankFormId) {
    this.idCategoryType = idCategoryType;
    this.usState = usState;
    this.applicationSubtype = applicationSubtype;
    this.pidlSubtype = pidlSubtype;
    this.amount = amount;
    this.numWeeks = numWeeks;
    this.blankFormId = blankFormId;
  }

  @Override
  public String toString() {
    return this.toJSON().toString();
  }

  public JSONObject toJSON() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("idCategoryType", this.idCategoryType.toString());
    jsonObject.put("usState", this.usState.toString());
    jsonObject.put("applicationSubtype", this.applicationSubtype.toString());
    jsonObject.put("pidlSubtype", this.pidlSubtype.map(PIDLSubtype::toString).orElse(""));
    jsonObject.put("amount", this.amount.toString());
    jsonObject.put("numWeeks", this.numWeeks);
    jsonObject.put("blankFormId", this.blankFormId.toString());
    return jsonObject;
  }
}
