package ru.digiteklabs.scheduler.job.api;

/**
 * Separate interface for observing under job state
 *
 * @author Mikhail Glukhikh
 */
public interface JobObserver {

    /**
     * Called when a given job changes its progress
     *
     * @param job a given job
     * @param progress job's progress
     */
    void progressChanged(Job job, int progress);

    /**
     * Called when a given job changes its ready status
     *
     * @param job a given job
     * @param ready true if job is ready to run, false otherwise
     */
    void readyChanged(Job job, boolean ready);
}
