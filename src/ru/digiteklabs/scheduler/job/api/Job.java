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

    static final int PROGRESS_NEVER = -1000;
    static final int PROGRESS_PLANNED = -1;
    static final int PROGRESS_STARTED = 0;
    static final int PROGRESS_FINISHED = 1000;

    /**
     * Gets information about this job progress.
     *
     * It is an integer with the following meaning:<ul>
     *     <li>PROGRESS_NEVER means this job should be never started</li>
     *     <li>PROGRESS_PLANNED means this job is planned but not yet started</li>
     *     <li>number from PROGRESS_STARTED to PROGRESS_FINISHED-1 means this job is started,
     *     the more is the number, the more of this job is done</li>
     *     <li>PROGRESS_FINISHED and more means this job is completed</li>
     * </ul>
     *
     * Normally, job should set its progress to negative number (PROGRESS_PLANNED) when created.
     * Immediately after its run() method is called, progress should be set to PROGRESS_STARTED and
     * then slowly increase to PROGRESS_FINISHED-1.
     * PROGRESS_FINISHED should be set by the last run() statement.
     * In case this job is recurring and should be run again, PROGRESS_PLANNED is set again instead.
     *
     * @return integer-encoded job progress
     */
    int getProgress();

}
