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

  /**
   * To be called when the process is receiving a message from another process with a timestamp.
   * Causes the times to be synchronized.
   * @param timestamp the timestamp sent by the other process
   */
  public void receive(Timestamp timestamp) {
    currentTime = new Timestamp((currentTime.compareTo(timestamp) == 1 ? currentTime.getTime() :
                                                                          timestamp.getTime()) + 1);
  }

  /**
   * To be called when the process is sending a message to another process. Causes the time to
   * increment.
   */
  public void send() {
    currentTime = new Timestamp(currentTime.getTime() + 1);
  }

  public Timestamp getTime() {
    return currentTime;
  }

  private Timestamp currentTime;
}
