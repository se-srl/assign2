package Util;

import java.util.ArrayList;
import java.util.UUID;

public class SubscriptionRequest extends Request {
  public UUID subscriber;
  public ArrayList<UUID> subscriptions;
}
