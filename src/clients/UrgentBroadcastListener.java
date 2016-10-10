package clients;

import util.Notification;

public interface UrgentBroadcastListener {
  void urgentNotificationReceived(Notification notification);
}
