package ru.digiteklabs.scheduler.core.impl;

import com.sun.istack.internal.NotNull;
import ru.digiteklabs.scheduler.core.api.Scheduler;
import ru.digiteklabs.scheduler.job.api.Job;
import ru.digiteklabs.scheduler.job.api.JobObserver;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * An implementation of a scheduler based on Timer usage
 */
public class TimerScheduler implements Scheduler, JobObserver {

    private static enum JobStatus {
        /**
         * Job should be run in the future because of its planned time.
         * Its JobTask is scheduled by the timer
         */
        FUTURE,
        /**
         * Job should be run but it is either not ready or requires some other jobs.
         * Its planned time is already in the past.
         * Its JobTask is already executed by the timer
         */
        NOT_READY,
        /**
         * Job is running now by executor
         */
        RUN,
        /**
         * Job is finished. May be it will be run again in the future.
         */
        FINISHED
    }

    /**
     * An inner class which saves all information about a job inside TimerScheduler
     */
    private class JobTask extends TimerTask {

        /**
         * The job bound to this task
         */
        private final Job job;

        private int progress;

        private boolean ready;

        private JobStatus status;

        /**
         * A set of job successors that depend on the considered job
         */
        private final Set<Job> successors = Collections.synchronizedSet(new HashSet<Job>());

        JobTask(final Job job) {
            this.job = job;
            progress = Job.PROGRESS_PLANNED;
            ready = false;
            status = JobStatus.FUTURE;
        }

        int getProgress() {
            return progress;
        }

        void setProgress(int progress) {
            this.progress = progress;
        }

        void setReadyStatus(boolean ready) {
            this.ready = ready;
        }

        JobStatus getExecutionStatus() {
            return status;
        }

        void addSuccessor(Job successor) {
            successors.add(successor);
        }

        void removeSuccessor(Job successor) {
            successors.remove(successor);
        }

        boolean hasSuccessors() {
            return !successors.isEmpty();
        }

        void trySuccessorsExecution() {
            for (Job successor: successors) {
                final JobTask jt = jobTaskMap.get(successor);
                if (jt != null)
                    jt.tryExecution();
            }
        }

