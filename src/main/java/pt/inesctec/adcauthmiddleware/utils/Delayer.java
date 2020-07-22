package pt.inesctec.adcauthmiddleware.utils;

import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import org.slf4j.LoggerFactory;

/**
 * Class responsible for sleeping the invoking thread if execution time is below a certain computed threshold.
 */
public class Delayer {
  private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(Delayer.class);

  private final SortedMultiset<Duration> durations = TreeMultiset.create();
  private final long maxPoolSize;

  /**
   * constructor.
   * @param requestDelaysPoolSize the pool size (parameter N).
   */
  public Delayer(long requestDelaysPoolSize) {
    this.maxPoolSize = requestDelaysPoolSize;
  }

  /**
   * Entrypoint to instance.
   * Will calculate the difference between the start time and current time to determine execution time,
   * which will be placed in a pool if it's one of the worst N times.
   * The calling thread will be slept if the execution time is below the Nth worst time in the pool.
   * If the pool is not full the worst execution time in the pool is considered as the threshold.
   * The pool is a collection of previous execution times.
   *
   * @param startTime the start time of the code execution.
   */
  public void delay(LocalDateTime startTime) {
    if (this.maxPoolSize <= 0) {
      return;
    }

    var taskEndTime = LocalDateTime.now();
    var taskDuration = Duration.between(startTime, taskEndTime);
    Duration thresholdDuration = null;

    synchronized (this) {
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

  /**
   * Calculate the sleep threshold.
   *
   * @param duration the caller's execution time.
   * @return the threshold.
   */
  private Duration calcThresholdDuration(Duration duration) {
    durations.add(duration);

    if (durations.size() <= this.maxPoolSize) {
      return this.durations.lastEntry().getElement();
    }

    durations.pollFirstEntry(); // delete smallest

    return durations.firstEntry().getElement();
  }

  /**
   * Empties the pool.
   */
  public void reset() {
    synchronized (this) {
      this.durations.clear();
    }
  }
}
