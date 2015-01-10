job-scheduler
=============

Architecture
------------

The developed project includes the following:

* a front end with Job and Scheduler interfaces, TimerScheduler as a scheduler implementation, AbstractJob skeleton plus some sample Job implementations
* some tests for the front end
* a back end with a toy server based on an HttpServer instance, and a toy environment that contains a scheduler and a set of jobs
 
Front end
---------
 
The job interface includes three scheduling opportunities. Implementations can use them together, in this case they are joined by logical and.

* by a planned time, not earlier than a given moment
* by a set of required jobs, all of them must be completed before execution
* by the ready status which must be true before execution (actually it's a direction for some advancement, at this moment sample jobs do not use this ready status)
 
Also, JobObserver interface is included to observe job progress and job ready status.

A scheduler can add and remove jobs. Also, it can give a set of currently scheduled jobs.

A given implementation of a scheduler is based on a timer to order planned times and on an executor to execute jobs itself. By default, a fixed thread pool is in use. Also, a given implementation observes job's progress and status itself. 

Given tests generally construct a scheduler and a number of jobs, then schedule jobs, then observe on their progress. Tests assume that PC is free enough to schedule everything in time.

Back end
--------

Back end is very straightforward. At the beginning, server constructs one periodic job and starts. HTML page contains a table with existing jobs, and input elements to construct a new one or delete an existing one. At this moment, periodic polling is in use with Refresh button or auto-refresh once per minute.

Advancement
-----------

* Frond-end: Check getReadyStatus(), m.b. using producer-consumer
* Frond-end: Think about JobObserver and its implementation in TimerScheduler, m.b. split these things
* Back-end: Try to refresh only job status / progress
* Back-end: Try to get rid of periodic refreshing

Initiating letter
-----------------

Develop a library for executing jobs with dependencies in the background. There should be an APIs for scheduling jobs, setting up dependencies between jobs, notifying library users about job progress. Jobs can be one-time, or recurrent with flexible recurrence configuration. There should be an ability to fire a pre-scheduled job as a response to an event. The library should be ready to be used in long-running processes, like web- and application servers. The library should be capable of handling swarm of short jobs, as well as long-running jobs, with flexible control about schedules. Otherwise design features and capabilities as you see fit.

Please don’t use existing libraries like Quartz as a backend for your library. That would defeat the purpose.

Complete solution should include:

* brief design document about how it works, architecture and future evolution opportunities
* library source code and deployment artifacts ready to be pushed to maven
* fair amount of documentation
* tests
* sample web application running jobs and single page with online (live) job status, progress, etc.
 * implementing status page without refresh and periodic polling would be a plus
 * easily runnable application without setup is a must

Job is a code user wants to run asynchronously in the application, so it’s more like a Runnable with extra features. Of course, running a Job has some purpose, so there should be some kind of result. Whether a Job is like Future with a result, like Runnable that should put result somewhere, or like an Observable or Event, that passes result down the chain is up to your design decision.

We’d like to see a web page that demonstrates using a library in a server environment, yes. Not necessary very useful, it should present capabilities to start jobs, may be with parameters, tracking jobs’ progress and observing a result.

Scheduling options should ideally be pluggable/extensible and including basic set of useful schedulers, like one-time job, one-time job with an fire-off alarm, recurring job, triggered when other job has finished, etc. Combinable schedules would be nice, so that I can combine “at specific date/time” and “recurring” schedules to be able to code scenario like “trigger job ’Send best wishes for New Year for respective time zone’ on 31 Dec every year at 17:00 and repeat 8 times with 1 hour interval”.

General thoughts
----------------

We should have a front-end (core, library itself) and a back-end (some visualizer). 

The frond-end is based on the following things:

* Scheduler, which controls everything
 * It converts each registered Job to Runnable which first do some waiting and then execute the Job itself
 * It is based on some Executor, like fixedThreadPool or scheduledThreadPool
 * It may use Timer and TimerTask to do scheduling. Timer does support absolute timing but scheduledThreadPool does not.
 * Some synchronization must be included because scheduling methods are called from HTTP request threads. NB: Timer is thread safe
* Job, which contains some Runnable inside and some scheduling precondition like
 * Time, relative (seconds from now) or absolute (Date)
 * Required jobs to complete first, like Set<Job>
  * Scheduler should have some loop-control
 * Initiating Event
* Event, which can be generated by a Job and caught by Scheduler to start another Job
* ...

The back-end can be based on a Java Servlet and included in Glassfish or so. NB: as I know, both this methods will require installation. May be something simple should be implemented instead?

