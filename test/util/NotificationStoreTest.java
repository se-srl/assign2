package util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

public class NotificationStoreTest {
  @Test
  public void retrievesAllNotifications() {
    String uuid = "4651c03e-76e5-4dbf-b2a6-0e196997ad8d";

    NotificationStore store = new NotificationStore();
    Notification notification = new Notification();
    notification.senderId = UUID.fromString(uuid);
    notification.sender = "Bureau of Peterology";
    notification.location = "Stormsville";
    notification.logicalTimestamp = new Timestamp(4);
    notification.message = "It's raining cats and dogs";
    notification.severity = Severity.CAUTION;

    store.add(notification);
    ArrayList<Notification> result = store.get(Severity.CAUTION, UUID.fromString(uuid), null);
    assertThat(result, hasItem(notification));
  }
}
