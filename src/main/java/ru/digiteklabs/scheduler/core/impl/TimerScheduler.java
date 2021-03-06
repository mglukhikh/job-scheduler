package ru.digiteklabs.scheduler.core.impl;

import org.jetbrains.annotations.NotNull;
import ru.digiteklabs.scheduler.core.api.Scheduler;
import ru.digiteklabs.scheduler.core.api.SchedulingException;
import ru.digiteklabs.scheduler.job.api.Job;
import ru.digiteklabs.scheduler.job.api.JobObserver;

import java.util.*;
import java.util.concurrent.*;

/**
 * An implementation of a scheduler based on Timer usage
 *
 * This class is thread safe.
 *
 * @author Mikhail Glukhikh
 */
public class TimerScheduler implements Scheduler, JobObserver {

    /**
     * Type describing job status, whether it's just planned, or on the run, or finished.
     */
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
     * An inner class which saves all information about a job inside TimerScheduler.
     *
     * This class is unconditionally thread-safe
     */
    private class JobTask extends TimerTask {

        /**
         * The job bound to this task, immutable
         */
        private final Job job;

        private volatile boolean ready;

        private volatile JobStatus status;

        /**
         * A set of job successors that depend on the considered job,
         * conditionally thread-safe except iterators
         */
        private final Set<Job> successors = Collections.synchronizedSet(new HashSet<Job>());

        JobTask(final Job job) {
            this.job = job;
            ready = false;
            status = JobStatus.FUTURE;
        }

