package util;

/**
 * An implementation of a Lamport clock.
 *
 * <p>Can respond to send and receive events, as required.
 */
public class LamportClock {
  public LamportClock() {
    currentTime = new Timestamp(0);
  }

  public void receive(Timestamp timestamp) {
    currentTime = new Timestamp((currentTime.compareTo(currentTime) == 1 ? currentTime.getTime() :
                                                                          timestamp.getTime()) + 1);
  }

  public void send() {
    currentTime = new Timestamp(currentTime.getTime() + 1);
  }

  public Timestamp getTime() {
    return currentTime;
  }

  private Timestamp currentTime;
}
