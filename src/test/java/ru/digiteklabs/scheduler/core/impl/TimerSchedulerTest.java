package ru.digiteklabs.scheduler.core.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.digiteklabs.scheduler.core.api.Scheduler;
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

    /**
     * Creates one-shot job with immediate run and checks its progress
     * @throws Exception
     */
    @Test
    public void testAddVerySimpleJob() throws Exception {
        final Scheduler scheduler = new TimerScheduler();
        final OneShotJob job = new OneShotJob();
        assertFalse(job.isCompleted());
        assertFalse(job.isStarted());
        scheduler.addJob(job);
        Thread.sleep(10);
        assertTrue(job.isFinished());
        assertTrue(scheduler.getScheduledJobs().isEmpty());
        assertTrue(job.isCompleted());
    }

    /**
     * Creates one-shot job with delayed run and checks its progress
     * @throws Exception
     */
    @Test
    public void testAddDelayedJob() throws Exception {
        final Scheduler scheduler = new TimerScheduler();
        // Job runs in a second after current moment
        final OneShotJob job = new OneShotJob(new Date(Calendar.getInstance().getTimeInMillis() + 1000));
        assertFalse(job.isCompleted());
        assertFalse(job.isStarted());
        scheduler.addJob(job);
        Thread.sleep(10);
        assertFalse(job.isStarted());
        assertFalse(scheduler.getScheduledJobs().isEmpty());
        assertFalse(job.isCompleted());
        Thread.sleep(1500);
        assertTrue(job.isFinished());
        assertTrue(scheduler.getScheduledJobs().isEmpty());
        assertTrue(job.isCompleted());
    }

    /**
     * Creates long one-shot job and checks its progress
     * @throws Exception
     */
    @Test
    public void testAddLongJob() throws Exception {
        final Scheduler scheduler = new TimerScheduler();
        final OneShotJob job = new OneShotJob(1000);
        assertFalse(job.isCompleted());
        assertFalse(job.isStarted());
        scheduler.addJob(job);
        Thread.sleep(10);
        assertTrue(job.isStarted());
        assertFalse(scheduler.getScheduledJobs().isEmpty());
        assertFalse(job.isCompleted());
        Thread.sleep(1500);
        assertTrue(job.isFinished());
        assertTrue(scheduler.getScheduledJobs().isEmpty());
        assertTrue(job.isCompleted());
    }

    /**
     * Creates two one-shot jobs and checks their progress
     * @throws Exception
     */
    @Test
    public void testTwoJobs() throws Exception {
        final Scheduler scheduler = new TimerScheduler();
        final OneShotJob job1 = new OneShotJob(new Date(Calendar.getInstance().getTimeInMillis() + 500));
        final OneShotJob job2 = new OneShotJob(new Date(Calendar.getInstance().getTimeInMillis() + 1000));
        scheduler.addJob(job1);
        scheduler.addJob(job2);
        Thread.sleep(250);
        assertFalse(job1.isStarted());
        assertFalse(job1.isCompleted());
        Thread.sleep(500);
        assertTrue(job1.isFinished());
        assertTrue(job1.isCompleted());
        assertFalse(job2.isStarted());
        assertFalse(job2.isCompleted());
        Thread.sleep(500);
        assertTrue(job2.isFinished());
        assertTrue(job2.isCompleted());
    }

    /**
     * Creates one-shot job, schedules it and removes it before run.
     * @throws Exception
     */
    @Test
    public void testRemoveJob() throws Exception {
        final Scheduler scheduler = new TimerScheduler();
        // Job runs in a second after current moment
        final OneShotJob job = new OneShotJob(new Date(Calendar.getInstance().getTimeInMillis() + 1000));
        assertFalse(job.isCompleted());
        assertFalse(job.isStarted());
        scheduler.addJob(job);
        Thread.sleep(10);
        assertFalse(job.isStarted());
        assertFalse(scheduler.getScheduledJobs().isEmpty());
        assertFalse(job.isCompleted());
        scheduler.removeJob(job);
        assertFalse(job.isStarted());
        assertTrue(scheduler.getScheduledJobs().isEmpty());
        assertFalse(job.isCompleted());
    }

    /**
     * Creates a sequential job and checks its progress and stages.
     * @throws Exception
     */
    @Test
    public void testSequentialJob() throws Exception {
        final Scheduler scheduler = new TimerScheduler();
        final SequentialJob job = new SequentialJob(10, new Date(Calendar.getInstance().getTimeInMillis() + 500), 100);
        assertTrue(job.getStage()==0);
        assertFalse(job.isStarted());
        scheduler.addJob(job);
        Thread.sleep(750);
        assertTrue(job.getStage() == 3);
        Thread.sleep(500);
        assertTrue(job.getStage() == 8);
        Thread.sleep(500);
        assertTrue(job.isFinished());
        assertTrue(scheduler.getScheduledJobs().isEmpty());
    }

    /**
     * Creates a periodic job and runs it a few times
     * @throws Exception
     */
    @Test
    public void testPeriodicJob() throws Exception {
        final Scheduler scheduler = new TimerScheduler();
        final PeriodicJob job = new PeriodicJob(new Date(Calendar.getInstance().getTimeInMillis() + 500), 500, 500);
        assertTrue(job.getLaunchNumber() == 0);
        assertFalse(job.isStarted());
        scheduler.addJob(job);
        Thread.sleep(1250);
        assertTrue(job.getLaunchNumber() == 1);
        assertTrue(job.isFinished());
        Thread.sleep(2000);
        assertTrue(job.getLaunchNumber() == 3);
        assertTrue(job.isFinished());
        scheduler.removeJob(job);
        Thread.sleep(1000);
        assertTrue(job.getLaunchNumber() == 3);
    }

    /**
     * Creates a set of 50 periodic jobs and checks their status at different time moments
     * @throws Exception
     */
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

    /**
     * Creates and checks prime calculator
     * @throws Exception
     */
    @Test
    public void testPrimeCalcJob() throws Exception {
        final Scheduler scheduler = new TimerScheduler();
        final PrimeCalcJob job = new PrimeCalcJob(Calendar.getInstance().getTime(), 10);
        scheduler.addJob(job);
        Thread.sleep(100);
        assertTrue(job.isFinished());
        assertTrue(job.getPrimes().size() == 4);
    }

    /**
     * Creates and checks prime calculator together with prime checker
     * @throws Exception
     */
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
        assertFalse(checkJob.isStarted());
        assertFalse(checkJob2.isStarted());
        Thread.sleep(800);
        assertTrue(checkJob.isFinished());
        assertFalse(checkJob2.isStarted());
        assertSame(PrimeCheckJob.CheckResult.PRIME, checkJob.getResult());
        Thread.sleep(800);
        assertTrue(checkJob2.isFinished());
        assertSame(PrimeCheckJob.CheckResult.NOT_PRIME, checkJob2.getResult());
    }

    /**
     * Creates and checks a ping-pong pair of FirstReadyJob and SecondReadyJob
     * @throws Exception
     */
    @Test
    public void testFirstSecond() throws Exception {
        final Scheduler scheduler = new TimerScheduler();
        final FirstReadyJob first = new FirstReadyJob(new Date(Calendar.getInstance().getTimeInMillis() + 500), 200);
        final SecondReadyJob second = new SecondReadyJob(new Date(Calendar.getInstance().getTimeInMillis() + 500), 200);
        first.setSecond(second);
        second.setFirst(first);
        scheduler.addJob(first);
        scheduler.addJob(second);
        // 500 ms -- First -- Second -- First -- Second -- ...
        Thread.sleep(800);
        assertEquals(1, first.getLaunchNumber());
        assertEquals(1, second.getLaunchNumber());
        Thread.sleep(400);
        assertEquals(2, first.getLaunchNumber());
        assertEquals(2, second.getLaunchNumber());
    }
}