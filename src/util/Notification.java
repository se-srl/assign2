package util;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class Notification {
  public String toString() {
    StringBuilder builder = new StringBuilder();
    Format format = new SimpleDateFormat("HH:mm dd/MM/yy");

    builder.append("Message: ").append(message).append("\n");
    builder.append("Sender: ").append(sender).append("\n");
    builder.append("Location: ").append(location).append("\n");
    builder.append("Severity: ").append(severity).append("\n");
    builder.append("Time: ").append(format.format(new Date(timestamp))).append("\n");

    return builder.toString();
  }

  public UUID senderId;
  public String sender;
  public UUID id = UUID.randomUUID();
  public String location;
  public String message;
  public long timestamp;
  public Timestamp logicalTimestamp;
  public Severity severity;
}
