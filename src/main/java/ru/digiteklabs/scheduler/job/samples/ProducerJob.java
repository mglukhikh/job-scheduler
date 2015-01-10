package ru.digiteklabs.scheduler.job.samples;

import ru.digiteklabs.scheduler.job.api.Job;
import ru.digiteklabs.scheduler.job.api.JobObserver;

import java.util.Date;

/**
 * A sample of producer job which is ready at the beginning
 * and after the bound consumer job ends execution. It is not ready
 * when the bound consumer job is on the run
 */
public class ProducerJob extends ReadyPeriodicJob implements JobObserver {

    private Job consumerJob;

    public ProducerJob(final Date plannedTime, long duration) {
        super(plannedTime, duration, true);
        consumerJob = null;
    }

    public void setConsumer(final Job job) {
        if (consumerJob != job && consumerJob != null)
            consumerJob.removeObserver(this);
        consumerJob = job;
        if (consumerJob != null)
            consumerJob.addObserver(this);
    }

    /**
     * Called when a given job changes its progress.
     *
     * The producer job is ready when a bound job is not executing
     *
     * @param job      a given job, normally should be consumer job
     * @param progress job's progress
     */
    @Override
    public void progressChanged(Job job, int progress) {
        if (consumerJob != job)
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
        changeReadyStatus(!consumerJob.getReadyStatus());
    }
}
