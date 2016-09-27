package MitterServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.UUID;

import Util.Timestamp;

public class ClientStore {
  public void add(UUID subscriber, UUID subscription) {
    subscriptions.get(subscriber).add(subscription);
  }

  public void addAll(UUID subscriber, ArrayList<UUID> newSubscriptions) {
    subscriptions.get(subscriber).addAll(newSubscriptions);
  }

  public ArrayList<UUID> getSubscriptions(UUID subscriber) {
    return subscriptions.get(subscriber);
  }

  public Timestamp getLastAcces(UUID subscriber) {
    return lastAccess.get(subscriber);
  }

  private LinkedHashMap<UUID, ArrayList<UUID> > subscriptions = new LinkedHashMap<>();
  private LinkedHashMap<UUID, Timestamp> lastAccess = new LinkedHashMap<>();
}
