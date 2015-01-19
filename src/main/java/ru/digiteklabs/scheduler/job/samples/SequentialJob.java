package ru.digiteklabs.scheduler.job.samples;

import ru.digiteklabs.scheduler.job.api.AbstractJob;
import ru.digiteklabs.scheduler.job.api.Job;

import java.util.Date;

/**
 * A sample job class implementing a sequential job
 * with a number of stages
 *
 * @author Mikhail Glukhikh
 */
public class SequentialJob extends AbstractJob {

    private volatile int stage = 0;

    private final int stages;

    private final long stageDuration;

    /**
     * Constructs a sequential job that has a given number of stages, will start at a given time and will run for a given duration
     * @param stages a given number of stages
     * @param plannedTime a given start time
     * @param stageDuration a given job duration in milliseconds
     */
    public SequentialJob(int stages, final Date plannedTime, long stageDuration) {
        super(plannedTime);
        this.stages = stages;
        this.stageDuration = stageDuration;
    }

    /**
     *
     * @return a number of current stage
     */
    public int getStage() {
        return stage;
    }

    @Override
    public void run() {
        try {
            for (stage++; stage <= stages; stage++) {
                if (stageDuration > 0)
                    Thread.sleep(stageDuration);
                changeProgress(Job.PROGRESS_FINISHED * stage / stages);
            }
        } catch (InterruptedException e) {
            System.out.println("A sequential job is interrupted!");
        }
    }

    @Override
    public String toString() {
        return stage <= stages ? ("Sequential #" + stage) : "Sequential: completed";
    }
}
