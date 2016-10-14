package clients;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
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

  public void send() throws InterruptedException, ExecutionException, TimeoutException, IOException {
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


    try {
      Future<Notification> created = httpClient.sendNotification(notification);
      for (int i = 0; i < config.getRetries(); i++) {
        try {
          if (created.get(config.getTimeout(), TimeUnit.MILLISECONDS) != null) {
            System.out.println("Successfully sent");
            return;
          }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
          created.cancel(true);
          created = httpClient.sendNotification(notification);
        }
      }
      throw new TimeoutException("Failed to connect after retrying.");
    } catch (IOException | TimeoutException e) {
      System.out.println("Server problem, try again.");
    }
  }

  public void register() {
    Future<UUID> success = null;

    try {
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

      throw new TimeoutException("Server didn't respond in time. Try again.");
    } catch(IOException | TimeoutException exception) {
      success.cancel(true);
      System.err.println("Server problem, try again.");
    }
  }

  public void setName(String senderName) {
    this.senderName = senderName;
  }

  private String getId() {
    return httpClient.getId().toString();
  }

  public static void main(String[] args) throws TimeoutException, IOException, URISyntaxException {
    CommandLineNotificationServer client = new CommandLineNotificationServer(new Config(args[0]));
    client.register();

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
            client.send();
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
