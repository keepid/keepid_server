package Mail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MailResult {
  private String lobId;
  private Date lobCreatedAt;
  private String expectedDeliveryDate;
  private String lobStatus;
  private List<TrackingEvent> trackingEvents;
  private String mailType;

  public MailResult() {
    this.trackingEvents = new ArrayList<>();
  }

  public MailResult(
      String lobId,
      Date lobCreatedAt,
      String expectedDeliveryDate,
      String lobStatus,
      List<TrackingEvent> trackingEvents,
      String mailType) {
    this.lobId = lobId;
    this.lobCreatedAt = lobCreatedAt;
    this.expectedDeliveryDate = expectedDeliveryDate;
    this.lobStatus = lobStatus;
    this.trackingEvents = trackingEvents != null ? trackingEvents : new ArrayList<>();
    this.mailType = mailType;
  }

  public String getLobId() {
    return lobId;
  }

  public void setLobId(String lobId) {
    this.lobId = lobId;
  }

  public Date getLobCreatedAt() {
    return lobCreatedAt;
  }

  public void setLobCreatedAt(Date lobCreatedAt) {
    this.lobCreatedAt = lobCreatedAt;
  }

  public String getExpectedDeliveryDate() {
    return expectedDeliveryDate;
  }

  public void setExpectedDeliveryDate(String expectedDeliveryDate) {
    this.expectedDeliveryDate = expectedDeliveryDate;
  }

  public String getLobStatus() {
    return lobStatus;
  }

  public void setLobStatus(String lobStatus) {
    this.lobStatus = lobStatus;
  }

  public List<TrackingEvent> getTrackingEvents() {
    return trackingEvents;
  }

  public void setTrackingEvents(List<TrackingEvent> trackingEvents) {
    this.trackingEvents = trackingEvents;
  }

  public String getMailType() {
    return mailType;
  }

  public void setMailType(String mailType) {
    this.mailType = mailType;
  }
}
