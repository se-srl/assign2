package server;

import util.Timestamp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;

public class ClientStore {
  /**
   * Adds to the client's subscription record.
   *
   * <p>If there is no existing record, one is created.
   * @param subscriber the ID of the client
   * @param subscription the ID of the notification server to subscribe to
   */
  public void add(UUID subscriber, UUID subscription) {
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
    return lastAccess.get(subscriber);
  }

  public void setLastAccess(UUID subscriber, Timestamp timestamp) {
    lastAccess.put(subscriber, timestamp);
  }

  private LinkedHashMap<UUID, ArrayList<UUID>> subscriptions = new LinkedHashMap<>();
  private LinkedHashMap<UUID, Timestamp> lastAccess = new LinkedHashMap<>();
}
