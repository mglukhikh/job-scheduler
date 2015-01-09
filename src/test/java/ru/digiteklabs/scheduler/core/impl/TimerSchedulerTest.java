package ru.digiteklabs.scheduler.core.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.digiteklabs.scheduler.core.api.Scheduler;
import ru.digiteklabs.scheduler.job.api.Job;
import ru.digiteklabs.scheduler.job.samples.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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

    @Test
    public void testPeriodicJob() throws Exception {
        final Scheduler scheduler = new TimerScheduler();
        final PeriodicJob job = new PeriodicJob(new Date(Calendar.getInstance().getTimeInMillis() + 500), 500, 500);
        assertTrue(job.getLaunchNumber() == 0);
        assertTrue(job.getProgress() == Job.PROGRESS_PLANNED);
        scheduler.addJob(job);
        Thread.sleep(1250);
        assertTrue(job.getLaunchNumber() == 1);
        assertTrue(job.getProgress() == Job.PROGRESS_FINISHED);
        Thread.sleep(2000);
        assertTrue(job.getLaunchNumber() == 3);
        assertTrue(job.getProgress() == Job.PROGRESS_FINISHED);
        scheduler.removeJob(job);
        Thread.sleep(1000);
        assertTrue(job.getLaunchNumber() == 3);
    }

    @Test
    public void testPeriodicJobSet() throws Exception {
        final Scheduler scheduler = new TimerScheduler();
        final List<PeriodicJob> jobs = new ArrayList<PeriodicJob>(50);
        for (int i=0; i<50; i++) {
            final PeriodicJob job = new PeriodicJob(new Date(Calendar.getInstance().getTimeInMillis() + i*100), 10, 990);
            jobs.add(job);
            scheduler.addJob(job);
        }
        Thread.sleep(1500);
        assertTrue(jobs.get(0).getLaunchNumber() == 2);
        assertTrue(jobs.get(4).getLaunchNumber() <= 2);
        assertTrue(jobs.get(6).getLaunchNumber() == 1);
        assertTrue(jobs.get(13).getLaunchNumber() <= 1);
        Thread.sleep(1500);
        assertTrue(jobs.get(2).getLaunchNumber() == 3);
        assertTrue(jobs.get(7).getLaunchNumber() <= 3);
        assertTrue(jobs.get(28).getLaunchNumber() <= 1);
        assertTrue(jobs.get(13).getLaunchNumber() == 2);
    }

    @Test
    public void testPrimeCalcJob() throws Exception {
        final Scheduler scheduler = new TimerScheduler();
        final PrimeCalcJob job = new PrimeCalcJob(Calendar.getInstance().getTime(), 10);
        scheduler.addJob(job);
        Thread.sleep(100);
        assertTrue(job.getProgress() == Job.PROGRESS_FINISHED);
        assertTrue(job.getPrimes().size() == 4);
    }

    @Test
    public void testPrimeCheckJob() throws Exception {
        final Scheduler scheduler = new TimerScheduler();
        final PrimeCalcJob job = new PrimeCalcJob(new Date(Calendar.getInstance().getTimeInMillis() + 500), 100);
        final PrimeCheckJob checkJob = new PrimeCheckJob(Calendar.getInstance().getTime(), job, 1009);
        final PrimeCheckJob checkJob2 = new PrimeCheckJob(new Date(Calendar.getInstance().getTimeInMillis() + 1500), job, 1003);
        scheduler.addJob(job);
        scheduler.addJob(checkJob);
        scheduler.addJob(checkJob2);
        Thread.sleep(200);
        assertTrue(checkJob.getProgress() == Job.PROGRESS_PLANNED);
        assertTrue(checkJob2.getProgress() == Job.PROGRESS_PLANNED);
        Thread.sleep(800);
        assertTrue(checkJob.getProgress() == Job.PROGRESS_FINISHED);
        assertTrue(checkJob2.getProgress() == Job.PROGRESS_PLANNED);
        assertSame(PrimeCheckJob.CheckResult.PRIME, checkJob.getResult());
        Thread.sleep(800);
        assertEquals(Job.PROGRESS_FINISHED, checkJob2.getProgress());
        assertSame(PrimeCheckJob.CheckResult.NOT_PRIME, checkJob2.getResult());
    }
}