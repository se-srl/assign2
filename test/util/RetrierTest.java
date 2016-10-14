package util;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RetrierTest {
  @Before
  public void setup() {
    task = mock(Callable.class);
    onFail = mock(Callable.class);
  }

  @Test
  public void noRetriesNeeded() throws Exception {
    when(task.call()).thenReturn("Everything is fine");

    Retrier.doWithRetries(task, onFail, 3);
    verify(task, times(1)).call();
    verify(onFail, times(0)).call();
  }

  @Test
  public void succeedOnRetry() throws Exception {
    // The first time will throw an exception, but the retry will succeed
    doThrow(Exception.class).doReturn("Everything is fine").when(task).call();

    Retrier.doWithRetries(task, onFail, 3);
    verify(task, times(2)).call();
    verify(onFail, times(1)).call();
  }

  @Test
  (expected = TimeoutException.class)
  public void failsOnRetry() throws Exception {
    when(task.call()).thenThrow(Exception.class);

    Retrier.doWithRetries(task, onFail, 3);
    verify(task, times(3)).call();
    verify(onFail, times(3)).call();
  }

  Callable task;
  Callable onFail;
}