package util;

import org.junit.Test;
import org.mockito.Spy;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class NotificationStoreTest {
  @Test
  public void retrievesANotification() {
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
    ArrayList<Notification> result = store.get(Severity.CAUTION, UUID.fromString(uuid),
                                               new Timestamp(2));
    assertThat(result, hasItem(notification));
  }

  @Test
  public void EmptySubscriptionReturnsEmptyResult() {
    NotificationStore store = new NotificationStore();
    assertThat(store.get(Severity.CAUTION, new ArrayList<UUID>(), new Timestamp(2)), empty());
  }

  @Test
  public void retrievesAllNotificationsWhenTimeNotSet() {
    NotificationStore store = new NotificationStore();

    String uuid = "4651c03e-76e5-4dbf-b2a6-0e196997ad8d";
    Notification n1 = new Notification();
    n1.senderId = UUID.fromString(uuid);
    n1.severity = Severity.URGENT;
    Notification n2 = new Notification();
    n2.senderId = UUID.fromString(uuid);
    n2.severity = Severity.URGENT;
    Notification n3 = new Notification();
    n3.senderId = UUID.randomUUID();
    n3.severity = Severity.URGENT;
    Notification n4 = new Notification();
    n4.senderId = UUID.fromString(uuid);
    n4.severity = Severity.CAUTION;

    store.add(n1);
    store.add(n2);
    store.add(n3);
    store.add(n4);

    ArrayList<Notification> result = store.get(Severity.URGENT, UUID.fromString(uuid), null);
    assertThat(result, hasItems(n1, n2));
  }

  @Test
  public void retrievesNotificationSinceTime() {
    String uuid = "4651c03e-76e5-4dbf-b2a6-0e196997ad8d";

    NotificationStore store = new NotificationStore();

    Notification n1 = new Notification();
    n1.logicalTimestamp = new Timestamp(4);
    n1.severity = Severity.NOTICE;
    n1.senderId = UUID.fromString(uuid);
    store.add(n1);

    Notification n2 = new Notification();
    n2.logicalTimestamp = new Timestamp(7);
    n2.severity = Severity.NOTICE;
    n2.senderId = UUID.fromString(uuid);
    store.add(n2);

    Notification n3 = new Notification();
    n3.logicalTimestamp = new Timestamp(9);
    n3.severity = Severity.NOTICE;
    n3.senderId = UUID.fromString(uuid);
    store.add(n3);

    ArrayList<Notification> result = store.get(Severity.NOTICE, UUID.fromString(uuid),
                                               new Timestamp(6));

    assertThat(result, hasItems(n2, n3));
  }

  @Test
  public void getsNotificationsForAllSubscriptions() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();

    NotificationStore store = new NotificationStore();
    NotificationStore spyStore = spy(store);

    spyStore.get(Severity.CAUTION, new ArrayList<>(Arrays.asList(id1, id2)), null);
    verify(spyStore).get(Severity.CAUTION, id1, null);
    verify(spyStore).get(Severity.CAUTION, id2, null);
  }
}
