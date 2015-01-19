package ru.digiteklabs.scheduler.job.samples;

import ru.digiteklabs.scheduler.job.api.Job;
import ru.digiteklabs.scheduler.job.api.JobObserver;

import java.util.Date;

/**
 * A sample of a ready periodic job which is ready after a bound first job ends execution.
 *
 * Assembles ping-pong pair with FirstReadyJob.
 *
 */
public class SecondReadyJob extends ReadyPeriodicJob implements JobObserver {

    private Job firstJob;

    public SecondReadyJob(final Date plannedTime, long duration) {
        super(plannedTime, duration, false);
        firstJob = null;
    }

    public void setFirst(final Job job) {
        if (firstJob != job && firstJob != null)
            firstJob.removeObserver(this);
        firstJob = job;
        if (firstJob != null)
            firstJob.addObserver(this);
    }

    /**
     * Called when a given job changes its progress.
     *
     * The second job is ready when a bound first job is not executing
     *
     * @param job      a given job, normally should be the first job
     * @param progress job's progress
     */
    @Override
    public void progressChanged(Job job, int progress) {
        if (firstJob != job)
            return;
        if (progress != Job.PROGRESS_PLANNED && progress != Job.PROGRESS_FINISHED) {
            changeReadyStatus(false);
        } else {
            changeReadyStatus(true);
        }
    }

    /**
     * Called when a given job changes its ready status
     *
     * @param job   a given job
     * @param ready true if job is ready to run, false otherwise
     */
    @Override
    public void readyChanged(Job job, boolean ready) {
        // DO NOTHING
    }

    @Override
    public void run() {
        super.run();
        changeReadyStatus(!firstJob.getReadyStatus());
    }
}

