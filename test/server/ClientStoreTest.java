package server;

import org.junit.Test;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

public class ClientStoreTest {
  @Test
  public void retrievesSubscriptions() {
    UUID client = UUID.fromString("875a2867-2e9a-43ad-acc9-0085a50aba45");
    UUID notifier = UUID.fromString("4651c03e-76e5-4dbf-b2a6-0e196997ad8d");
    ClientStore store = new ClientStore();
    store.add(client, notifier);

    assertThat(store.getSubscriptions(client), hasItem(notifier));
  }
}
