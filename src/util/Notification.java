package util;

import java.util.UUID;

public class Notification {
  public UUID senderId;
  public String sender;
  public UUID id = UUID.randomUUID();
  public String location;
  public String message;
  public long timestamp;
  public Timestamp logicalTimestamp;
  public Severity severity;
}
