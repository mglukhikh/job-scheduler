package ru.digiteklabs.scheduler.job.samples;

import ru.digiteklabs.scheduler.job.api.AbstractJob;
import ru.digiteklabs.scheduler.job.api.Job;

import java.util.Calendar;
import java.util.Date;

/**
 * A sample job class implementing a periodic job
 * with launch number recording
 *
 * @author Mikhail Glukhikh
 */
public class PeriodicJob extends AbstractJob {

    private final long duration, pause;

    private int launchNumber = 0;

    /**
     * Constructs a periodic job that will start at a given time and will run for a given duration,
     * then will wait for a given pause and run again, and so and so.
     * @param plannedTime a given start time
     * @param duration a given job duration in milliseconds
     * @param pause a given pause
     */
    public PeriodicJob(final Date plannedTime, long duration, long pause) {
        super(plannedTime);
        this.duration = duration;
        this.pause = pause;
    }

    /**
     *
     * @return a number of completed launches
     */
    public int getLaunchNumber() {
        return launchNumber;
    }

    @Override
    public void run() {
        changeProgress(Job.PROGRESS_STARTED);
        try {
            if (duration > 0)
                Thread.sleep(duration);
        } catch (InterruptedException e) {
            System.out.println("A periodic job is interrupted!");
        }
        launchNumber++;
        changeProgress(Job.PROGRESS_FINISHED);
        changePlannedTime(new Date(Calendar.getInstance().getTimeInMillis() + pause));
    }
}