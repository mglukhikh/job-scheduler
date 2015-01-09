package ru.digiteklabs.scheduler.web;

import ru.digiteklabs.scheduler.core.api.Scheduler;
import ru.digiteklabs.scheduler.core.api.SchedulingException;
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

    /**
     * Create an environment with a sample periodic job, duration 1s, period 10s
     */
    public ToyEnvironment() {
        final Job job = new PeriodicJob(calendar.getTime(), 1000, 10000);
        jobs.put("First", job);
        try {
            scheduler.addJob(job);
        } catch (SchedulingException ex) {
            // Should not occur
            throw new AssertionError("Cannot create a job in a toy environment constructor!");
        }
    }

    public boolean removeJob(final String name) throws SchedulingException {
        if (jobs.containsKey(name)) {
            final Job job = jobs.get(name);
            if (scheduler.removeJob(job)) {
                jobs.remove(name);
                return true;
            }
        }
        return false;
    }

    public boolean addJob(final String name, final Job job) throws SchedulingException {
        if (jobs.containsKey(name))
            return false;
        if (scheduler.addJob(job)) {
            jobs.put(name, job);
            return true;
        } else {
            return false;
        }
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

    static private int getProgressValue(final Job job) {
        final int progress = job.getProgress();
        return progress < 0 ? 0 : (progress > Job.PROGRESS_FINISHED ? Job.PROGRESS_FINISHED : progress);
    }

    public String toHtml() {
        final StringBuilder sb = new StringBuilder();
        sb.append("<form id=\"main\"><table><tr><th>Job</th><th>Status</th><th>Progress</th><th></th></tr>");
        for (Map.Entry<String, Job> entry: jobs.entrySet()) {
            sb.append("<tr><td>").append(entry.getKey()).append("</td><td>");
            sb.append(entry.getValue()).append("</td><td>");
            sb.append("<progress value=\"").append(getProgressValue(entry.getValue())).append("\"");
            sb.append(" max=\"").append(Job.PROGRESS_FINISHED).append("\"/></td><td>");
            sb.append("<button type=\"submit\" name=\"remove\" value=\"").append(entry.getKey());
            sb.append("\">Remove</button>");
        }
        sb.append("</table></form>");
        return sb.toString();
    }
}
