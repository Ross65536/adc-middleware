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

  public void delay(LocalDateTime startTime) {
    var taskEndTime = LocalDateTime.now();
    var taskDuration = Duration.between(startTime, taskEndTime);
    Duration thresholdDuration = null;

    synchronized (this.monitor) {
      thresholdDuration = this.getMedian(taskDuration);
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

  private static final long MAX_DURATIONS = 40;

  // return 75th percentile of 100 worst performers
  private Duration getMedian(Duration duration) {
    durations.add(duration);

    if (durations.size() > MAX_DURATIONS) {
      durations.pollFirstEntry();
    }

    var skip = durations.size() / 4 - 1;
    var it = durations.descendingMultiset().iterator();
    for (long i = 0; i < skip; i++) {
      it.next();
    }

    return it.next();
  }
}
