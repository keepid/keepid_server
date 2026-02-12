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
        this.put("Face to Face", new ObjectId("6725da57ebfdb30698fff2eb"));
        this.put("TSA C.A.T.S Program", new ObjectId("672870a32de24d7c8ba75bf4"));
      }}),

  BC$PA$STANDARD(
      IdCategoryType.BIRTH_CERTIFICATE,
      USStateOrCity.PA,
      Optional.empty(),
      ApplicationSubtype.INITIAL,
      BigDecimal.ZERO,
      1,
      new HashMap<String,ObjectId>(){{
        this.put("TSA C.A.T.S Program", new ObjectId("67206bbb17d3b63a60456c48"));
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
        this.put("TSA C.A.T.S Program", new ObjectId("6737d8ac905dca46f9a0330e"));
      }}),
  PIDL$PA$PI$DUPLICATE(
      IdCategoryType.DRIVERS_LICENSE_PHOTO_ID,
      USStateOrCity.PA,
      Optional.of(PIDLSubtype.PI),
      ApplicationSubtype.DUPLICATE,
      BigDecimal.ZERO,
      1,
      new HashMap<String,ObjectId>(){{
        this.put("TSA C.A.T.S Program", new ObjectId("672870a32de24d7c8ba75bf4"));
      }}),
  PIDL$PA$PI$RENEWAL(
      IdCategoryType.DRIVERS_LICENSE_PHOTO_ID,
      USStateOrCity.PA,
      Optional.of(PIDLSubtype.PI),
      ApplicationSubtype.RENEWAL,
      BigDecimal.ZERO,
      1,
          new HashMap<String,ObjectId>(){{
            this.put("TSA C.A.T.S Program", new ObjectId("6720705917d3b63a60456d54"));
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
          this.put("TSA C.A.T.S Program", new ObjectId("67206361a4290f111ad4fd3f"));
        }}),

  BC$NJ$STANDARD(
          IdCategoryType.BIRTH_CERTIFICATE,
          USStateOrCity.NJ,
          Optional.empty(),
          ApplicationSubtype.INITIAL,
          BigDecimal.ZERO,
          1,
          new HashMap<String,ObjectId>(){{
            this.put("TSA C.A.T.S Program", new ObjectId("672870922de24d7c8ba75bee"));
          }});

  private final IdCategoryType idCategoryType;
  private final USStateOrCity usState;
  private final ApplicationSubtype applicationSubtype;
  private final Optional<PIDLSubtype> pidlSubtype;
  private final BigDecimal amount;
  private final int numWeeks;
  /**
   * Maps organization name to the file._id (from the "file" collection) of the annotated
   * form template. V2 services look up forms via formDao.getByFileId(), so these MUST be
   * file._id values, NOT form._id values.
   */
  private final Map<String,ObjectId> orgsToFileIds;

  ApplicationRegistry(
      IdCategoryType idCategoryType,
      USStateOrCity usState,
      Optional<PIDLSubtype> pidlSubtype,
      ApplicationSubtype applicationSubtype,
      BigDecimal amount,
      int numWeeks,
      Map<String,ObjectId> orgsToFileIds) {
    this.idCategoryType = idCategoryType;
    this.usState = usState;
    this.applicationSubtype = applicationSubtype;
    this.pidlSubtype = pidlSubtype;
    this.amount = amount;
    this.numWeeks = numWeeks;
    this.orgsToFileIds = orgsToFileIds;
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
    jsonObject.put("blankFormId", this.orgsToFileIds.get(org).toString());
    return jsonObject;
  }
}
