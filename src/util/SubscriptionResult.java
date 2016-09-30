package util;

import java.util.ArrayList;
import java.util.UUID;

public class SubscriptionResult {
  public SubscriptionResult(ArrayList<UUID> subscriptions, Timestamp timestamp) {
    this.subscriptions = subscriptions;
    this.timestamp = timestamp;
  }

  public ArrayList<UUID> subscriptions;
  public Timestamp timestamp;
}
