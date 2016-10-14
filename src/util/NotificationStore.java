package util;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
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
  public NotificationStore(Config config) {
    this(new File(config.getNotificationSaveFile()), config);

    notifications.put(Severity.NOTICE, new ExpiringLinkedHashMap<>(config.getNoticeMax()));
    notifications.put(Severity.CAUTION, new ExpiringLinkedHashMap<>(config.getCautionMax()));
    notifications.put(Severity.URGENT, new ExpiringLinkedHashMap<>(config.getUrgentMax()));
  }

  /**
   * Creates an empty store which is automatically written to the given file at the given interval.
   * @param file the file to be written to
   */
  NotificationStore(File file, Config config) {
    this.file = file;
    this.config = config;

    gson = new Gson();
  }

  public void load(String filename) throws FileNotFoundException {
    file = new File(filename);
    FileReader fr = new FileReader(file);
    notifications = gson.fromJson(fr, notifications.getClass());
  }

  /**
   * Adds a notification to the store.
   *
   * It implicitly rewrites any notifications in existence. This is okay, assuming that only one
   * process is sending requests for any given identifier.
   * @param notification the notification to add
   */
  public void add(util.Notification notification) {
    notifications.get(notification.severity).put(notification.id, notification);
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
    return new ArrayList<>(notifications.get(severity).values().stream()
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

  public void scheduleSaves() {
    final Runnable saver = () -> {
      // Do saving things. Only save things that haven't been saved before. This will probably
      // involve appending the JSON of each notification since the last write.
      try {
        FileWriter fw = new FileWriter(file);
        gson.toJson(notifications, fw);
      } catch (IOException e) {
        // Don't stress too much, it will be written over in writeInterval seconds anyway.
      }

    };

    scheduler.scheduleAtFixedRate(saver, config.getSaveInterval(), config.getSaveInterval(), TimeUnit.SECONDS);
  }

  protected File getFile() {
    return file;
  }

  private Config config;
  private Gson gson;
  private File file;

  /*
   * It may seem strange to store the notifications by their severity, as whenever a user
   * requires notifications, they always search by the notification creator. However, a regular
   * task that occurs involves removing notifications when there are too many. This is done by
   * severity.
   */
  private HashMap<Severity, ExpiringLinkedHashMap<UUID, Notification>> notifications =
    new HashMap<>();

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
}
