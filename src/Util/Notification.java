package Util;

import java.util.UUID;

public class Notification {
  UUID senderId;
  String sender;
  UUID id = UUID.randomUUID();
  String location;
  String message;
  long timestamp;
  Timestamp logicalTimestamp;
  Severity severity;
}
