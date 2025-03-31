package Form;

import File.IdCategoryType;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

public enum ApplicationRegistry {
  SS$FED$INITIAL(
      IdCategoryType.SOCIAL_SECURITY_CARD,
      USStateOrCity.FED,
      Optional.empty(),
      ApplicationSubtype.INITIAL,
      BigDecimal.ZERO,
      1,
      new HashMap<String,ObjectId>()),

  // Fields that need to be verified: Application Subtype, amount, numWeeks
  SS$FED$REPLACEMENT(
      IdCategoryType.SOCIAL_SECURITY_CARD,
      USStateOrCity.FED,
      Optional.empty(),
      ApplicationSubtype.DUPLICATE,
      BigDecimal.ZERO,
      1,
      new HashMap<String,ObjectId>(){{
        this.put("Face to Face", new ObjectId("6725daa1ebfdb30698fff327"));
        this.put("TSA C.A.T.S Program", new ObjectId("672871ed2de24d7c8ba75c30"));
      }}),

  BC$PA$STANDARD(
      IdCategoryType.BIRTH_CERTIFICATE,
      USStateOrCity.PA,
      Optional.empty(),
      ApplicationSubtype.INITIAL,
      BigDecimal.ZERO,
      1,
      new HashMap<String,ObjectId>(){{
        this.put("TSA C.A.T.S Program", new ObjectId("67206e7e17d3b63a60456d45"));
      }}),
  BC$PA$DUPLICATE(
      IdCategoryType.BIRTH_CERTIFICATE,
      USStateOrCity.PA,
      Optional.empty(),
      ApplicationSubtype.DUPLICATE,
      new BigDecimal("20.0"),
      1,
          new HashMap<String,ObjectId>()),
  BC$PA$HOMELESS(
      IdCategoryType.BIRTH_CERTIFICATE,
      USStateOrCity.PA,
      Optional.empty(),
      ApplicationSubtype.HOMELESS,
      BigDecimal.ZERO,
      1,
          new HashMap<String,ObjectId>()),
  BC$PA$JUVENILE_JUSTICE_INVOLVED(
      IdCategoryType.BIRTH_CERTIFICATE,
      USStateOrCity.PA,
      Optional.empty(),
      ApplicationSubtype.JUVENILE_JUSTICE_INVOLVED,
      BigDecimal.ZERO,
      1,
          new HashMap<String,ObjectId>()),
  BC$PA$VETERANS(
      IdCategoryType.BIRTH_CERTIFICATE,
      USStateOrCity.PA,
      Optional.empty(),
      ApplicationSubtype.VETERANS,
      BigDecimal.ZERO,
      1,
      new HashMap<String,ObjectId>()),
  PIDL$PA$PI$INITIAL(
      IdCategoryType.DRIVERS_LICENSE_PHOTO_ID,
      USStateOrCity.PA,
      Optional.of(PIDLSubtype.PI),
      ApplicationSubtype.INITIAL,
      BigDecimal.ZERO,
      1,
      new HashMap<String,ObjectId>(){{
        this.put("TSA C.A.T.S Program", new ObjectId("6737d90505484e688bdc9420"));
      }}),
  PIDL$PA$PI$DUPLICATE(
      IdCategoryType.DRIVERS_LICENSE_PHOTO_ID,
      USStateOrCity.PA,
      Optional.of(PIDLSubtype.PI),
      ApplicationSubtype.DUPLICATE,
      BigDecimal.ZERO,
      1,
      new HashMap<String,ObjectId>(){{
        this.put("TSA C.A.T.S Program", new ObjectId("672871ed2de24d7c8ba75c30"));
      }}),
  PIDL$PA$PI$RENEWAL(
      IdCategoryType.DRIVERS_LICENSE_PHOTO_ID,
      USStateOrCity.PA,
      Optional.of(PIDLSubtype.PI),
      ApplicationSubtype.RENEWAL,
      BigDecimal.ZERO,
      1,
          new HashMap<String,ObjectId>(){{
            this.put("TSA C.A.T.S Program", new ObjectId("6720718f17d3b63a60456d7c"));
          }}),
  PIDL$PA$PI$CHANGE_OF_ADDRESS(
      IdCategoryType.DRIVERS_LICENSE_PHOTO_ID,
      USStateOrCity.PA,
      Optional.of(PIDLSubtype.PI),
      ApplicationSubtype.CHANGE_OF_ADDRESS,
      BigDecimal.ZERO,
      1,
          new HashMap<String,ObjectId>()),
  PIDL$PA$DL$INITIAL(
      IdCategoryType.DRIVERS_LICENSE_PHOTO_ID,
      USStateOrCity.PA,
      Optional.of(PIDLSubtype.DL),
      ApplicationSubtype.INITIAL,
      BigDecimal.ZERO,
      1,
          new HashMap<String,ObjectId>()),
  PIDL$PA$DL$DUPLICATE(
      IdCategoryType.DRIVERS_LICENSE_PHOTO_ID,
      USStateOrCity.PA,
      Optional.of(PIDLSubtype.DL),
      ApplicationSubtype.DUPLICATE,
      BigDecimal.ZERO,
      1,
          new HashMap<String,ObjectId>()),
  PIDL$PA$DL$RENEWAL(
      IdCategoryType.DRIVERS_LICENSE_PHOTO_ID,
      USStateOrCity.PA,
      Optional.of(PIDLSubtype.DL),
      ApplicationSubtype.RENEWAL,
      BigDecimal.ZERO,
      1,
          new HashMap<String,ObjectId>()),
  PIDL$PA$DL$CHANGE_OF_ADDRESS(
      IdCategoryType.DRIVERS_LICENSE_PHOTO_ID,
      USStateOrCity.PA,
      Optional.of(PIDLSubtype.DL),
      ApplicationSubtype.CHANGE_OF_ADDRESS,
      BigDecimal.ZERO,
      1,
      new HashMap<String,ObjectId>()),

