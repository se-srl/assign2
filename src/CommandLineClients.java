import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import util.Notification;

public class CommandLineClients {

  public List<Notification> getNotifications() throws IOException, URISyntaxException, TimeoutException {
    // TODO handle the IO Exceptions and URISyntaxException
    Future<List<Notification>> futureNotifications = httpClient.retrieve("caution");

    for (int i = 0; i < retries; i++) {
      try {
        return futureNotifications.get(timeout, TimeUnit.MILLISECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException exception) {
        futureNotifications.cancel(true);
        futureNotifications = httpClient.retrieve("caution")
      }
    }

    futureNotifications.cancel(true);
    throw new TimeoutException("Server didn't respond in time.");
  }

  int timeout;
  int retries;
  HttpClient httpClient;
}
