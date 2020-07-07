package pt.inesctec.adcauthmiddleware.utils;

import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import org.slf4j.LoggerFactory;

public class Delayer {
  private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(Delayer.class);

  private final Object monitor = new Object();
  private final SortedMultiset<Duration> durations = TreeMultiset.create();
  private final long maxPoolSize;

  public Delayer(long requestDelaysPoolSize) {
    this.maxPoolSize = requestDelaysPoolSize;
  }

  public void delay(LocalDateTime startTime) {
    if (this.maxPoolSize <= 0) {
      return;
    }

    var taskEndTime = LocalDateTime.now();
    var taskDuration = Duration.between(startTime, taskEndTime);
    Duration thresholdDuration = null;

    synchronized (this.monitor) {
      thresholdDuration = this.calcThresholdDuration(taskDuration);
    }

    var currentTime = LocalDateTime.now();
    Duration currentDuration = Duration.between(startTime, currentTime);
    var sleepDuration = thresholdDuration.minus(currentDuration);

    Logger.debug("Sleeping thread for {}", sleepDuration);
    try {
      TimeUnit.MILLISECONDS.sleep(sleepDuration.toMillis());
    } catch (InterruptedException e) {
      Logger.error("Failed to sleep: {}", e.getMessage());
      Logger.debug("Stacktrace: ", e);
    }
  }

  private Duration calcThresholdDuration(Duration duration) {
    durations.add(duration);

    if (durations.size() <= this.maxPoolSize) {
      return this.durations.lastEntry().getElement();
    }

    durations.pollFirstEntry(); // delete smallest

    return durations.firstEntry().getElement();
  }
}
