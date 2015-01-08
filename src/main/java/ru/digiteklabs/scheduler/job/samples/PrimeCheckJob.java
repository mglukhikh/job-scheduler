package ru.digiteklabs.scheduler.job.samples;

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

    public PrimeCheckJob(final Date plannedTime, final PrimeCalcJob calcJob, final int number) {
        super(plannedTime, Collections.<Job>singleton(calcJob));
        this.calcJob = calcJob;
        this.number = number;
        result = CheckResult.UNKNOWN;
    }

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
}
