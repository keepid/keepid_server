package Mail;

import java.util.Date;
import org.bson.types.ObjectId;

public class Mail {
  ObjectId id;
  ObjectId fileId;
  FormMailAddress mailing_address;
  MailStatus mailStatus; // CREATED, FAILED, MAILED
  String lobId;
  Date lobCreatedAt;
  String targetUsername;
  String requesterUsername;

  public Mail(
      ObjectId fileId,
      FormMailAddress mailing_address,
      String targetUsername,
      String requesterUsername) {
    this.id = new ObjectId();
    this.fileId = fileId;
    this.mailing_address = mailing_address;
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
    return this.mailing_address;
  }

  public String getTargetUsername() {
    return targetUsername;
  }

  public String getRequesterUsername() {
    return requesterUsername;
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
