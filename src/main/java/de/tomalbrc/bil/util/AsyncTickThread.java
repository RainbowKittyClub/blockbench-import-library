package de.tomalbrc.bil.util;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Worker of {@link de.tomalbrc.bil.BIL#EXECUTOR}, the pool that runs every holder's async tick.
 * Packets sent from one wait for the end-of-tick flush, as the server thread's do.
 */
public class AsyncTickThread extends ForkJoinWorkerThread {
    private static final AtomicInteger COUNT = new AtomicInteger();

    public AsyncTickThread(ForkJoinPool pool) {
        super(pool);
        this.setName("BIL Async Tick #" + COUNT.incrementAndGet());
    }

    public static boolean isCurrent() {
        return Thread.currentThread() instanceof AsyncTickThread;
    }

    /** Same shape as {@code Executors.newWorkStealingPool()}, with these workers. */
    public static ForkJoinPool newPool() {
        return new ForkJoinPool(Runtime.getRuntime().availableProcessors(), AsyncTickThread::new, null, true);
    }
}
