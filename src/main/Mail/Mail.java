package Mail;

import org.bson.types.ObjectId;
import java.util.Date;

public class Mail {

  ObjectId form_id;
  ObjectId file_id;
  FormMailAddress mailing_address;
  MailStatus mailStatus; // CREATED, FAILED, MAILED
  String lob_id;
  Date lob_created_at;

  public Mail(ObjectId form_id,
              ObjectId file_id,
              FormMailAddress mailing_address,
              String lob_id,
              Date lob_created_at) {
    this.form_id = form_id;
    this.file_id = file_id;
    this.mailing_address = mailing_address;
    this.mailStatus = MailStatus.CREATED;
    this.lob_id = lob_id;
    this.lob_created_at = lob_created_at;
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
