package ru.digiteklabs.scheduler.job.samples;

import ru.digiteklabs.scheduler.job.api.AbstractJob;

import java.util.*;

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
        primes.add(2);
        search:
        for (int i = 3; i <= limit; i += 2) {
            for (int pr : primes) {
                if (pr * pr > i)
                    break;
                if (i % pr == 0)
                    continue search;
            }
            primes.add(i);
        }
    }

    /**
     * Gets information whether this job is deleted automatically from scheduling if completed
     *
     * Normally, this method should return true but for jobs that have successors
     * it's better to return false.
     *
     * @return true if it's allowed to delete job on completion, false otherwise
     */
    public boolean autoDeletedOnCompletion() {
        return false;
    }

    @Override
    public String toString() {
        return "Prime calculator: " + primes.size() + " primes up to " + limit;
    }
}
