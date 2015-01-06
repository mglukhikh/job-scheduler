package ru.digiteklabs.scheduler.web;

import ru.digiteklabs.scheduler.core.api.Scheduler;
import ru.digiteklabs.scheduler.core.impl.TimerScheduler;
import ru.digiteklabs.scheduler.job.api.Job;
import ru.digiteklabs.scheduler.job.samples.PeriodicJob;

import java.util.*;

/**
 * An environment with a scheduler and an opportunity to add/remove/monitor jobs
 *
 * @author Mikhail Glukhikh
 */
public class ToyEnvironment {

    private final Scheduler scheduler = new TimerScheduler();

    private final Map<String, Job> jobs = new HashMap<String, Job>();

    private final Calendar calendar = Calendar.getInstance();

    public ToyEnvironment() {
        final Job job = new PeriodicJob(calendar.getTime(), 1000, 10000);
        jobs.put("First", job);
        scheduler.addJob(job);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Job> entry: jobs.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue());
            sb.append(" -> ").append(entry.getValue().getProgress()).append("/1000\n");
        }
        return sb.toString();
    }
}
