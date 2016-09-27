package Util;

import java.util.UUID;

public class Timestamp implements Comparable<Timestamp> {
  public Timestamp(int time) {
    this.time = time;
  }

  @Override
  public int compareTo(Timestamp t) {
    if (t.time == this.time) {
      return this.id.compareTo(t.id);
    }

    if (this.time < t.time) {
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
