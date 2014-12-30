job-scheduler
=============

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

