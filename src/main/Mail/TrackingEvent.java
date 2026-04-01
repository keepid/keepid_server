package Mail;

import java.util.Date;
import org.bson.codecs.pojo.annotations.BsonProperty;

public class TrackingEvent {

  @BsonProperty("type")
  private String type;

  @BsonProperty("name")
  private String name;

  @BsonProperty("time")
  private Date time;

  @BsonProperty("location")
  private String location;

  public TrackingEvent() {}

  public TrackingEvent(String type, String name, Date time, String location) {
    this.type = type;
    this.name = name;
    this.time = time;
    this.location = location;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Date getTime() {
    return time;
  }

  public void setTime(Date time) {
    this.time = time;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }
}
