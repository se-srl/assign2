package clients;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import util.Config;
import util.Notification;

public class CommandLineClient implements UrgentBroadcastListener {
  public CommandLineClient(Config config) throws IOException {
    this(new HttpClient(config), config);
  }

  public CommandLineClient(HttpClient httpClient, Config config) {
    this.httpClient = httpClient;
    this.config = config;
    httpClient.addListener(this);
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

    for (int i = 0; i < config.getRetries(); i++) {
      try {
        return futureNotifications.get(config.getTimeout(), TimeUnit.MILLISECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException exception) {
        futureNotifications.cancel(true);
        futureNotifications = httpClient.retrieve(severity);
      }
    }

    futureNotifications.cancel(true);
    throw new TimeoutException("Server didn't respond in time.");
  }

  public boolean subscribe(String subscribee) throws TimeoutException {
    Future<List<UUID>> futureSubscriptions = null;


    for (int i = 0; i < config.getRetries(); i++) {
      try {
        futureSubscriptions = httpClient.subscribe(UUID.fromString(subscribee));
        if (futureSubscriptions.get(config.getTimeout(), TimeUnit.MILLISECONDS).contains(UUID.fromString
                                                                                              (subscribee))) {
          System.out.println("Subscribed!");
          return true;
        }
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        futureSubscriptions.cancel(true);
        // It seems like there are unhandled exceptions here, but it will have been caught the
        // first time the request was sent, and handled below.
        try {
          futureSubscriptions = httpClient.subscribe(UUID.fromString(subscribee));
        } catch (URISyntaxException | IOException e1) {
          // Do nothing, because this will literally never happen.
        }
      } catch (IOException | URISyntaxException e) {
        throw new RuntimeException("Unexpected error. Try again");
      }
    }
    throw new TimeoutException("Server didn't respond in time, try again.");
  }

  public void register() throws TimeoutException, IOException {
    Future<UUID> success = null;

    for (int i = 0; i < config.getRetries(); i++) {
      try {
        success = httpClient.register();
        success.get(config.getTimeout(), TimeUnit.MILLISECONDS);
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
    CommandLineClient client = new CommandLineClient(new Config(args[0]));

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
    // Schedule tasks to receive notifications with "caution" severity.
    scheduler.scheduleWithFixedDelay(() -> {
      try {
        List<Notification> retrieved = getNotifications("caution");

        System.out.println("NOTIFICATIONS RECEIVED");
        retrieved.forEach(System.out::println);
      } catch (IOException | URISyntaxException | TimeoutException e) {
        e.printStackTrace();
      }
    }, config.getCautionInterval(), config.getCautionInterval(), TimeUnit.MINUTES);

    // Schedule tasks to receive notifications with "notice" severity.
    scheduler.scheduleAtFixedRate(() -> {
      try {
        List<Notification> retrieved = getNotifications("notice");

        System.out.println("NOTIFICATIONS RECEIVED");
        retrieved.forEach(System.out::println);
      } catch (IOException | URISyntaxException | TimeoutException e) {
        // Do nothing. The next retrieval will get them.
      }
    }, config.getNoticeInterval(), config.getNoticeInterval(), TimeUnit.MINUTES);

    // Join the multicast group to receive urgent notifications.
    httpClient.startListeningToBroadcast();
  }

  private String getId() {
    return httpClient.getId().toString();
  }

  static String possibleInputs = "subscribe <id> or retrieve <severity>";
  ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  Config config;

  HttpClient httpClient;
}
