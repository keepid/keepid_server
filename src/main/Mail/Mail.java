package Mail;

import java.util.Date;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

@Slf4j
@Setter
public class Mail {

  ObjectId id;
  ObjectId fileId;

  @BsonProperty(value = "mailingAddress")
  FormMailAddress mailingAddress;

  @BsonProperty(value = "mailStatus")
  MailStatus mailStatus; // CREATED, FAILED, MAILED

  String lobId;
  Date lobCreatedAt;
  String targetUsername;
  String requesterUsername;

  public Mail(
      ObjectId fileId,
      FormMailAddress mailingAddress,
      String targetUsername,
      String requesterUsername) {
    this.id = new ObjectId();
    this.fileId = fileId;
    this.mailingAddress = mailingAddress;
    this.mailStatus = MailStatus.CREATED;
    this.lobId = null;
    this.lobCreatedAt = null;
    this.targetUsername = targetUsername;
    this.requesterUsername = requesterUsername;
  }

  public ObjectId getId() {
    return id;
  }

  public FormMailAddress getMailingAddress() {
    return this.mailingAddress;
  }

  public String getTargetUsername() {
    return targetUsername;
  }

  public String getRequesterUsername() {
    return requesterUsername;
  }

  public MailStatus getMailStatus() {
    return this.mailStatus;
  }

  public String getLobId() {
    return lobId;
  }

  public Date getLobCreatedAt() {
    return lobCreatedAt;
  }

  public void setLobId(String id) {
    this.lobId = id;
  }

  public void setLobCreatedAt(Date lobCreatedAt) {
    this.lobCreatedAt = lobCreatedAt;
  }

  public ObjectId getFileId() {
    return this.fileId;
  }

  public void setMailStatus(MailStatus mailStatus) {
    this.mailStatus = mailStatus;
  }

  // First, Daniel calls this get all endpoint for all the form mail addresses
  // he will select one and return it in the response

  // retrieve information from frontend api
  // then, save the information into a Mail object with the correct FormMailAddress
  // validate this form to make sure that it matches with the possible form mail address
  // after validated, save the Mail object and then trigger Lob api request at
  // https://docs.lob.com/#tag/Letters/operation/letter_create
  // once Lob api request is returned, populate respective lob fields from lob, such as lob_id and
  // lob_created_at
  // start first in the sandbox lob environment, we should have a lob account and Connor will send
  // you the credentials
  // from there, test a couple of mail forms and see if they are going to the correct address

  // want a delete method, hook that up to lob just in case we want to cancel/delete a mailed form

}
