package clients;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import util.Notification;

public class CommandLineClient {
  public CommandLineClient(HttpClient httpClient, int retries, int timeout) {
    this.httpClient = httpClient;
    this.retries = retries;
    this.timeout = timeout;
  }

  public CommandLineClient(String requestRoot, int retries, int timeout) {
    this(new HttpClient(requestRoot), retries, timeout);
  }

  public List<Notification> getNotifications(String severity)
      throws IOException, URISyntaxException, TimeoutException {
    // TODO handle the IO Exceptions and URISyntaxException (Also fix main).
    Future<List<Notification>> futureNotifications = httpClient.retrieve(severity);

    for (int i = 0; i < retries; i++) {
      try {
        return futureNotifications.get(timeout, TimeUnit.MILLISECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException exception) {
        futureNotifications.cancel(true);
        futureNotifications = httpClient.retrieve(severity);
      }
    }

    futureNotifications.cancel(true);
    throw new TimeoutException("Server didn't respond in time.");
  }

  public void register() throws TimeoutException, IOException {
    Future<UUID> success = null;

    for (int i = 0; i < retries; i++) {
      try {
        success = httpClient.register();
        success.get(timeout, TimeUnit.MILLISECONDS);
        return;
      } catch (InterruptedException | ExecutionException | TimeoutException exception) {
        success.cancel(true);
        success = httpClient.register();
      }
    }

    success.cancel(true);
    throw new TimeoutException("Server didn't respond in time. Try again.");
  }

  public static void main(String[] args) throws TimeoutException, IOException, URISyntaxException {
    CommandLineClient client = new CommandLineClient(args[0], Integer.parseInt(args[1]), Integer
      .parseInt(args[2]));
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    try {
      client.register();
    } catch (IOException | TimeoutException e) {
      System.err.println("System error. Try again later");
      System.exit(1);
    }

    System.out.println("Client ready to go.");


    // Schedule tasks to receive notifications with "notice" severity.
    scheduler.scheduleAtFixedRate(() -> {
      try {
        client.getNotifications("notice");
      } catch (IOException e) {
        e.printStackTrace();
      } catch (URISyntaxException e) {
        e.printStackTrace();
      } catch (TimeoutException e) {
        e.printStackTrace();
      }
    }, 0, 30, TimeUnit.MINUTES);

    // Schedule tasks to receive notifications with "caution" severity.
    scheduler.scheduleAtFixedRate(() -> {
      try {
        client.getNotifications("caution");
      } catch (IOException e) {
        e.printStackTrace();
      } catch (URISyntaxException e) {
        e.printStackTrace();
      } catch (TimeoutException e) {
        e.printStackTrace();
      }
    }, 0, 1, TimeUnit.MINUTES);
  }

  int timeout;
  int retries;
  HttpClient httpClient;
}
