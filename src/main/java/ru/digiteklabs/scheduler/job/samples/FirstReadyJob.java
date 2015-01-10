package ru.digiteklabs.scheduler.job.samples;

import ru.digiteklabs.scheduler.job.api.Job;
import ru.digiteklabs.scheduler.job.api.JobObserver;

import java.util.Date;

/**
 * A sample of a ready periodic job which is ready at the beginning
 * and after the bound second job ends execution. It is not ready
 * when the bound second job is on the run
 */
public class FirstReadyJob extends ReadyPeriodicJob implements JobObserver {

    private Job secondJob;

    public FirstReadyJob(final Date plannedTime, long duration) {
        super(plannedTime, duration, true);
        secondJob = null;
    }

    public void setSecond(final Job job) {
        if (secondJob != job && secondJob != null)
            secondJob.removeObserver(this);
        secondJob = job;
        if (secondJob != null)
            secondJob.addObserver(this);
    }

    /**
     * Called when a given job changes its progress.
     *
     * The first job is ready when a bound second job is not executing
     *
     * @param job      a given job, normally should be second job
     * @param progress job's progress
     */
    @Override
    public void progressChanged(Job job, int progress) {
        if (secondJob != job)
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
        changeReadyStatus(!secondJob.getReadyStatus());
    }
}
