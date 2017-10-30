package com.jiandan.terence.realtimevideotcp;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class ExecutorManager {
    static ThreadPoolExecutor mExecutor;

    public static ThreadPoolExecutor getExecutor() {
        if (mExecutor == null || mExecutor.isShutdown()) {
            mExecutor = new ThreadPoolExecutor(5, 40, 10000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(50),
                    new RejectedExecutionHandler() {
                        @Override
                        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                          //  new Thread(r).start();
                        }
                    });
        }
        return mExecutor;
    }
}
