package util;

import java.util.UUID;

public class Registration {
  public Registration(Timestamp timestamp, UUID id) {
    this.timestamp = timestamp;
    this.id = id;
  }

  public Timestamp timestamp;
  public UUID id;
}
