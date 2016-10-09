import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

import clients.HttpClient;
import server.MitterServer;
import util.Notification;
import util.Severity;

import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class SillyTest {
  @Test
  public void doSomeThings() throws IOException, URISyntaxException, ExecutionException, InterruptedException {
    Thread server = new Thread(new MitterServerRunnable());
    server.start();

    HttpClient notificationServer = new HttpClient("http://" + hostname + ":" + mitterPort);
    notificationServer.register().get();
    assertNotNull(notificationServer.getId());

    HttpClient client = new HttpClient("http://" + hostname + ":" + mitterPort);
    client.register().get();

    // Subscribe the client to the notification server, and assert that the subscription has been
    // stored (implying it was received correctly by the mitter server)
    assertThat(client.subscribe(notificationServer.getId()).get(), hasItem(notificationServer.getId()));

    // Create a notification to send.
    Notification notification = new Notification();
    notification.sender = "me!";
    notification.severity = Severity.NOTICE;
    notification.location = "my house";
    notification.message = "Sometimes, things happen";
    notification.timestamp = 1475211772;

    Notification received = notificationServer.sendNotification(notification).get();

    // Assert that the sent notification is now stored properly.
    assertEquals("me!", received.sender);
    assertEquals("my house", received.location);
    assertEquals("Sometimes, things happen", notification.message);
    assertEquals(1475211772, notification.timestamp);

    // See if the client receives the notification. If they have, then it should show in the output.
    client.retrieve("notice");
  }

  public class MitterServerRunnable implements Runnable {
    @Override
    public void run() {
      try {
        MitterServer server = new MitterServer(hostname, mitterPort);
        server.init();
        server.start();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private String hostname = "localhost";
  private int mitterPort = 8080;
}