  BC$MD$STANDARD(
        IdCategoryType.BIRTH_CERTIFICATE,
        USStateOrCity.MD,
        Optional.empty(),
        ApplicationSubtype.INITIAL,
        BigDecimal.ZERO,
        1,
        new HashMap<String,ObjectId>(){{
          this.put("TSA C.A.T.S Program", new ObjectId("6720679117d3b63a60456c33"));
        }}),

  BC$NJ$STANDARD(
          IdCategoryType.BIRTH_CERTIFICATE,
          USStateOrCity.NJ,
          Optional.empty(),
          ApplicationSubtype.INITIAL,
          BigDecimal.ZERO,
          1,
          new HashMap<String,ObjectId>(){{
            this.put("TSA C.A.T.S Program", new ObjectId("672877fc2de24d7c8ba75c95"));
          }});

  private final IdCategoryType idCategoryType;
  private final USStateOrCity usState;
  private final ApplicationSubtype applicationSubtype;
  private final Optional<PIDLSubtype> pidlSubtype;
  private final BigDecimal amount;
  private final int numWeeks;
  private final Map<String,ObjectId> orgsToFormIds;

  ApplicationRegistry(
      IdCategoryType idCategoryType,
      USStateOrCity usState,
      Optional<PIDLSubtype> pidlSubtype,
      ApplicationSubtype applicationSubtype,
      BigDecimal amount,
      int numWeeks,
      Map<String,ObjectId> orgsToFormsIds) {
    this.idCategoryType = idCategoryType;
    this.usState = usState;
    this.applicationSubtype = applicationSubtype;
    this.pidlSubtype = pidlSubtype;
    this.amount = amount;
    this.numWeeks = numWeeks;
    this.orgsToFormIds = orgsToFormsIds;
  }

  public String toString(String org) {
    return this.toJSON(org).toString();
  }

  public JSONObject toJSON(String org) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("idCategoryType", this.idCategoryType.toString());
    jsonObject.put("usState", this.usState.toString());
    jsonObject.put("applicationSubtype", this.applicationSubtype.toString());
    jsonObject.put("pidlSubtype", this.pidlSubtype.map(PIDLSubtype::toString).orElse(""));
    jsonObject.put("amount", this.amount.toString());
    jsonObject.put("numWeeks", this.numWeeks);
    jsonObject.put("blankFormId", this.orgsToFormIds.get(org).toString());
    return jsonObject;
  }
}