        void tryExecution() {
            if (status != JobStatus.NOT_READY)
                return;
            ready = job.getReadyStatus();
            if (!ready)
                return;
            for (Job required: job.getRequiredJobs()) {
                final JobTask jt = jobTaskMap.get(required);
                if (jt == null || jt.getExecutionStatus() != JobStatus.FINISHED)
                    return;
            }
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    status = JobStatus.RUN;
                    job.run();
                    status = JobStatus.FINISHED;
                    reschedule(job);
                }
            });
        }

        @Override
        public void run() {
            status = JobStatus.NOT_READY;
            tryExecution();
        }
    }

    //private final Calendar calendar = Calendar.getInstance();

    private final Timer timer = new Timer();

    private final Executor executor;

    private final Map<Job, JobTask> jobTaskMap = new ConcurrentHashMap<Job, JobTask>();

    protected TimerScheduler(@NotNull final Executor executor) {
        this.executor = executor;
    }

    public TimerScheduler(final int threadNumber) {
        this(Executors.newFixedThreadPool(threadNumber));
    }

    /**
     * Called when a given job changes its progress.
     *
     * Thread-unsafe because assumes that it is called only from job's thread
     *
     * @param job      a given job
     * @param progress job's progress
     */
    @Override
    public void progressChanged(Job job, int progress) {
        final JobTask jt = jobTaskMap.get(job);
        if (jt != null)
            jt.setProgress(progress);
    }

    /**
     * Called when a given job changes its ready status.
     *
     * Thread-unsafe. NB: think about who can call it.
     *
     * @param job   a given job
     * @param ready true if job is ready to run, false otherwise
     */
    @Override
    public void readyChanged(Job job, boolean ready) {
        final JobTask jt = jobTaskMap.get(job);
        if (jt != null)
            jt.setReadyStatus(ready);
    }

    /**
     * Register a new job for scheduling.
     * <p/>
     * The job should not be accepted for scheduling if a scheduler cannot run it ever.
     * It may happen if its planned time is null or its required job list includes non-scheduled jobs.
     *
     * @param job a new job
     * @return true if job is accepted for scheduling, false if this scheduler already has accepted the job.
     * @throws java.util.concurrent.RejectedExecutionException if the job cannot be accepted for execution, particularly
     * if its planned time is null, its required job list is null or includes jobs that are not on scheduling list.
     */
    @Override
    public boolean addJob(Job job) throws RejectedExecutionException {
        if (job.getPlannedTime()==null)
            throw new RejectedExecutionException("Scheduling not permitted because planned time is NEVER");
        if (jobTaskMap.get(job) != null)
            return false;
        if (!jobTaskMap.keySet().containsAll(job.getRequiredJobs()))
            throw new RejectedExecutionException("Scheduling not permitted because required jobs are not scheduled");
        for (Job required: job.getRequiredJobs()) {
            jobTaskMap.get(required).addSuccessor(job);
        }
        final JobTask jt = new JobTask(job);
        jobTaskMap.put(job, jt);
        //if (job.getPlannedTime().after(calendar.getTime()))
        // if planned time is in the past, task is scheduled for immediate execution
        timer.schedule(jt, job.getPlannedTime());
        return true;
    }

    /**
     * Unregister a job from scheduling.
     * <p/>
     * Normally should remove the job from scheduling list if it's present.
     * A specific situation occurs if the job runs now.
     * A scheduler can select to interrupt and remove it immediately,
     * or wait until it is completed and remove it after,
     * or just throw ConcurrentModificationException.
     * <p/>
     * Another specific situation occurs if the job is required for other jobs from
     * scheduling list. A scheduler can select to remove the job and all its dependents,
     * or just throw ConcurrentModificationException.
     *
     * @param job a job already accepted for scheduling.
     * @return true if job is successfully unregistered, false if job is not on scheduling list
     * @throws java.util.ConcurrentModificationException if the job is on scheduling list but cannot be removed
     * at this moment because of scheduling politics, e.g. if it runs now (not necessary) or if it is requires
     * for another job on scheduling list.
     */
    @Override
    public boolean removeJob(Job job) throws ConcurrentModificationException {
        final JobTask jt = jobTaskMap.get(job);
        if (jt == null)
            return false;
        if (jt.getExecutionStatus() == JobStatus.RUN)
            throw new ConcurrentModificationException("Unscheduling not permitted because job is running now");
        if (jt.hasSuccessors())
            throw new ConcurrentModificationException("Unscheduling not permitted because job is required by another scheduled job");
        jobTaskMap.remove(job);
        for (Job required: job.getRequiredJobs()) {
            jobTaskMap.get(required).removeSuccessor(job);
        }
        return true;
    }

    /**
     * Gets information about all scheduled jobs.
     * <p/>
     * This method should return an unmodifiable set of all jobs this scheduler ever plans to run.
     * All jobs for which addJob() is successfully called should be in the result set
     * until they are run at least once or removeJob() is called. Then this scheduler may decide to remove them.
     * All jobs for which removeJob() is successfully called should not be in the result set
     * unless addJob() was called for them later.
     *
     * @return a set of jobs on the current scheduling list
     */
    @Override
    public Set<Job> getScheduledJobs() {
        return Collections.unmodifiableSet(jobTaskMap.keySet());
    }

    /**
     * Gets a progress of a given job.
     * <p/>
     * Normally should be called only for jobs on scheduling list using getScheduledJobs() first.
     * Then should return a progress of a job using its getProgress() method.
     * Otherwise must return Job.PROGRESS_NEVER.
     *
     * @param job a given job
     * @return integer-encoded progress
     */
    @Override
    public int getJobProgress(Job job) {
        final JobTask jt = jobTaskMap.get(job);
        if (jt != null)
            return jt.getProgress();
        else
            return Job.PROGRESS_NEVER;
    }

    /**
     * An auxiliary method that is called when some job is finished.
     * It should reschedule a given job, and try to execute its successors.
     * @param job a just finished job
     */
    void reschedule(final Job job) {
        final JobTask jt = jobTaskMap.get(job);
        jt.trySuccessorsExecution();
        if (job.getPlannedTime() != null)
            timer.schedule(jt, job.getPlannedTime());
        else if (!jt.hasSuccessors())
            removeJob(job);
        // NB: who will remove this job if it has successors?
    }
}
