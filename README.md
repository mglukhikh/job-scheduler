job-scheduler
=============

Architecture
------------

The developed project includes the following:

* a back end with Job and Scheduler interfaces, TimerScheduler as a scheduler implementation, AbstractJob skeleton plus some sample Job implementations: an one-shot job, a periodic job, a sequential (staged) job, a prime checker that depends on a prime calculator, a ping-pong pair of "First Ready" and "Second Ready" jobs
* some tests for the back end
* a front end with a toy server based on an HttpServer instance, and a toy environment that contains a scheduler and a set of jobs. The frond end uses JavaScript and jQuery for progress auto-refreshing
 
Back end
---------
 
The job interface includes three scheduling opportunities. Implementations can use them together, in this case they are joined by logical and.

* by a planned time, not earlier than a given moment
* by a set of required jobs, all of them must be completed before execution
* by the ready status which must be true before execution
 
Also, JobObserver interface is included to observe job progress and job ready status.

A scheduler can add and remove jobs. Also, it can give a set of currently scheduled jobs.

A given implementation of a scheduler is based on a timer to order planned times and on an executor to execute jobs itself. By default, a fixed thread pool is in use. Also, a given implementation observes job's progress and status itself. 

Given tests generally construct a scheduler and a number of jobs, then schedule jobs, then observe on their progress. Tests assume that PC is free enough to schedule everything in time.

Front end
--------

Front end is very straightforward. At the beginning, server constructs one periodic job and starts. HTML page contains a table with existing jobs, and input elements to construct a new one or delete an existing one. At this moment, periodic polling once per second is used to update progress and status of jobs.

To use this server, just run it. It listens port number 8080 instead of standard 80. Job status and progress are updates automatically. To create a new job, you should fill its name, choose its type, fill its start time and duration and press "New job". "Start time" field represents an interval between now and job start moment. The following types are supported by the server:
* One-Shot job -- just runs once and prints "Completed" in status field
* Periodic job -- runs periodically, first at start time, then after the same pause
* Sequential job -- job runs once but has ten progress stages, each with a given duration
* Prime calculator job -- job runs once and calculates all prime numbers up to a given limit
* Prime checker job -- job runs once and checks whether a given number is prime, it has prime calculator as a prerequisite so cannot run without running prime calculator first

Advancement
-----------

* Back-end: Check getReadyStatus() (DONE)
* Front-end: Refresh only job status / progress (DONE using JS / jQuery)
* Back-end: Fix a problem with a set of observers (DONE via CopyOnWriteArrayList)
* Back-end: Get rid of code duplications at job's beginning and end (DONE via beforeRun and afterRun) 
* Back-end: Refactor progress staff to get rid of constants like PROGRESS_PLANNED and to make flexible progress scale (DONE)
* Back-end: Think about observer's exceptions and suggest a workaround
* Tests: split on fragile and stable, make two different files
* Front-end: Try to get rid of periodic refreshing (probably can be done using WebSocket or Server-sent events)
* Back-end: more intelligent implementations for add / remove jobs for a scheduler

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

