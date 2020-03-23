package Organization;

public enum OrgEnrollmentStatus {
  ORG_EXISTS("ORG_EXISTS: Organization Exists Already"),
  SUCCESSFUL_ENROLLMENT("SUCCESSFUL_ENROLLMENT: Please Wait 1-3 Business Days For Response"),
  PASS_HASH_FAILURE("PASS_HASH_FAILURE: Server Password Failure, Please Try Again"),
  FIELD_EMPTY(""),
  NAME_LEN_OVER_30(""),
  EMAIL_LEN_OVER_40(""),
  INVALID_CHARACTERS(""),
  PASS_UNDER_8(""),
  INVALID_PARAMETER("INVALID_PARAMETER: Please Check Input");

  public String errorMessage;

  OrgEnrollmentStatus(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String toString() {
    return this.errorMessage;
  }

  public String getErrorName() {
    return this.errorMessage.split(":")[0];
  }

  public String getErrorDescription() {
    return this.errorMessage.split(":")[1];
  }
}
