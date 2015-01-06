package ru.digiteklabs.scheduler.job.samples;

import ru.digiteklabs.scheduler.job.api.AbstractJob;
import ru.digiteklabs.scheduler.job.api.Job;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Sample job with a purpose of calculating prime numbers.
 *
 * After run() is performed, this class is effectively immutable
 *
 * @author Mikhail Glukhikh
 */
public class PrimeCalcJob extends AbstractJob {

    private final List<Integer> primes = new ArrayList<Integer>();

    private final int limit;

    public PrimeCalcJob(final Date plannedTime, int limit) {
        super(plannedTime);
        this.limit = limit;
    }

    public List<Integer> getPrimes() {
        return Collections.unmodifiableList(primes);
    }

    @Override
    public void run() {
        changeProgress(Job.PROGRESS_STARTED);
        primes.add(2);
        search: for (int i=3; i<=limit; i+=2) {
            for (int pr: primes) {
                if (pr*pr > i)
                    break;
                if (i % pr == 0)
                    continue search;
            }
            primes.add(i);
        }
        changeProgress(Job.PROGRESS_FINISHED);
        changePlannedTime(Job.PLANNED_TIME_NEVER);
    }
}
