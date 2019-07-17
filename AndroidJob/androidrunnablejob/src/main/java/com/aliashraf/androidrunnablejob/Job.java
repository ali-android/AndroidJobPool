package com.aliashraf.androidrunnablejob;

public abstract class Job implements Runnable {
    /** Implement job that should be done, by this class. This runs on "calling" thread. To run job(),
     * in a different Thread, (using ThreadPoolExecutor), consider calling runJobAsync(). */
    protected abstract void job();
    /**Override <b>"shouldBeSingleJobExecutingAtAnyMoment"</b> in concrete class to true,
     * if only 1 instance of this job should be running at any moment.
     * If this variable is set to True, make sure to <b>override equals() and hashCode()</b>.
     */
    protected boolean shouldBeSingleJobExecutingAtAnyMoment = false;

    /**Schedules job(), in another thread.*/
    final public void runJobAsync(){
        WorkerThreadPool.obtain().submitTask(this, shouldBeSingleJobExecutingAtAnyMoment);
    }

    @Override
    public void run() {
        job();
        if(shouldBeSingleJobExecutingAtAnyMoment) {
            WorkerThreadPool.obtain().releaseTaskFromJobPool(this);
        }
    }
}
