package com.aliashraf.androidrunnablejob;

import android.util.ArrayMap;
import android.util.Log;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class WorkerThreadPool {


    private static WorkerThreadPool singleInstance;
    ThreadPoolExecutor executor;
    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    private static int KEEP_ALIVE_TIME_OUT = 1;
    private LinkedBlockingQueue taskQueue;
    private static final String TAG = WorkerThreadPool.class.getName();
    private ArrayMap<Runnable, Boolean> mapOfTasks;

    private WorkerThreadPool() {
        taskQueue = new LinkedBlockingQueue();
        executor = new ThreadPoolExecutor(NUMBER_OF_CORES, NUMBER_OF_CORES, KEEP_ALIVE_TIME_OUT, TimeUnit.SECONDS, taskQueue);
    }

    static WorkerThreadPool obtain() {
        if (singleInstance == null) {
            synchronized (WorkerThreadPool.class) {
                singleInstance = new WorkerThreadPool();
            }
        }
        return singleInstance;
    }

    synchronized void submitTask(Runnable runnable, boolean shouldBeSingleJobExecutingAtAnyMoment) {
        Log.d(TAG, "submitTask() ::: called for "+runnable.getClass().toString());
        if(shouldBeSingleJobExecutingAtAnyMoment){
            Log.d(TAG, "submitTask() ::: this job shouldBeSingleJobExecutingAtAnyMoment ");
            if (mapOfTasks == null) {
                mapOfTasks = new ArrayMap<>(3);
            }
            if (mapOfTasks.containsKey(runnable)){
                Log.d(TAG, "submitTask() ::: job is already in queue or is currently executing, thus returning");
                return;
            }else{
                Log.d(TAG, "submitTask() ::: job will be added to set of tasks to be executed.");
                mapOfTasks.put(runnable, false);
                try {

                    for(int i = 0; i < mapOfTasks.size(); i++){
                        try {
                            if(mapOfTasks.valueAt(i) == false) {
                                Log.d(TAG, "submitTask() ::: iterating over set of task, now will execute " + runnable.getClass().toString());
                                mapOfTasks.put(runnable, true);
                                executor.execute(mapOfTasks.keyAt(i));
                            }else{
                                Log.d(TAG, "submitTask() ::: came inside for loop, "+runnable.getClass().toString()+ " job is already in queue or is currently executing.");
                            }
                        }catch (RejectedExecutionException e) {
                            Log.d(TAG, "submitTask() ::: RejectedExecutionException caught : " + e.toString());
                            if (executor.isTerminated()) {
                                Log.d(TAG, "submitTask() ::: Executor was terminated : reinitialising Executor and sumbiting the task again for : " + runnable.getClass().toString());
                                executor = new ThreadPoolExecutor(NUMBER_OF_CORES, NUMBER_OF_CORES, KEEP_ALIVE_TIME_OUT, TimeUnit.SECONDS, taskQueue);
                                mapOfTasks.put(runnable, false);
                                submitTask(runnable, true);
                            }
                        }
                    }
                } catch (ConcurrentModificationException e) {
                    mapOfTasks.put(runnable, false);
                    submitTask(runnable, true);
                }
            }
        }else{
            try {
                Log.d(TAG, "submitTask() ::: called and Runnable task will be submitted now for " + runnable.getClass().toString());
                executor.execute(runnable);
            }catch (RejectedExecutionException e) {
                Log.d(TAG, "submitTask() ::: RejectedExecutionException caught : " + e.toString());
                if (executor.isTerminated()) {
                    Log.d(TAG, "submitTask() ::: Executor was terminated : reinitialising Executor and sumbiting the task again for : " + runnable.getClass().toString());
                    executor = new ThreadPoolExecutor(NUMBER_OF_CORES, NUMBER_OF_CORES, KEEP_ALIVE_TIME_OUT, TimeUnit.SECONDS, taskQueue);
                    submitTask(runnable, false);
                }
            }
        }
    }

    synchronized void shutdownWorkerThreadPool() {
        Log.d(TAG,"shutdown() ::: called, executor will shutdown gracefully after previously submitted tasks are executed.");
        executor.shutdownNow();
    }

    synchronized List<Runnable> abort() {
        Log.d(TAG,"abort() ::: called, executor will shutdown now.");
        List<Runnable> listOfUnexecutedRunnables = executor.shutdownNow();
        return listOfUnexecutedRunnables;
    }

    synchronized void releaseTaskFromJobPool(Runnable taskAllocatedEarlier){
        if(mapOfTasks == null) {
            return ;
        }
        if(taskAllocatedEarlier == null){
            return ;
        }
        mapOfTasks.remove(taskAllocatedEarlier);
    }
}
