package ru.digiteklabs.scheduler.job.api;

import java.util.Date;
import java.util.Set;

/**
 * Common interface for all jobs.
 *
 * A job is always something runnable with additional prerequisites to be run.
 * This interface considers three possible prerequisites:<ul>
 *     <li>A set of required jobs.</li>
 *     <li>A planned launch date and time.</li>
 *     <li>A custom ready status.</li>
 * </ul>
 * A job may be run when all required jobs are completed AND launch date and time is reached
 * AND ready status is true. After that, a scheduler runs the job. Then the job may change
 * prerequisites if it is recurring. Otherwise it sets planned launch date and time to never (null).
 *
 * Also, a job includes a method to determine its run progress.
 * It changes from PROGRESS_PLANNED when job is just created to PROGRESS_STARTED when it is started
 * and PROGRESS_FINISHED when it is finished. A recurring job may then return to PROGRESS_PLANNED again.
 *
 * @author Mikhail Glukhikh
 */
public interface Job extends Runnable {

    /**
     * Gets information about prerequisite jobs.
     *
     * All jobs from the result set must be completed before this job may be run.
     * In case no jobs are required, the method should return the empty set.
     * The method may never return null. Also, the result set can never contain this job.
     *
     * The result set may not change until job is run.
     *
     * @return a set of jobs that are required to be completed before this job is able to be run
     */
    Set<Job> getRequiredJobs();

    static final Date PLANNED_TIME_NEVER = null;

    /**
     * Gets information about planned date and time of the next launch.
     *
     * Can be set with any precision but it's quite possible that a used scheduler does not allow
     * too high precision. A planned time can be in the future, in this case this job
     * may not be run until the given time point is reached.
     * It can be also in the past, in this case this job should be run when all required jobs
     * are completed and ready status is true.
     * It can be also null, and it means this job should never be launched again.
     *
     * After run() method is called, this job should change planned time.
     * Normally it should be changed to null if this job is one-time
     * or something in the future if this job is recurring.
     *
     * @return planned date and time of the next launch, or null if job should never be launched
     */
    Date getPlannedTime();

    /**
     * Gets information whether this job is ready to run or not.
     *
     * This method can return any value if planned time is not reached yet or
     * some required jobs are not completed yet. In both situations scheduler
     * should not use the result in any way.
     *
     * When planned time is reached and all required jobs are completed,
     * this method should return true if this job is ready to run or false otherwise.
     * A used scheduler can use some suggestions about a moment when job ready status may change,
     * or just poll status from time to time.
     *
     * @return true if job is ready to run and false otherwise
     */
    boolean getReadyStatus();

    /**
     * Gets information whether this job is deleted automatically from scheduling if completed
     *
     * Normally, this method should return true but for jobs that have successors
     * it's better to return false.
     *
     * @return true if it's allowed to delete job on completion, false otherwise
     */
    boolean autoDeletedOnCompletion();

    /**
     * Gets information about this job progress.
     *
     * It is an integer with the following meaning:<ul>
     *     <li>negative value means this job is not yet started</li>
     *     <li>non-negative number less than getMaxProgress() means this job is started and not finished,
     *     the more is the number, the more of this job is done</li>
     *     <li>same value as getMaxProgress() and more means this job is completed</li>
     * </ul>
     *
     * Normally, job should set its progress to negative number when created.
     * Immediately when beforeRun() method is called, progress should be set to 0 and
     * then slowly increase to getMaxProgress()-1 during run() execution.
     * The same value as getMaxProgress() should be set by afterRun() method.
     * In case this job is recurring and should be run again, negative value can be set again instead.
     * Leave progress as getMaxProgress() is also OK in this case.
     *
     * @return integer-encoded job progress
     */
    int getProgress();

    /**
     * Gets a maximum possible value of a Job's progress
     *
     * @return a maximum possible value of a Job's progress
     */
    int getMaxProgress();

    /**
     * Checks whether this job was started.
     *
     * Job is considered as started if its progress is non-negative
     *
     * @return true if job is started, false otherwise
     */
    boolean isStarted();

    /**
     * Checks whether this job was finished.
     *
     * Job is considered as finished if its progress is equals to its maximum value
     *
     * @return true if job is finished, false otherwise
     */
    boolean isFinished();

    /**
     * A method that should be called before execution of a job's core.
     *
     * Normally, it should at least change job's progress to started
     *
     */
    void beforeRun();

    /**
     * A method that should be called before execution of a job's core.
     *
     * Normally, it should at least change job's progress to finished and set a new planned time
     *
     */
    void afterRun();

    /**
     * Adds a new observer for this job's state.
     *
     * A job should pass information about progress change and ready status change to all its observers.
     * A job may not support observers at all, or support just one observer, or support a set of observers.
     * Also it's possible to have limitations on moments when observers are added. It's better to add
     * observers before registration in a scheduler.
     *
     * @param observer a new observer
     * @return true if observer is added successfully, false otherwise
     */
    boolean addObserver(JobObserver observer);

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
    boolean removeObserver(JobObserver observer);
}
