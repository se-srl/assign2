package util;

public class CreatedNotification {
  public CreatedNotification(Timestamp time, Notification notification) {
    this.time = time;
    this.notification = notification;
  }
  public Timestamp time;
  public Notification notification;
}
