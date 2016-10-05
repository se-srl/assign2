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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;

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
    this.executor = Executors.newCachedThreadPool();
  }

  public Future<UUID> register() throws IOException {
    clock.send();
    Request request = Request.Get(requestRoot + "/register");


    return executor.submit(() -> {
      String result = request.execute().returnContent().asString();
      Registration registration = gson.fromJson(result, Registration.class);

      clock.receive(registration.timestamp);
      id = registration.id;
      return registration.id;
    });
  }

  public Future<Notification> sendNotification(Notification notification) throws IOException {
    notification.logicalTimestamp = clock.getTime();
    notification.senderId = id;

    clock.send();

    Request request =  Request.Post(requestRoot + "/send").bodyString(gson.toJson(notification),
      ContentType.APPLICATION_JSON);

    return executor.submit(() -> {
      String result = request.execute().returnContent().asString();

      CreatedNotification created = gson.fromJson(result, CreatedNotification.class);
      clock.receive(created.time);
      return created.notification;
    });
  }

  public Future<List<UUID>> subscribe(UUID subscription) throws URISyntaxException, IOException {
    clock.send();

    URIBuilder builder = new URIBuilder(new URI(requestRoot + "/subscribe"));
    builder.setParameter("id", id.toString());
    builder.setParameter("subscription", subscription.toString());
    builder.setParameter("time", Integer.toString(clock.getTime().getTime()));

    Request request = Request.Get(builder.build());

    return executor.submit(() ->{
      String resultStr = request.execute().returnContent().asString();
      SubscriptionResult result = gson.fromJson(resultStr, SubscriptionResult.class);
      clock.receive(result.timestamp);

      return result.subscriptions;
    });
  }

  public Future<List<Notification>> retrieve(String severity) throws URISyntaxException, IOException {
    clock.send();

    URIBuilder builder = new URIBuilder(new URI(requestRoot + "/retrieve"));
    builder.setParameter("id", id.toString());
    builder.setParameter("severity", severity);
    builder.setParameter("time", Integer.toString(clock.getTime().getTime()));

    Request request = Request.Get(builder.build());

    return executor.submit(() -> {
      String resultStr = request.execute().returnContent().asString();
      RetrievalResult retrieved = gson.fromJson(resultStr, RetrievalResult.class);

      clock.receive(retrieved.timestamp);

      return retrieved.notifications;
    });
  }

  public UUID getId() {
    return id;
  }

  private Gson gson = new Gson();
  private UUID id;

  private String requestRoot;

  private ExecutorService executor;

  private LamportClock clock = new LamportClock();
}
