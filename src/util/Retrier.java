package util;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

public class Retrier {
  public static void doWithRetries(Callable<?> task, Callable<?> onFail, int retries) throws
  TimeoutException {
    for (int i = 0; i < retries; i++) {
      try {
        task.call();
        return;
      } catch (Exception exception) {
        try {
          onFail.call();
        } catch (Exception strangeException) {
          strangeException.printStackTrace();
          throw new RuntimeException("Unexpected behaviour. Exception handler threw exception.");
        }
      }
    }

    throw new TimeoutException("Failed to complete task with retries");
  }
}
