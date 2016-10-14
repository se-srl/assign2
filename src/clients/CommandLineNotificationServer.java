package clients;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import util.Config;
import util.Notification;
import util.Retrier;
import util.Severity;

public class CommandLineNotificationServer {
  CommandLineNotificationServer(Config config) throws IOException {
    this(new HttpClient(config), config);
  }

  public CommandLineNotificationServer(HttpClient httpClient, Config config) {
    this.httpClient = httpClient;
    this.config = config;
  }

  class SendTask implements Callable<Notification> {
    SendTask(Future<Notification> future, Notification notification) {
      this.future = future;
      this.notification = notification;
    }

    @Override
    public Notification call() throws Exception {
      future = httpClient.sendNotification(notification);
      Notification returned = future.get(config.getTimeout(), TimeUnit.MILLISECONDS);
      if (notification != null) {
        System.out.println("Successfully sent");
        return returned;
      }
      throw new Exception();
    }

    Future<Notification> future;
    Notification notification;
  }

  class SendFail implements Callable<Void> {
    SendFail(Future<Notification> future, Notification notification) {
      this.future = future;
      this.notification = notification;
    }

    @Override
    public Void call() throws Exception {
      future.cancel(true);
      return null;
    }

    Future<Notification> future;
    Notification notification;
  }

  private Notification createNotification(InputStream in) {
    Scanner input = new Scanner(System.in);
    Notification notification = new Notification();
    notification.timestamp = System.currentTimeMillis();
    System.out.println("Message: ");
    notification.message = input.nextLine();
    notification.sender = senderName;
    System.out.println("Location: ");
    notification.location = input.nextLine();
    System.out.println("Severity: ");
    while (notification.severity == null) {
      try {
        notification.severity = Severity.fromString(input.next());
      } catch (IllegalArgumentException illegal) {
        System.out.println(illegal.getMessage() + " Try again.");
      }
    }

    return notification;
  }

  public void send(Notification notification) throws InterruptedException, ExecutionException,
                                         TimeoutException, IOException {
    Future<Notification> created = null;

    try {
      Retrier.doWithRetries(new SendTask(created, notification), new SendFail(created,notification), config.getRetries());
    } catch (TimeoutException e) {
      System.out.println("Server problem, try again.");
    }
  }

  class RegisterTask implements Callable<Void> {
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

  class RegisterFail implements Callable<Void> {
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

  public void setName(String senderName) {
    this.senderName = senderName;
  }

  private String getId() {
    return httpClient.getId().toString();
  }

  public static void main(String[] args) throws TimeoutException, IOException, URISyntaxException {
    CommandLineNotificationServer client = new CommandLineNotificationServer(new Config(args[0]));

    try {
      client.register();
    } catch (TimeoutException e) {
      System.err.println("System error. Try again later");
      System.exit(1);
    }

    Scanner input = new Scanner(System.in);

    System.out.println("Give yourself a name: ");
    client.setName(input.nextLine());

    System.out.println("Ready to go.");
    System.out.println(client.getId());
    System.out.print("> ");

    while(input.hasNextLine()) {
      String line = input.nextLine();
      ArrayList<String> components = new ArrayList<>(Arrays.asList(line.split(" ")));
      switch (components.get(0)) {
        case "send":
          try {
            Notification notification = client.createNotification(System.in);
            client.send(notification);
          } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
          }
          break;
        default:
          System.out.println("Not sure what you mean. Try " + possibleInputs);
      }

      System.out.print("> ");
    }
  }

  private static String possibleInputs = "send";
  private Config config;
  private String senderName;
  private HttpClient httpClient;
}
