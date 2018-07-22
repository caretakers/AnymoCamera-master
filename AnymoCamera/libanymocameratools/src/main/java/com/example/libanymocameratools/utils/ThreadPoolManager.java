package com.example.libanymocameratools.utils;

/**
 * Created by Anymo on 2018/7/6.
 */

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 线程池管理
 */
public class ThreadPoolManager {

    private static final String TAG = "ThreadPoolManager";
    private static ThreadPoolManager mInstance;

    public static ThreadPoolManager getInstance() {
        if (mInstance == null) {
            synchronized (ThreadPoolManager.class) {
                mInstance = new ThreadPoolManager();
            }
        }
        return mInstance;
    }

    private int corePoolSize;//核心线程池的数量，同时能够执行的线程数量
    private int maximumPoolSize;//最大线程池数量，表示当缓冲队列满的时候能继续容纳的等待任务的数量
    private long keepAliveTime = 1;//存活时间
    private TimeUnit unit = TimeUnit.SECONDS;
    private ThreadPoolExecutor executor;

    private ThreadPoolManager() {
        /**
         * 给corePoolSize赋值：当前设备可用处理器核心数*2 + 1,能够让cpu的效率得到最大程度执行（有研究论证的）
         */
        corePoolSize = Runtime.getRuntime().availableProcessors() * 2 + 1;
        maximumPoolSize = corePoolSize; //虽然maximumPoolSize用不到，但是需要赋值，否则报错
        executor = new MyThreadPoolExecutor(
                corePoolSize, //当某个核心任务执行完毕，会依次从缓冲队列中取出等待任务
                maximumPoolSize, //5,先corePoolSize,然后new LinkedBlockingQueue<Runnable>(),然后maximumPoolSize,但是它的数量是包含了corePoolSize的
                keepAliveTime, //表示的是maximumPoolSize当中等待任务的存活时间
                unit,
                new LinkedBlockingQueue<Runnable>(), //缓冲队列，用于存放等待任务，Linked的先进先出
                Executors.defaultThreadFactory(), //创建线程的工厂
                new ThreadPoolExecutor.AbortPolicy() //用来对超出maximumPoolSize的任务的处理策略
        );
    }

    /**
     * 执行任务
     */
    public void execute(Runnable runnable) {
        if (runnable == null) return;

        executor.execute(runnable);
    }

    /**
     * 从线程池中移除任务
     */
    public void remove(Runnable runnable) {
        if (runnable == null) return;

        executor.remove(runnable);
    }

    /**
     * 关闭线程池
     */
    public void closeThreadPool(Runnable runnable) {
        if (runnable == null) return;

        executor.shutdown();
    }

    //创建Handler 把主线程的Looper加入
    private Handler handler = new Handler(Looper.getMainLooper());

    /**
     * 在主线程中执行任务
     *
     * @param task
     */
    public void runOnUIThread(Runnable task) {
        handler.post(task);
    }

    public class MyThreadPoolExecutor extends ThreadPoolExecutor {

        private boolean isPaused;
        private ReentrantLock pauseLock = new ReentrantLock();
        private Condition unpaused = pauseLock.newCondition();

        public MyThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        }

        public MyThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        }

        public MyThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
        }

        public MyThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            super.beforeExecute(t, r);
            Log.i(TAG , " beforeExecute()");
            pauseLock.lock();
            try {
                while (isPaused) unpaused.await();
            } catch (InterruptedException ie) {
                t.interrupt();
            } finally {
                pauseLock.unlock();
            }
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            Log.i(TAG , ": afterExecute()");
        }

        @Override
        protected void terminated() {
            super.terminated();
            Log.i(TAG , ": terminated()");
        }

        public void pause() {
            Log.i(TAG , " pause()");
            pauseLock.lock();
            try {
                isPaused = true;
            } finally {
                pauseLock.unlock();
            }
        }

        public void resume() {
            Log.i(TAG , " resume()");
            pauseLock.lock();
            try {
                isPaused = false;
                unpaused.signalAll();
            } finally {
                pauseLock.unlock();
            }
        }
    }
}
