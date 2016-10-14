package clients;

import org.mockito.internal.matchers.Not;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import util.Config;
import util.Notification;
import util.Retrier;

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

  class SubscribeTask implements Callable<List<UUID>> {
    SubscribeTask(Future<List<UUID>> future, String subscribee) {
      this.future = future;
      this.subscribee = subscribee;
    }

    @Override
    public List<UUID> call() throws Exception {
      future = httpClient.subscribe(UUID.fromString(subscribee));
      List<UUID> subscriptions = future.get(config.getTimeout(), TimeUnit .MILLISECONDS);
      if (subscriptions.contains(UUID.fromString(subscribee))) {
        System.out.println("Subscribed!");
        return subscriptions;
      }
      throw new Exception();
    }

    Future<List<UUID>> future;
    String subscribee;
  }

  class SubscribeFail implements Callable<Void> {
    SubscribeFail(Future<List<UUID>> future) {
      this.future = future;
    }

    @Override
    public Void call() throws Exception {
      future.cancel(true);
      return null;
    }

    Future<List<UUID>> future;
  }

  public void subscribe(String subscribee) throws TimeoutException {
    Future<List<UUID>> futureSubscriptions = null;


    Retrier.doWithRetries(new SubscribeTask(futureSubscriptions, subscribee), new SubscribeFail(futureSubscriptions), config.getRetries());
  }

  class GetTask implements Callable<Void> {
    GetTask(Future<List<Notification>> future, String severity) {
      this.future = future;
      this.severity = severity;
    }

    @Override
    public Void call() throws Exception {
      future = httpClient.retrieve(severity);

      List<Notification> notifications = future.get(config.getTimeout(), TimeUnit .MILLISECONDS);
      System.out.print(notifications);
      return null;
    }

    Future<List<Notification>> future;
    String severity;
  }

  class GetFail implements Callable<Void> {
    GetFail(Future<List<Notification>> future) {
      this.future = future;
    }

    @Override
    public Void call() throws Exception {
      future.cancel(true);
      return null;
    }

    Future<List<Notification>> future;
  }

  public void getNotifications(String severity)
      throws IOException, URISyntaxException, TimeoutException {
    // TODO handle the IO Exceptions and URISyntaxException (Also fix main).
    Future<List<Notification>> futureNotifications = httpClient.retrieve(severity);

    Retrier.doWithRetries(new GetTask(futureNotifications, severity), new GetFail(futureNotifications), config.getRetries());
  }

  private class RegisterTask implements Callable<Void> {
    RegisterTask(Future<UUID> future) {
      this.future = future;
    }

    @Override
    public Void call() throws Exception {
      future = httpClient.register();
      future.get(config.getTimeout(), TimeUnit.MILLISECONDS);

      return null;
    }

    Future<UUID> future;
  }

  private class RegisterFail implements Callable<Void> {
    RegisterFail(Future<UUID> future) {
      this.future = future;
    }

    @Override
    public Void call() throws Exception {
      future.cancel(true);
      return null;
    }

    Future<UUID> future;
  }

  public void register() throws TimeoutException {
    Future<UUID> success = null;
    Retrier.doWithRetries(new RegisterTask(success), new RegisterFail(success), config.getRetries());
  }

  public static void main(String[] args) throws TimeoutException, IOException, URISyntaxException {
    CommandLineClient client = new CommandLineClient(new Config(args[0]));

    try {
      client.register();
    } catch (TimeoutException e) {
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
          try {
            client.getNotifications(components[1]);
          } catch (TimeoutException timeout) {
            System.out.println("Connection problem. Try again");
          }
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
        System.out.println("Retrieving notifications:");
        getNotifications("caution");
      } catch (IOException | URISyntaxException | TimeoutException e) {
        e.printStackTrace();
      }
    }, config.getCautionInterval(), config.getCautionInterval(), TimeUnit.MINUTES);

    // Schedule tasks to receive notifications with "notice" severity.
    scheduler.scheduleAtFixedRate(() -> {
      try {
        System.out.println("Retrieving notifications:");
        getNotifications("notice");
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
