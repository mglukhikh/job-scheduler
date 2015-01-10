package ru.digiteklabs.scheduler.job.api;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * A job skeleton implementation which requires only run() method.
 * Ready status here is always true, but inherited classes may overwrite this method.
 * Observers are supported.
 *
 * This class is unconditionally thread-safe
 *
 * @author Mikhail Glukhikh
 */
public abstract class AbstractJob implements Job {

    // Conditionally thread-safe (except iterators)
    private final Set<JobObserver> observers = Collections.synchronizedSet(new HashSet<JobObserver>());

    // Reference and content are immutable
    private final Set<Job> requiredJobs;

    // Thread-safe
    private volatile Date plannedTime = Job.PLANNED_TIME_NEVER;

    // Thread-safe
    private volatile int progress = Job.PROGRESS_PLANNED;

    // Thread-safe
    private volatile boolean readyStatus = true;

    /**
     * A protected method for changing job's planned time.
     *
     * Should be called at the end of run() method.
     *
     * @param plannedTime a new planned time
     */
    protected final void changePlannedTime(final Date plannedTime) {
        this.plannedTime = plannedTime;
    }

    /**
     * A protected method for changing job's progress
     *
     * Should be called, at least, in the beginning of run() with PROGRESS_STARTED,
     * in the end of run() with PROGRESS_FINISHED or PROGRESS_PLANNED.
     *
     * @param progress a new job Sprogress
     */
    protected final void changeProgress(final int progress) {
        this.progress = progress;
        // Iterating through a synchronized set requires external synchronization
        synchronized(observers) {
            for (JobObserver observer : observers) {
                observer.progressChanged(this, progress);
            }
        }
    }

    /**
     * A protected method for changing job's ready status
     *
     * @param readyStatus a new job ready status
     */
    protected final void changeReadyStatus(final boolean readyStatus) {
        this.readyStatus = readyStatus;
        // Iterating through a synchronized set requires external synchronization
        synchronized(observers) {
            for (JobObserver observer : observers) {
                observer.readyChanged(this, readyStatus);
            }
        }
    }

    /**
     * Constructs a job which is planned by time.
     * @param plannedTime launch time
     */
    protected AbstractJob(final Date plannedTime) {
        this.plannedTime = plannedTime;
        requiredJobs = Collections.emptySet();
    }

    /**
     * Constructs a job which is planned by required jobs.
     * @param requiredJobs a set of required jobs to run this job
     */
    protected AbstractJob(final Set<Job> requiredJobs) {
        this.requiredJobs = Collections.unmodifiableSet(requiredJobs);
    }

    /**
     * Constructs a job which is planned both by time AND by required jobs.
     * @param plannedTime launch time, earliest possible
     * @param requiredJobs a set of required jobs to run this job
     */
    protected AbstractJob(final Date plannedTime, final Set<Job> requiredJobs) {
        this.plannedTime = plannedTime;
        this.requiredJobs = Collections.unmodifiableSet(requiredJobs);
    }

    /**
     * Gets information about prerequisite jobs.
     * <p/>
     * All jobs from the result set must be completed before this job may be run.
     * In case no jobs are required, the method should return the empty set.
     * The method may never return null. Also, the result set can never contain this job.
     * <p/>
     * The result set may not change until job is run.
     *
     * @return a set of jobs that are required to be completed before this job is able to be run
     */
    @Override
    public final Set<Job> getRequiredJobs() {
        return requiredJobs;
    }

    /**
     * Gets information about planned date and time of the next launch.
     * <p/>
     * Can be set with any precision but it's quite possible that a used scheduler does not allow
     * too high precision. A planned time can be in the future, in this case this job
     * may not be run until the given time point is reached.
     * It can be also in the past, in this case this job should be run when all required jobs
     * are completed and ready status is true.
     * It can be also null, and it means this job should never be launched again.
     * <p/>
     * After run() method is called, this job should change planned time.
     * Normally it should be changed to null if this job is one-time
     * or something in the future if this job is recurring.
     *
     * @return planned date and time of the next launch, or null if job should never be launched
     */
    @Override
    public final Date getPlannedTime() {
        return plannedTime;
    }

    /**
     * Gets information whether this job is ready to run or not.
     * <p/>
     * This method can return any value if planned time is not reached yet or
     * some required jobs are not completed yet. In both situations scheduler
     * should not use the result in any way.
     * <p/>
     * When planned time is reached and all required jobs are completed,
     * this method should return true if this job is ready to run or false otherwise.
     * A used scheduler can use some suggestions about a moment when job ready status may change,
     * or just poll status from time to time.
     *
     * @return true if job is ready to run and false otherwise
     */
    @Override
    public final boolean getReadyStatus() {
        return readyStatus;
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
        return true;
    }

    /**
     * Gets information about this job progress.
     * <p/>
     * It is an integer with the following meaning:<ul>
     * <li>PROGRESS_NEVER means this job should be never started</li>
     * <li>PROGRESS_PLANNED means this job is planned but not yet started</li>
     * <li>number from PROGRESS_STARTED to PROGRESS_FINISHED-1 means this job is started,
     * the more is the number, the more of this job is done</li>
     * <li>PROGRESS_FINISHED and more means this job is completed</li>
     * </ul>
     * <p/>
     * Normally, job should set its progress to negative number (PROGRESS_PLANNED) when created.
     * Immediately after its run() method is called, progress should be set to PROGRESS_STARTED and
     * then slowly increase to PROGRESS_FINISHED-1.
     * PROGRESS_FINISHED should be set by the last run() statement.
     * In case this job is recurring and should be run again, PROGRESS_PLANNED is set again instead.
     *
     * @return integer-encoded job progress
     */
    @Override
    public int getProgress() {
        return progress;
    }

    /**
     * Adds a new observer for this job's state.
     * <p/>
     * A job should pass information about progress change and ready status change to all its observers.
     * A job may not support observers at all, or support just one observer, or support a set of observers.
     * Also it's possible to have limitations on moments when observers are added. It's better to add
     * observers before registration in a scheduler.
     *
     * @param observer a new observer
     * @return true if observer is added successfully, false otherwise
     */
    @Override
    public boolean addObserver(JobObserver observer) {
        return observers.add(observer);
    }

    /**
     * Removes a new observer for this job's state.
     *
     * A job should pass information about progress change and ready status change to all its observers.
     * A job may not support observers at all, or support just one observer, or support a set of observers.
     * Also it's possible to have limitations on moments when observers are added or removed.
     * It's better to add / remove observers before registration in a scheduler.
     *
     * @param observer a observer to remove
     * @return true if observer is removed successfully, false otherwise
     */
    public boolean removeObserver(JobObserver observer) { return observers.remove(observer); }
}