        void setReadyStatus(boolean ready) {
            this.ready = ready;
            tryExecution();
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

        /**
         * Try to execute all job's successors.
         * A successor is a job which has this job as a prerequisite
         */
        void trySuccessorsExecution() {
            // Iteration requires external synchronization here
            synchronized(successors) {
                for (Job successor : successors) {
                    final JobTask jt = jobTaskMap.get(successor);
                    if (jt != null)
                        jt.tryExecution();
                }
            }
        }

        /**
         * Checks whether job is ready to run and run it if it's ready
         */
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
                    job.beforeRun();
                    job.run();
                    job.afterRun();
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

    /**
     * Timer is used for scheduling initial job checking when its planned time is reached.
     *
     * Timer class is thread-safe
     */
    private final Timer timer = new Timer();

    /**
     * Executor is used for execution of jobs run() methods
     *
     * Thread pool based implementations are thread safe
     */
    private final ExecutorService executor;

    /**
     * Binds Jobs to JobTasks with all auxiliary information about this job
     *
     * Concurrent hash map is thread safe. However, it includes some invariants that must be obeyed:<ul>
     *     <li>if job is inside than its required jobs are also inside</li>
     * </ul>
     */
    private final ConcurrentMap<Job, JobTask> jobTaskMap = new ConcurrentHashMap<Job, JobTask>();

    /**
     * Optional constructor based on a custom executor
     *
     * @param executor a base executor for the scheduler
     */
    protected TimerScheduler(@NotNull final ExecutorService executor) {
        this.executor = executor;
    }

    /**
     * Constructs a timer scheduler with a given thread number.
     *
     * The scheduler will be based on fixed thread pool
     *
     * @param threadNumber a necessary thread number
     */
    public TimerScheduler(final int threadNumber) {
        this(Executors.newFixedThreadPool(threadNumber));
    }

    /**
     * Constructs a timer scheduler with one thread
     *
     */
    public TimerScheduler() {
        this(1);
    }

    /**
     * Called when a given job changes its progress.
     *
     * @param job      a given job
     * @param progress job's progress
     */
    @Override
    public void progressChanged(Job job, int progress) {
        // DO NOTHING
    }

    /**
     * Called when a given job changes its ready status.
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
     * @throws SchedulingException if the job cannot be accepted for execution, particularly
     * if its planned time is null, its required job list is null or includes jobs that are not on scheduling list.
     */
    @Override
    public boolean addJob(Job job) throws SchedulingException {
        if (job.getPlannedTime()==Job.PLANNED_TIME_NEVER)
            throw new SchedulingException("Scheduling not permitted because planned time is NEVER");
        // Synchronizing to prevent any magic with jobTaskMap invariants
        synchronized(jobTaskMap) {
            if (jobTaskMap.get(job) != null)
                return false;
            if (!jobTaskMap.keySet().containsAll(job.getRequiredJobs()))
                throw new SchedulingException("Scheduling not permitted because required jobs are not scheduled");
            for (Job required : job.getRequiredJobs()) {
                jobTaskMap.get(required).addSuccessor(job);
            }
            job.addObserver(this);
            final JobTask jt = new JobTask(job);
            jobTaskMap.put(job, jt);
            //if (job.getPlannedTime().after(calendar.getTime()))
            // if planned time is in the past, task is scheduled for immediate execution
            timer.schedule(jt, job.getPlannedTime());
        }
        return true;
    }

    /**
     * Unregister a job from scheduling.
     * <p/>
     * Normally should remove the job from scheduling list if it's present.
     * A specific situation occurs if the job runs now.
     * A scheduler can select to interrupt and remove it immediately,
     * or wait until it is completed and remove it after,
     * or just throw SchedulingException.
     * <p/>
     * Another specific situation occurs if the job is required for other jobs from
     * scheduling list. A scheduler can select to remove the job and all its dependents,
     * or just throw SchedulingException.
     *
     * This implementation throws SchedulingException in both specific situations.
     *
     * @param job a job already accepted for scheduling.
     * @return true if job is successfully unregistered, false if job is not on scheduling list
     * @throws SchedulingException if the job is on scheduling list but cannot be removed
     * at this moment because of scheduling politics, e.g. if it runs now (not necessary) or if it is requires
     * for another job on scheduling list.
     */
    @Override
    public boolean removeJob(Job job) throws SchedulingException {
        // Synchronizing to prevent any magic with jobTaskMap invariants
        synchronized (jobTaskMap) {
            final JobTask jt = jobTaskMap.get(job);
            if (jt == null)
                return false;
            if (jt.getExecutionStatus() == JobStatus.RUN)
                throw new SchedulingException("Unscheduling not permitted because job is running now");
            if (jt.hasSuccessors())
                throw new SchedulingException("Unscheduling not permitted because job is required by another scheduled job");
            jt.cancel();
            job.removeObserver(this);
            jobTaskMap.remove(job);
            for (Job required : job.getRequiredJobs()) {
                final JobTask rt = jobTaskMap.get(required);
                // required must be inside because job was inside
                assert(rt != null);
                rt.removeSuccessor(job);
            }
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
     * An auxiliary method that is called when some job is finished.
     * It should reschedule a given job, and try to execute its successors.
     *
     * This implementations deletes job from the scheduling list if
     * it should not be run again AND has no successors.
     * Otherwise job must be deleted from the scheduling list manually.
     * 
     * @param job a just finished job
     */
    void reschedule(final Job job) {
        final JobTask jt = jobTaskMap.get(job);
        if (jt == null) {
            // Somebody has already removed this job, it's quite possible
            return;
        }
        jt.trySuccessorsExecution();
        jt.cancel();
        if (job.getPlannedTime() != Job.PLANNED_TIME_NEVER) {
            final JobTask next = new JobTask(job);
            if (jobTaskMap.replace(job, jt, next))
                timer.schedule(next, job.getPlannedTime());
        } else if (job.autoDeletedOnCompletion() && !jt.hasSuccessors()) {
            try {
                removeJob(job);
            } catch (SchedulingException ex) {
                // Should not occur
                throw new AssertionError("Cannot remove job in TimerScheduler.reschedule!");
            }
        }
        // NB: job with successors should be deleted manually
    }
}
