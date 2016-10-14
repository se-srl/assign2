//package server;
//
//import org.junit.Test;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.UUID;
//
//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.hamcrest.Matchers.empty;
//import static org.hamcrest.core.IsCollectionContaining.hasItem;
//import static org.hamcrest.core.IsCollectionContaining.hasItems;
//
//public class ClientStoreTest {
//  @Test
//  public void retrievesSubscriptions() {
//    UUID client = UUID.fromString("875a2867-2e9a-43ad-acc9-0085a50aba45");
//    UUID notifier = UUID.fromString("4651c03e-76e5-4dbf-b2a6-0e196997ad8d");
//    ClientStore store = new ClientStore(config, file);
//    store.add(client, notifier);
//
//    assertThat(store.getSubscriptions(client), hasItem(notifier));
//  }
//
//  @Test
//  public void returnsEmptySetForNoSubscriptions() {
//    ClientStore store = new ClientStore(config, file);
//    assertThat(store.getSubscriptions(UUID.randomUUID()), empty());
//  }
//
//  @Test
//  public void addsSingleSubscription() {
//    UUID client = UUID.randomUUID();
//    UUID notifier = UUID.randomUUID();
//
//    ClientStore store = new ClientStore(config, file);
//    store.add(client, notifier);
//
//    assertThat(store.getSubscriptions(client), hasItem(notifier));
//  }
//
//  @Test
//  public void addsMultipleSubscriptions() {
//    UUID client = UUID.randomUUID();
//    UUID notifier1 = UUID.randomUUID();
//    UUID notifier2 = UUID.randomUUID();
//
//    ClientStore store = new ClientStore(config, file);
//    store.addAll(client, new ArrayList<>(Arrays.asList(notifier1, notifier2)));
//    assertThat(store.getSubscriptions(client), hasItems(notifier1, notifier2));
//  }
//}
