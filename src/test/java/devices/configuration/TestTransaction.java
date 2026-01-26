package devices.configuration;

import static org.springframework.test.context.transaction.TestTransaction.*;

import java.util.function.Supplier;

public class TestTransaction {

  public static void transactional(Runnable body) {
    if (!isActive()) {
      start();
    }
    body.run();
    flagForCommit();
    end();
  }

  public static <T> T transactional(Supplier<T> body) {
    if (!isActive()) {
      start();
    }
    T result = body.get();
    flagForCommit();
    end();
    return result;
  }
}
