package Util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.stream.Collectors;

public class NotificationStore {
  NotificationStore(boolean writeToFile) {
    this.writeToFile = writeToFile;
  }

  public void add(Util.Notification notification) {
    notifications.get(notification.severity).add(notification);
  }

  public ArrayList<Notification> get(Severity severity, UUID subscription) {
    return new ArrayList<>(notifications.get(severity).stream()
                               .filter(notification -> notification.senderId == subscription)
                               .collect(Collectors.toList()));
  }

  public ArrayList<Notification> get(Severity severity, ArrayList<UUID> subscriptions) {
    ArrayList<Notification> relevantNotifications = new ArrayList<>();
    for (UUID subscription : subscriptions) {
      relevantNotifications.addAll(get(severity, subscription));
    }

    return relevantNotifications;
  }

  private boolean writeToFile;
  private LinkedHashMap<Severity, ArrayList<Notification> > notifications = new LinkedHashMap<>();
}
