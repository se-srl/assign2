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

import util.Notification;

public class CommandLineNotificationServer {
  public CommandLineNotificationServer(HttpClient httpClient, int retries, int timeout) {
    this.httpClient = httpClient;
    this.retries = retries;
    this.timeout = timeout;
  }

  public CommandLineNotificationServer(String requestRoot, int retries, int timeout) {
    this(new HttpClient(requestRoot), retries, timeout);
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
    CommandLineNotificationServer client = new CommandLineNotificationServer(args[0],
                                                  Integer.parseInt(args[1]),
                                                  Integer.parseInt(args[2]));
    try {
      client.register();
    } catch (IOException | TimeoutException e) {
      System.err.println("System error. Try again later");
      System.exit(1);
    }

    Scanner input = new Scanner(System.in);
    System.out.println("Client ready to go.");

    while(input.hasNextLine()) {
      String line = input.nextLine();
      String[] components = line.split(" ");
      switch (components[0]) {
        case "send":
          client.send(components.);
          break;
        default:
          System.out.println("Not sure what you mean. Try " + possibleInputs);
      }
    }
  }

  static String possibleInputs = "send";
  int timeout;
  int retries;
  HttpClient httpClient;
}
