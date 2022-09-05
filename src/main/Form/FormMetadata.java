package Form;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Stream;

public class FormMetadata implements Comparable<FormMetadata> {
  String title;
  String description;
  String state;
  String county;
  Set<ObjectId> prerequisities;
  LocalDateTime lastRevisedAt;
  // In order, amount of payment, method of payment,
  // who to send money to, and address
  List<String> paymentInfo;
  int numLines;

  public FormMetadata(
      String title,
      String description,
      String state,
      String county,
      Set<ObjectId> prerequisites,
      LocalDateTime lastRevisedAt,
      List<String> paymentInfo,
      int numLines) {
    this.title = title;
    this.description = description;
    this.state = state;
    this.county = county;
    this.prerequisities = prerequisites;
    this.lastRevisedAt = lastRevisedAt;
    this.numLines = numLines;
    this.paymentInfo = paymentInfo;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public String getState() {
    return state;
  }

  public String getCounty() {
    return county;
  }

  public LocalDateTime getLastRevisedAt() {
    return lastRevisedAt;
  }

  public int getNumLines() {
    return numLines;
  }

  public Set<ObjectId> getPrerequisites() {
    return prerequisities;
  }

  public List<String> getPaymentInfo() {
    return paymentInfo;
  }

  private Comparator<FormMetadata> getComparator() {
    return Comparator.comparing(FormMetadata::getTitle)
        .thenComparing(FormMetadata::getCounty)
        .thenComparing(FormMetadata::getDescription)
        .thenComparing(FormMetadata::getState)
        .thenComparing(FormMetadata::getNumLines)
        .thenComparing(metadata -> metadata.getLastRevisedAt())
        .thenComparingInt(
            metadata ->
                metadata.getPrerequisites().stream()
                    .flatMap(objectId -> Stream.of(objectId.hashCode()))
                    .reduce(Integer::sum)
                    .orElse(0))
        .thenComparing(
            metadata ->
                metadata.getPaymentInfo().stream().sorted().reduce(String::concat).orElse(""));
  }

  @Override
  public boolean equals(Object obj) {

    if (obj == null) {
      return false;
    }
    if (obj.getClass() != this.getClass()) {
      return false;
    }

    final FormMetadata other = (FormMetadata) obj;
    return getComparator().compare(this, other) == 0;
  }

  @Override
  public int compareTo(@NotNull FormMetadata metadata) {
    return getComparator().compare(this, metadata);
  }

  public static class MetadataCodec implements Codec<FormMetadata> {
    @Override
    public void encode(BsonWriter writer, FormMetadata value, EncoderContext encoderContext) {
      if (value != null) {
        writer.writeStartDocument();
        writer.writeName("title");
        writer.writeString(value.title);
        writer.writeName("description");
        writer.writeString(value.description);
        writer.writeName("state");
        writer.writeString(value.state);
        writer.writeName("county");
        writer.writeString(value.county);
        writer.writeName("lines");
        writer.writeInt32(value.numLines);
        writer.writeName("date");
        writer.writeDateTime(value.lastRevisedAt.toEpochSecond(ZoneOffset.UTC));
        writer.writeName("prereqsSize");
        writer.writeInt32(value.prerequisities.size());
        for (ObjectId prereq : value.prerequisities) {
          writer.writeObjectId(prereq);
        }
        writer.writeName("infoSize");
        writer.writeInt32(value.paymentInfo.size());
        for (String item : value.paymentInfo) {
          writer.writeName(item);
          writer.writeString(item);
        }
        writer.writeEndDocument();
      }
    }

    @Override
    public FormMetadata decode(BsonReader reader, DecoderContext decoderContext) {
      reader.readStartDocument();
      reader.readName();
      String title = reader.readString();
      reader.readName();
      String description = reader.readString();
      reader.readName();
      String state = reader.readString();
      reader.readName();
      String county = reader.readString();
      reader.readName();
      int numLines = reader.readInt32();
      reader.readName();
      LocalDateTime lastRevisedAt =
          LocalDateTime.ofEpochSecond(reader.readDateTime(), 0, ZoneOffset.UTC);
      reader.readName();
      int prereqsSize = reader.readInt32();
      Set<ObjectId> prerequisities = new TreeSet<>();
      for (int i = 0; i < prereqsSize; i++) {
        prerequisities.add(reader.readObjectId());
      }
      reader.readName();
      int paymentInfoSize = reader.readInt32();
      List<String> paymentInfo = new ArrayList<>();
      for (int i = 0; i < paymentInfoSize; i++) {
        reader.readName();
        paymentInfo.add(reader.readString());
      }
      reader.readEndDocument();
      return new FormMetadata(
          title, description, state, county, prerequisities, lastRevisedAt, paymentInfo, numLines);
    }

    @Override
    public Class<FormMetadata> getEncoderClass() {
      return FormMetadata.class;
    }
  }
}
