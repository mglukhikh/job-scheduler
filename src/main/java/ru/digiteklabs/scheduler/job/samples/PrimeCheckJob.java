package ru.digiteklabs.scheduler.job.samples;

import org.jetbrains.annotations.NotNull;
import ru.digiteklabs.scheduler.job.api.AbstractJob;
import ru.digiteklabs.scheduler.job.api.Job;

import java.util.Collections;
import java.util.Date;

/**
 * Sample job class with a purpose of checking whether a number is prime
 *
 * @author Mikhail Glukhikh
 */
public class PrimeCheckJob extends AbstractJob {

    public enum CheckResult {
        PRIME,
        NOT_PRIME,
        UNKNOWN
    }

    private final int number;

    private final PrimeCalcJob calcJob;

    private volatile CheckResult result;

    /**
     * Constructs a prime checker job.
     * @param plannedTime planned time to run
     * @param calcJob prime calculator job to take base primes
     * @param number number to check
     */
    public PrimeCheckJob(final Date plannedTime, final @NotNull PrimeCalcJob calcJob, final int number) {
        super(plannedTime, Collections.<Job>singleton(calcJob));
        this.calcJob = calcJob;
        this.number = number;
        result = CheckResult.UNKNOWN;
    }

    /**
     * Gets a result of a check
     * @return either number is prime, or non-prime, or result is unknown
     */
    public CheckResult getResult() {
        return result;
    }

    @Override
    public void run() {
        changeProgress(Job.PROGRESS_STARTED);
        for (int pr: calcJob.getPrimes()) {
            if (pr*pr > number) {
                result = CheckResult.PRIME;
                break;
            }
            if (number % pr == 0) {
                result = CheckResult.NOT_PRIME;
                break;
            }
        }
        if (result == CheckResult.UNKNOWN)
            result = CheckResult.PRIME;
        changeProgress(Job.PROGRESS_FINISHED);
        changePlannedTime(Job.PLANNED_TIME_NEVER);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Prime checker: ").append(number).append(" is ");
        switch (result) {
            case PRIME:
                sb.append("prime");
                break;
            case NOT_PRIME:
                sb.append("not prime");
                break;
            default:
                sb.append("unknown");
                break;
        }
        return sb.toString();
    }
}
