package ru.digiteklabs.scheduler.core.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.digiteklabs.scheduler.core.api.Scheduler;
import ru.digiteklabs.scheduler.job.api.Job;
import ru.digiteklabs.scheduler.job.samples.OneShotJob;
import ru.digiteklabs.scheduler.job.samples.SequentialJob;

import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.*;

public class TimerSchedulerTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testAddVerySimpleJob() throws Exception {
        final Scheduler scheduler = new TimerScheduler();
        final OneShotJob job = new OneShotJob();
        assertFalse(job.isCompleted());
        assertTrue(job.getProgress() == Job.PROGRESS_PLANNED);
        scheduler.addJob(job);
        assertTrue(scheduler.getJobProgress(job) == Job.PROGRESS_PLANNED);
        Thread.sleep(10);
        assertTrue(job.getProgress() == Job.PROGRESS_FINISHED);
        assertTrue(scheduler.getScheduledJobs().isEmpty());
        assertTrue(job.isCompleted());
    }

    @Test
    public void testAddDelayedJob() throws Exception {
        final Scheduler scheduler = new TimerScheduler();
        // Job runs in a second after current moment
        final OneShotJob job = new OneShotJob(new Date(Calendar.getInstance().getTimeInMillis() + 1000));
        assertFalse(job.isCompleted());
        assertTrue(job.getProgress() == Job.PROGRESS_PLANNED);
        scheduler.addJob(job);
        Thread.sleep(10);
        assertTrue(job.getProgress() == Job.PROGRESS_PLANNED);
        assertFalse(scheduler.getScheduledJobs().isEmpty());
        assertFalse(job.isCompleted());
        Thread.sleep(1500);
        assertTrue(job.getProgress() == Job.PROGRESS_FINISHED);
        assertTrue(scheduler.getScheduledJobs().isEmpty());
        assertTrue(job.isCompleted());
    }

    @Test
    public void testAddLongJob() throws Exception {
        final Scheduler scheduler = new TimerScheduler();
        final OneShotJob job = new OneShotJob(1000);
        assertFalse(job.isCompleted());
        assertTrue(job.getProgress() == Job.PROGRESS_PLANNED);
        scheduler.addJob(job);
        Thread.sleep(10);
        assertTrue(job.getProgress() == Job.PROGRESS_STARTED);
        assertTrue(scheduler.getJobProgress(job) == Job.PROGRESS_STARTED);
        assertFalse(scheduler.getScheduledJobs().isEmpty());
        assertFalse(job.isCompleted());
        Thread.sleep(1500);
        assertTrue(job.getProgress() == Job.PROGRESS_FINISHED);
        assertTrue(scheduler.getScheduledJobs().isEmpty());
        assertTrue(job.isCompleted());
    }

    @Test
    public void testTwoJobs() throws Exception {
        final Scheduler scheduler = new TimerScheduler();
        final OneShotJob job1 = new OneShotJob(new Date(Calendar.getInstance().getTimeInMillis() + 500));
        final OneShotJob job2 = new OneShotJob(new Date(Calendar.getInstance().getTimeInMillis() + 1000));
        scheduler.addJob(job1);
        scheduler.addJob(job2);
        Thread.sleep(250);
        assertTrue(job1.getProgress() == Job.PROGRESS_PLANNED);
        assertTrue(scheduler.getJobProgress(job1) == Job.PROGRESS_PLANNED);
        assertFalse(job1.isCompleted());
        Thread.sleep(500);
        assertTrue(job1.getProgress() == Job.PROGRESS_FINISHED);
        assertTrue(job1.isCompleted());
        assertTrue(job2.getProgress() == Job.PROGRESS_PLANNED);
        assertFalse(job2.isCompleted());
        Thread.sleep(500);
        assertTrue(job2.getProgress() == Job.PROGRESS_FINISHED);
        assertTrue(job2.isCompleted());
    }

    @Test
    public void testRemoveJob() throws Exception {
        final Scheduler scheduler = new TimerScheduler();
        // Job runs in a second after current moment
        final OneShotJob job = new OneShotJob(new Date(Calendar.getInstance().getTimeInMillis() + 1000));
        assertFalse(job.isCompleted());
        assertTrue(job.getProgress() == Job.PROGRESS_PLANNED);
        scheduler.addJob(job);
        Thread.sleep(10);
        assertTrue(job.getProgress() == Job.PROGRESS_PLANNED);
        assertFalse(scheduler.getScheduledJobs().isEmpty());
        assertFalse(job.isCompleted());
        scheduler.removeJob(job);
        assertTrue(job.getProgress() == Job.PROGRESS_PLANNED);
        assertTrue(scheduler.getScheduledJobs().isEmpty());
        assertFalse(job.isCompleted());
    }

    @Test
    public void testSequentialJob() throws Exception {
        final Scheduler scheduler = new TimerScheduler();
        final SequentialJob job = new SequentialJob(10, new Date(Calendar.getInstance().getTimeInMillis() + 500), 100);
        assertTrue(job.getStage()==0);
        assertTrue(job.getProgress() == Job.PROGRESS_PLANNED);
        scheduler.addJob(job);
        Thread.sleep(750);
        assertTrue(job.getStage() == 3);
        Thread.sleep(500);
        assertTrue(job.getStage() == 8);
        Thread.sleep(500);
        assertTrue(job.getProgress() == Job.PROGRESS_FINISHED);
        assertTrue(scheduler.getScheduledJobs().isEmpty());
    }
}