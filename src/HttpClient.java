import com.google.gson.Gson;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.UUID;

import util.CreatedNotification;
import util.LamportClock;
import util.Notification;
import util.NotificationStore;
import util.Registration;
import util.RetrievalResult;
import util.Severity;
import util.SubscriptionResult;
import util.Timestamp;

/**
 * A client for a MitterServer.
 *
 * <p>The MitterServer does not differentiate between notification senders, and notification
 * receivers. Nor does this client. It is intended to be used within other classes.
 */
public class HttpClient {
  public HttpClient(String requestRoot) {
    this.requestRoot = requestRoot;
  }

  public void register() throws IOException {
    clock.send();
    String result = Request.Get(requestRoot + "/register").execute().returnContent()
                         .asString();

    Registration registration = gson.fromJson(result, Registration.class);

    clock.receive(registration.timestamp);
    id = registration.id;
  }

  public void sendNotification(Notification notification) throws IOException {
    notification.logicalTimestamp = clock.getTime();
    notification.senderId = id;

    clock.send();
    String result = Request.Post(requestRoot + "/send")
                           .bodyString(gson.toJson(notification), ContentType.APPLICATION_JSON)
                           .execute().returnContent().asString();

    CreatedNotification created = gson.fromJson(result, CreatedNotification.class);
    clock.receive(created.time);
    sentStore.add(created.notification);
  }

  public void subscribe(UUID subscription) throws URISyntaxException, IOException {
    clock.send();

    URIBuilder builder = new URIBuilder(new URI(requestRoot + "/subscribe"));
    builder.setParameter("id", id.toString());
    builder.setParameter("subscription", subscription.toString());
    builder.setParameter("time", Integer.toString(clock.getTime().getTime()));

    String resultStr = Request.Get(builder.build()).execute().returnContent().asString();

    SubscriptionResult result = gson.fromJson(resultStr, SubscriptionResult.class);
    clock.receive(result.timestamp);
    subscriptions = result.subscriptions;
  }

  public void retrieve(String severity) throws URISyntaxException, IOException {
    clock.send();

    URIBuilder builder = new URIBuilder(new URI(requestRoot + "/retrieve"));
    builder.setParameter("id", id.toString());
    builder.setParameter("severity", severity);
    builder.setParameter("time", Integer.toString(clock.getTime().getTime()));

    String result = Request.Get(builder.build()).execute().returnContent().asString();
    RetrievalResult retrieved = gson.fromJson(result, RetrievalResult.class);

    clock.receive(retrieved.timestamp);

    for (Notification notification : retrieved.notifications) {
      System.out.print(notification.toString());
    }
  }

  public UUID getId() {
    return id;
  }

  /**
   * A collection of notifications that have been sent by this client. Obviously, the ID will
   * always be the same for each notification.
   */
  public NotificationStore sentStore = new NotificationStore();
  public ArrayList<UUID> subscriptions = new ArrayList<>();

  private Gson gson = new Gson();
  private UUID id;

  private String requestRoot;

  private LamportClock clock = new LamportClock();
}
