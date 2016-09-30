package util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LamportClockTest {
  @Test
  public void receivesAndUpdatesTimestamps() {
    LamportClock clock = new LamportClock();
    clock.receive(new Timestamp(7));
    assertEquals(8, clock.getTime().getTime());

    clock.receive(new Timestamp(4));
    assertEquals(9, clock.getTime().getTime());
  }

  @Test
  public void sendsAndUpdatesTimestamps() {
    LamportClock clock = new LamportClock();
    clock.send();
    assertEquals(1, clock.getTime().getTime());
    clock.send();
    assertEquals(2, clock.getTime().getTime());
  }
}
