package clients;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import util.Notification;

public class CommandLineClient implements UrgentBroadcastListener {
  public CommandLineClient(HttpClient httpClient, int retries, int timeout) {
    this.httpClient = httpClient;
    this.retries = retries;
    this.timeout = timeout;

    httpClient.addListener(this);
  }

  public CommandLineClient(String hostname, int serverPort, int multicastPort, int retries, int
    timeout) throws IOException {
    this(new HttpClient(hostname, serverPort, multicastPort), retries, timeout);
  }

  @Override
  public void urgentNotificationReceived(Notification notification) {
    System.out.println("URGENT NOTIFICATION RECEIVED:");
    System.out.println(notification.toString());
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

  public boolean subscribe(String subscribee) {
    try {
      Future<List<UUID>> futureSubscriptions = httpClient.subscribe(UUID.fromString(subscribee));
      for (int i = 0; i < retries; i++) {
        if (futureSubscriptions.get().contains(UUID.fromString(subscribee))) {
          System.out.println("Subscribed!");
          return true;
        } else {
          System.err.println("Server failed to subscribe. Try again.");
          return false;
        }
      }
    } catch (InterruptedException | URISyntaxException | ExecutionException | IOException e) {
      System.err.println("Failed to send request. Try again.");
      return false;
    }
    return false;
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
      .parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    try {
      client.register();
    } catch (IOException | TimeoutException e) {
      System.err.println("System error. Try again later");
      System.exit(1);
    }

    Scanner input = new Scanner(System.in);
    System.out.println("Client ready to go.");
    System.out.println("UUID is " + client.getId());

    client.startListening();

    // Schedule tasks to receive notifications with "notice" severity.
    // scheduler.scheduleAtFixedRate(() -> {
    //   try {
    //     List<Notification> retrieved =  client.getNotifications("notice");

    //     System.out.println("NOTIFICATIONS RECEIVED");
    //     retrieved.forEach(System.out::println);
    //   } catch (IOException e) {
    //     e.printStackTrace();
    //   } catch (URISyntaxException e) {
    //     e.printStackTrace();
    //   } catch (TimeoutException e) {
    //     e.printStackTrace();
    //   }
    // }, 20, 20, TimeUnit.SECONDS);


    // Schedule tasks to receive notifications with "caution" severity.
/*    scheduler.scheduleWithFixedDelay(() -> {
      try {
        List<Notification> retrieved = client.getNotifications("caution");

        System.out.println("NOTIFICATIONS RECEIVED");
        retrieved.forEach(System.out::println);
      } catch (IOException e) {
        e.printStackTrace();
      } catch (URISyntaxException e) {
        e.printStackTrace();
      } catch (TimeoutException e) {
        e.printStackTrace();
      }
    }, 15, 15, TimeUnit.SECONDS);
*/
    while(input.hasNextLine()) {
      String line = input.nextLine();
      String[] components = line.split(" ");
      switch (components[0]) {
        case "subscribe":
          client.subscribe(components[1]);
          break;
        case "retrieve":
          List<Notification> notifications = client.getNotifications(components[1]);
          System.out.println(notifications);
          break;
        default:
          System.out.println("Not sure what you mean. Try " + possibleInputs);
      }
    }

  }

  private void startListening() throws IOException {
    httpClient.startListeningToBroadcast();
  }

  private String getId() {
    return httpClient.getId().toString();
  }

  static String possibleInputs = "subscribe";
  int timeout;
  int retries;

  HttpClient httpClient;
}
