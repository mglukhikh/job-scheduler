package ru.digiteklabs.scheduler.job.samples;

import ru.digiteklabs.scheduler.job.api.AbstractJob;

import java.util.Calendar;
import java.util.Date;

/**
 * A sample job class implementing one-shot job
 * with completion flag
 *
 * @author Mikhail Glukhikh
 */
public class OneShotJob extends AbstractJob {

    private volatile boolean completed = false;

    private final long duration;

    /**
     * Constructs one-shot job that will start at a given time and will run for a given duration
     * @param plannedTime a given start time
     * @param duration a given job duration in milliseconds
     */
    public OneShotJob(final Date plannedTime, long duration) {
        super(plannedTime);
        this.duration = duration;
    }

    /**
     * Constructs one-shot job that will start at a given time and will be completed immediately
     * @param plannedTime a given start time
     */
    public OneShotJob(final Date plannedTime) {
        this(plannedTime, 0);
    }

    /**
     * Constructs one-shot job that will start immediately and will run for a given duration
     * @param duration a given job duration in milliseconds
     */
    public OneShotJob(long duration) {
        this(Calendar.getInstance().getTime(), duration);
    }

    /**
     * Constructs one-shot job that will start immediately and will be completed immediately
     */
    public OneShotJob() {
        this(0);
    }

    /**
     *
     * @return true if one-shot job is completed, false otherwise
     */
    public boolean isCompleted() {
        return completed;
    }

    @Override
    public void run() {
        try {
            if (duration > 0)
                Thread.sleep(duration);
        } catch (InterruptedException e) {
            System.out.println("An one-shot job is interrupted!");
        }
        completed = true;
    }

    @Override
    public String toString() {
        return "One shot: " + (completed ? "completed" : "not completed");
    }
}
