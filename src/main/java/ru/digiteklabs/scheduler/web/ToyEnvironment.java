package ru.digiteklabs.scheduler.web;

import ru.digiteklabs.scheduler.core.api.Scheduler;
import ru.digiteklabs.scheduler.core.api.SchedulingException;
import ru.digiteklabs.scheduler.core.impl.TimerScheduler;
import ru.digiteklabs.scheduler.job.api.Job;
import ru.digiteklabs.scheduler.job.samples.PeriodicJob;
import ru.digiteklabs.scheduler.job.samples.PrimeCalcJob;

import java.util.*;

/**
 * An environment with a scheduler and an opportunity to add/remove/monitor jobs
 *
 * @author Mikhail Glukhikh
 */
public class ToyEnvironment {

    private final Scheduler scheduler;

    private final Map<String, Job> jobs = new HashMap<String, Job>();

    private final Calendar calendar = Calendar.getInstance();

    private PrimeCalcJob primeCalcJob = null;

    /**
     * Creates an environment with a sample periodic job, duration 1s, period 10s
     */
    public ToyEnvironment(final int threadNumber) {
        scheduler = new TimerScheduler(threadNumber);
        final Job job = new PeriodicJob(calendar.getTime(), 1000, 10000);
        jobs.put("First", job);
        try {
            scheduler.addJob(job);
        } catch (SchedulingException ex) {
            // Should not occur
            throw new AssertionError("Cannot create a job in a toy environment constructor!");
        }
    }

    /**
     * Removes a job with a given name from the environment
     * @param name a given job name
     * @return true if a job is successfully removed, false if name does not exist
     * @throws SchedulingException if name exists but scheduler cannot remove this job
     */
    public boolean removeJob(final String name) throws SchedulingException {
        if (jobs.containsKey(name)) {
            final Job job = jobs.get(name);
            if (!scheduler.getScheduledJobs().contains(job) || scheduler.removeJob(job)) {
                jobs.remove(name);
                if (job==primeCalcJob)
                    primeCalcJob = null;
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a new job with a given name to the environment
     * @param name a given job name
     * @param job a given job itself
     * @return true if a job is successfully added, false if name is duplicated or a second
     * prime calculator is to add
     * @throws SchedulingException if scheduler cannot add this job
     */
    public boolean addJob(final String name, final Job job) throws SchedulingException {
        if (jobs.containsKey(name))
            return false;
        if (job instanceof PrimeCalcJob && primeCalcJob != null)
            return false;
        if (scheduler.addJob(job)) {
            jobs.put(name, job);
            if (job instanceof PrimeCalcJob)
                primeCalcJob = (PrimeCalcJob)job;
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return a reference to the current prime calculator if exists, null otherwise
     */
    public PrimeCalcJob getCalcJob() {
        return primeCalcJob;
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

    /**
     *
     * @return description of the environment in HTML format
     */
    public String toHtml() {
        final StringBuilder sb = new StringBuilder();
        sb.append("<div id=\"env\"><form id=\"main\"><table><tr><th>Job</th><th>Status</th><th>Progress</th><th></th></tr>");
        for (Map.Entry<String, Job> entry: jobs.entrySet()) {
            sb.append("<tr><td>").append(entry.getKey()).append("</td><td>");
            sb.append(entry.getValue()).append("</td><td>");
            sb.append("<progress value=\"").append(getProgressValue(entry.getValue())).append("\"");
            sb.append(" max=\"").append(Job.PROGRESS_FINISHED).append("\"/></td><td>");
            sb.append("<button type=\"submit\" name=\"remove\" value=\"").append(entry.getKey());
            sb.append("\">Remove</button>");
        }
        sb.append("</table></form></div>");
        return sb.toString();
    }
}
