package User;

import org.bson.codecs.pojo.annotations.BsonProperty;
import org.json.JSONObject;

import java.util.Objects;

public class Name {

  @BsonProperty(value = "first")
  private String first;

  @BsonProperty(value = "middle")
  private String middle;

  @BsonProperty(value = "last")
  private String last;

  @BsonProperty(value = "suffix")
  private String suffix;

  @BsonProperty(value = "maiden")
  private String maiden;

  public Name() {}

  public Name(String first, String last) {
    this.first = first;
    this.last = last;
  }

  public Name(String first, String middle, String last, String suffix, String maiden) {
    this.first = first;
    this.middle = middle;
    this.last = last;
    this.suffix = suffix;
    this.maiden = maiden;
  }

  public String getFirst() {
    return first;
  }

  public void setFirst(String first) {
    this.first = first;
  }

  public String getMiddle() {
    return middle;
  }

  public void setMiddle(String middle) {
    this.middle = middle;
  }

  public String getLast() {
    return last;
  }

  public void setLast(String last) {
    this.last = last;
  }

  public String getSuffix() {
    return suffix;
  }

  public void setSuffix(String suffix) {
    this.suffix = suffix;
  }

  public String getMaiden() {
    return maiden;
  }

  public void setMaiden(String maiden) {
    this.maiden = maiden;
  }

  public String getFullName() {
    StringBuilder sb = new StringBuilder();
    if (first != null && !first.isBlank()) sb.append(first);
    if (middle != null && !middle.isBlank()) sb.append(" ").append(middle);
    if (last != null && !last.isBlank()) sb.append(" ").append(last);
    if (suffix != null && !suffix.isBlank()) sb.append(" ").append(suffix);
    return sb.toString().trim();
  }

  public JSONObject serialize() {
    JSONObject json = new JSONObject();
    json.put("first", first);
    json.put("middle", middle);
    json.put("last", last);
    json.put("suffix", suffix);
    json.put("maiden", maiden);
    return json;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Name name = (Name) o;
    return Objects.equals(first, name.first)
        && Objects.equals(middle, name.middle)
        && Objects.equals(last, name.last)
        && Objects.equals(suffix, name.suffix)
        && Objects.equals(maiden, name.maiden);
  }

  @Override
  public int hashCode() {
    return Objects.hash(first, middle, last, suffix, maiden);
  }

  @Override
  public String toString() {
    return "Name{"
        + "first='" + first + '\''
        + ", middle='" + middle + '\''
        + ", last='" + last + '\''
        + ", suffix='" + suffix + '\''
        + ", maiden='" + maiden + '\''
        + '}';
  }
}
