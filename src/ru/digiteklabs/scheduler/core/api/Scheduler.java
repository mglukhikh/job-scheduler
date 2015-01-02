package ru.digiteklabs.scheduler.core.api;

import ru.digiteklabs.scheduler.job.api.Job;

import java.util.ConcurrentModificationException;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;

/**
 * Common interface for all schedulers.
 *
 * Scheduler is an object which is able to plan and execute jobs,
 * resolving its dependencies inside.
 *
 * After creation, scheduler normally contains no jobs but is ready to accept them.
 * By calling addJob(), a user registers a new job for scheduling.
 * Such a job must be run at least once before removing it from scheduling list.
 *
 * By calling removeJob(), a user manually removes a job from scheduling list.
 * Such a job should never be run again, at least if addJob() is not called again.
 *
 * @author Mikhail Glukhikh
 */
public interface Scheduler {

    /**
     * Register a new job for scheduling.
     *
     * The job should not be accepted for scheduling if a scheduler cannot run it ever.
     * It may happed if its planned time is null or its required job list includes non-scheduled jobs.
     *
     * @param job a new job
     * @return true if job is accepted for scheduling, false if this scheduler already has accepted the job.
     * @throws RejectedExecutionException if the job cannot be accepted for execution, particularly
     * if its planned time is null, its required job list is null or includes jobs that are not on scheduling list.
     */
    boolean addJob(Job job) throws RejectedExecutionException;

    /**
     * Unregister a job from scheduling.
     *
     * Normally should remove the job from scheduling list if it's present.
     * A specific situation occurs if the job runs now.
     * A scheduler can select to interrupt and remove it immediately,
     * or wait until it is completed and remove it after,
     * or just throw ConcurrentModificationException.
     *
     * Another specific situation occurs if the job is required for other jobs from
     * scheduling list. A scheduler can select to remove the job and all its dependents,
     * or just throw ConcurrentModificationException.
     *
     * @param job a job already accepted for scheduling.
     * @return true if job is successfully unregistered, false if job is not on scheduling list
     * @throws ConcurrentModificationException if the job is on scheduling list but cannot be removed
     * at this moment because of scheduling politics, e.g. if it runs now (not necessary) or if it is requires
     * for another job on scheduling list.
     */
    boolean removeJob(Job job) throws ConcurrentModificationException;

    /**
     * Gets information about all scheduled jobs.
     *
     * This method should return an unmodifiable set of all jobs this scheduler ever plans to run.
     * All jobs for which addJob() is successfully called should be in the result set
     * until they are run at least once or removeJob() is called. Then this scheduler may decide to remove them.
     * All jobs for which removeJob() is successfully called should not be in the result set
     * unless addJob() was called for them later.
     *
     * @return a set of jobs on the current scheduling list
     */
    Set<Job> getScheduledJobs();

    /**
     * Gets a progress of a given job.
     *
     * Normally should be called only for jobs on scheduling list using getScheduledJobs() first.
     * Then should return a progress of a job using its getProgress() method.
     * Otherwise must return Job.PROGRESS_NEVER.
     *
     * @param job a given job
     * @return integer-encoded progress
     */
    int getJobProgress(Job job);
}
