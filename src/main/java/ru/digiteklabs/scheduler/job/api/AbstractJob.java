package ru.digiteklabs.scheduler.job.api;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

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

    /**
     * Job which is not yet started or waits for the next start
     */
    static final int PROGRESS_NOT_STARTED = -1;
    /**
     * Job which has just started.
     */
    static final int PROGRESS_STARTED = 0;
    /**
     * Job was on the run, now it's finished.
     */
    static final int PROGRESS_FINISHED = 1000;

    // Unconditionally thread-safe
    private final List<JobObserver> observers = new CopyOnWriteArrayList<JobObserver>();

    // Reference and content are immutable
    private final Set<Job> requiredJobs;

    // Thread-safe
    private volatile Date plannedTime = Job.PLANNED_TIME_NEVER;

    // Thread-safe
    private volatile int progress = PROGRESS_NOT_STARTED;

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
     * @param progress a new job progress
     */
    protected final void changeProgress(final int progress) {
        this.progress = progress;
        // Now we have CopyOnWriteArrayList and do not require synchronization
        for (JobObserver observer : observers) {
            // TODO: what if exception happens?
            observer.progressChanged(this, progress);
        }
    }

    /**
     * A protected method for changing job's ready status
     *
     * @param readyStatus a new job ready status
     */
    protected final void changeReadyStatus(final boolean readyStatus) {
        this.readyStatus = readyStatus;
        // Now we have CopyOnWriteArrayList and do not require synchronization
        for (JobObserver observer : observers) {
            // TODO: what if exception happens?
            observer.readyChanged(this, readyStatus);
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
     *
     * This implementation sets AbstractJob.PROGRESS_NOT_STARTED during construction,
     * then sets AbstractJob.PROGRESS_STARTED in beforeRun() and AbstractJob.PROGRESS_FINISHED in afterRun().
     *
     * @return integer-encoded job progress
     */
    @Override
    public final int getProgress() {
        return progress;
    }

    /**
     * Gets a maximum possible value of a Job's progress
     *
     * @return a maximum possible value of a Job's progress
     */
    @Override
    public int getMaxProgress() {
        return PROGRESS_FINISHED;
    }

    /**
     * Checks whether this job was started.
     *
     * Job is considered as started if its progress is non-negative
     *
     * @return true if job is started, false otherwise
     */
    @Override
    public final boolean isStarted() {
        return progress >= 0;
    }

    /**
     * Checks whether this job was finished.
     *
     * Job is considered as finished if its progress is equals to its maximum value
     *
     * @return true if job is finished, false otherwise
     */
    @Override
    public final boolean isFinished() {
        return progress == getMaxProgress();
    }

    /**
     * A method that should be called before execution of a job's core.
     *
     * Changes job's progress to started
     *
     */
    public void beforeRun() {
        changeProgress(PROGRESS_STARTED);
    }

    /**
     * A method that should be called before execution of a job's core.
     *
     * Changes job's progress to getMaxProgress() and sets a new planned time to never.
     * A periodic job must modify this behaviour because a new planned time should be not never.
     */
    public void afterRun() {
        changeProgress(getMaxProgress());
        changePlannedTime(Job.PLANNED_TIME_NEVER);
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
