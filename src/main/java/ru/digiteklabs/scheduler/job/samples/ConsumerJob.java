package ru.digiteklabs.scheduler.job.samples;

import ru.digiteklabs.scheduler.job.api.Job;
import ru.digiteklabs.scheduler.job.api.JobObserver;

import java.util.Date;

/**
 * A sample of consumer job which is ready after a producer job ends execution
 */
public class ConsumerJob extends ReadyPeriodicJob implements JobObserver {

    private Job producerJob;

    public ConsumerJob(final Date plannedTime, long duration) {
        super(plannedTime, duration, false);
        producerJob = null;
    }

    public void setProducer(final Job job) {
        if (producerJob != job && producerJob != null)
            producerJob.removeObserver(this);
        producerJob = job;
        if (producerJob != null)
            producerJob.addObserver(this);
    }

    /**
     * Called when a given job changes its progress.
     *
     * The consumer job is ready when a bound job is not executing
     *
     * @param job      a given job, normally should be producer job
     * @param progress job's progress
     */
    @Override
    public void progressChanged(Job job, int progress) {
        if (producerJob != job)
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
        changeReadyStatus(!producerJob.getReadyStatus());
    }
}

