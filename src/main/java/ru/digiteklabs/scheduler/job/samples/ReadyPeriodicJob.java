package ru.digiteklabs.scheduler.job.samples;

import java.util.Date;

/**
 * A base class for periodic jobs that repeat on ready status
 * and not using some period of time
 *
 * @author Mikhail Glukhikh
 */
public class ReadyPeriodicJob extends PeriodicJob {

    /**
     * Constructs a periodic job that will start at a given time and will run for a given duration,
     * then will wait for a ready status of true and run again, and so
     * @param plannedTime a given start time
     * @param duration a given job duration in milliseconds
     * @param readyStatus ready status at the beginning
     */
    ReadyPeriodicJob(final Date plannedTime, long duration, boolean readyStatus) {
        super(plannedTime, duration, 0);
        changeReadyStatus(readyStatus);
    }

    /**
     * Gets information whether this job is deleted automatically from scheduling if completed
     *
     * Normally, this method should return true but for jobs that have successors
     * it's better to return false.
     *
     * @return true if it's allowed to delete job on completion, false otherwise
     */
    public final boolean autoDeletedOnCompletion() {
        return false;
    }

    @Override
    public String toString() {
        return "Ready." + super.toString();
    }
}
