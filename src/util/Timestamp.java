package util;

import java.util.UUID;

public class Timestamp implements Comparable<Timestamp> {
  public Timestamp(int time) {
    this.time = time;
  }

  @Override
  public int compareTo(Timestamp timestamp) {
    if (timestamp.time == this.time) {
      return this.id.compareTo(timestamp.id);
    }

    if (this.time < timestamp.time) {
      return -1;
    }

    return 1;
  }

  public int getTime() {
    return time;
  }

  private UUID id = UUID.randomUUID();
  private int time;
}
