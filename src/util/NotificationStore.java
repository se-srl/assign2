package util;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * An object for storing (and later retrieving) notifications, that may be used by Mitter
 * servers, notification servers, or clients.
 *
 * <p>It optionally writes to file at a given time interval.
 */
public class NotificationStore {
  /**
   * Creates an empty store, which is not automatically written to file, unless a file is added
   * later.
   */
  public NotificationStore() {
    notifications.put(Severity.CAUTION, new ArrayList<>());
    notifications.put(Severity.NOTICE, new ArrayList<>());
    notifications.put(Severity.URGENT, new ArrayList<>());
  }

  /**
   * Creates an empty store, which is automatically written to the given file at the given interval.
   * @param fileName the name of the file to be written to
   * @param writeInterval the duration of time between file writes, given in seconds.
   */
  NotificationStore(String fileName, int writeInterval) {
    this(new File(fileName), writeInterval);
  }

  /**
   * Creates an empty store which is automatically written to the given file at the given interval.
   * @param file the file to be written to
   * @param writeInterval the duration of the time between file writes, given in seconds
   */
  NotificationStore(File file, int writeInterval) {
    this();
    this.file = file;
    this.writeInterval = writeInterval;
  }

  /**
   * Adds a notification to the store.
   * @param notification the notification to add
   */
  public void add(util.Notification notification) {
    notifications.get(notification.severity).add(notification);
  }

  /**
   * Retrieves the notifications of a given notification server, and a severity.
   * @param severity the type of notification to retrieve
   * @param subscription the ID of the notification server
   * @param since the earliest timestamp the returned notifications should have. If null, all
   *              stored notifications matching the requirements are sent.
   * @return a list of notifications matching the requirements
   */
  public ArrayList<Notification> get(Severity severity, UUID subscription, Timestamp since) {
    return new ArrayList<>(notifications.get(severity).stream()
                               .filter(note -> note.senderId.equals(subscription))
                               .filter(note -> since == null ||
                                               note.logicalTimestamp.compareTo(since) == 1)
                               .collect(Collectors.toList()));
  }

  /**
   * Retrieves the notifications of a number of notifications servers, for a given severity.
   * @param severity the type of notification to retrieve
   * @param subscriptions a list of notification server IDs
   * @param since the earliest timestamp the returned notifications should have. If null, all
   *              stored notifications matching the requirements are sent.
   * @return a list of notifications matching the requirements
   */
  public ArrayList<Notification> get(Severity severity,
                                     ArrayList<UUID> subscriptions,
                                     Timestamp since) {
    ArrayList<Notification> relevantNotifications = new ArrayList<>();
    for (UUID subscription : subscriptions) {
      relevantNotifications.addAll(get(severity, subscription, since));
    }

    return relevantNotifications;
  }

  private int writeInterval;
  private File file;
  /*
   * It may seem strange to store the notifications by their severity, as whenever a user
   * requires notifications, they always search by the notification creator. However, a regular
   * task that occurs involves removing notifications when there are too many. This is done by
   * severity.
   */
  private LinkedHashMap<Severity, ArrayList<Notification>> notifications = new LinkedHashMap<>();
}
