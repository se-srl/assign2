package server;

import com.google.gson.Gson;

import util.Config;
import util.Timestamp;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClientStore {
  public ClientStore(File file) {
    this.file = file;
  }

  public ClientStore() {
    this(new File("subscriptions.out"));
  }

  public void load(String filename) throws IOException {
    file = new File(filename);
    FileReader fr = new FileReader(file);
    subscriptions = gson.fromJson(fr, subscriptions.getClass());
  }

  /**
   * Adds to the client's subscription record.
   *
   * <p>If there is no existing record, one is created.
   * @param subscriber the ID of the client
   * @param subscription the ID of the notification server to subscribe to
   */
  public void add(UUID subscriber, UUID subscription) {
    System.out.println("Subscribing " + subscriber + " to " + subscription);

    if (!subscriptions.containsKey(subscriber)) {
      subscriptions.put(subscriber, new ArrayList<>());
    }
    subscriptions.get(subscriber).add(subscription);
  }

  /**
   * Adds a number of subscriptions to a clients' record.
   *
   * <p>If there is no existing record, one is created.
   * @param subscriber the ID of the client
   * @param newSubscriptions a list of IDs of the notification servers to subscribe to.
   */
  public void addAll(UUID subscriber, ArrayList<UUID> newSubscriptions) {
    if (!subscriptions.containsKey(subscriber)) {
      subscriptions.put(subscriber, new ArrayList<>());
    }
    subscriptions.get(subscriber).addAll(newSubscriptions);
  }

  /**
   * Retrieves the notification servers that a client is subscribed to.
   * @param subscriber the ID of the client
   * @return a list of IDs of the notification servers.
   */
  public ArrayList<UUID> getSubscriptions(UUID subscriber) {
    if (subscriptions.containsKey(subscriber)) {
      return subscriptions.get(subscriber);
    } else {
      // Override the default behaviour.
      return new ArrayList<>();
    }
  }

  /**
   * Gets the logical timestamp of the client's last retrieval.
   * @param subscriber the ID of the client
   * @return a Timestamp recorded at the time of the last access.
   */
  public Timestamp getLastAccess(UUID subscriber) {
    Timestamp time = lastAccess.get(subscriber);
    return time;
  }

  public void scheduleSaves() {
    final Runnable saver = () -> {
      // Do saving things. Only save things that haven't been saved before. This will probably
      // involve appending the JSON of each notification since the last write.
      try {
        FileWriter fw = new FileWriter(file);
        // Clear the empty string.
        fw.write("");
        gson.toJson(subscriptions, fw);
      } catch (IOException e) {
        // Don't stress too much, it will be written over in writeInterval seconds anyway.
      }

    };

    scheduler.scheduleAtFixedRate(saver, 5, 5, TimeUnit.MINUTES);
  }

  public void setLastAccess(UUID subscriber, Timestamp timestamp) {
    lastAccess.put(subscriber, timestamp);
  }

  private File file;
  private Gson gson = new Gson();
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  private LinkedHashMap<UUID, ArrayList<UUID>> subscriptions = new LinkedHashMap<>();
  private LinkedHashMap<UUID, Timestamp> lastAccess = new LinkedHashMap<>();
}
