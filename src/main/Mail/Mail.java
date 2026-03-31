package Mail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
  MailStatus mailStatus;

  String lobId;
  Date lobCreatedAt;
  String targetUsername;
  String requesterUsername;

  @BsonProperty(value = "expectedDeliveryDate")
  String expectedDeliveryDate;

  @BsonProperty(value = "trackingEvents")
  List<TrackingEvent> trackingEvents;

  @BsonProperty(value = "lobStatus")
  String lobStatus;

  @BsonProperty(value = "costCents")
  int costCents;

  @BsonProperty(value = "mailType")
  String mailType;

  @BsonProperty(value = "checkAmount")
  String checkAmount;

  @BsonProperty(value = "organizationName")
  String organizationName;

  public Mail() {
    this.trackingEvents = new ArrayList<>();
  }

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
    this.trackingEvents = new ArrayList<>();
    this.checkAmount = mailingAddress.getMaybeCheckAmount().toPlainString();
    this.mailType =
        mailingAddress.getMaybeCheckAmount().signum() > 0 ? "check" : "letter";
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

  public ObjectId getFileId() {
    return this.fileId;
  }

  public String getExpectedDeliveryDate() {
    return expectedDeliveryDate;
  }

  public List<TrackingEvent> getTrackingEvents() {
    return trackingEvents != null ? trackingEvents : new ArrayList<>();
  }

  public String getLobStatus() {
    return lobStatus;
  }

  public int getCostCents() {
    return costCents;
  }

  public String getMailType() {
    return mailType;
  }

  public String getCheckAmount() {
    return checkAmount;
  }

  public String getOrganizationName() {
    return organizationName;
  }

  public void applyResult(MailResult result) {
    this.lobId = result.getLobId();
    this.lobCreatedAt = result.getLobCreatedAt();
    this.mailStatus = MailStatus.MAILED;
    this.expectedDeliveryDate = result.getExpectedDeliveryDate();
    this.lobStatus = result.getLobStatus();
    this.trackingEvents = result.getTrackingEvents();
    if (result.getMailType() != null) {
      this.mailType = result.getMailType();
    }
  }

  public void applyRefreshResult(MailResult result) {
    this.expectedDeliveryDate = result.getExpectedDeliveryDate();
    this.lobStatus = result.getLobStatus();
    this.trackingEvents = result.getTrackingEvents();
    updateMailStatusFromTracking();
  }

  private void updateMailStatusFromTracking() {
    if (trackingEvents == null || trackingEvents.isEmpty()) return;
    for (TrackingEvent event : trackingEvents) {
      if ("Delivered".equalsIgnoreCase(event.getType())
          || "Delivered".equalsIgnoreCase(event.getName())) {
        this.mailStatus = MailStatus.DELIVERED;
        return;
      }
    }
    for (TrackingEvent event : trackingEvents) {
      String type = event.getType();
      if ("In Transit".equalsIgnoreCase(type)
          || "In Local Area".equalsIgnoreCase(type)
          || "Processed for Delivery".equalsIgnoreCase(type)) {
        this.mailStatus = MailStatus.IN_TRANSIT;
        return;
      }
    }
  }
}
