package by.bsu.imageproc.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public final class Parallel {

    private static final int MIN_ROWS_PER_BAND = 16;
    private static final ExecutorService POOL = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "pixel-worker");
        thread.setDaemon(true);
        return thread;
    });

    private static volatile int threads = Runtime.getRuntime().availableProcessors();

    private Parallel() {
    }

    public interface RowBody {
        void run(int y);
    }

    public static int threads() {
        return threads;
    }

    public static void setThreads(int count) {
        threads = Math.max(1, Math.min(64, count));
    }

    public static void rows(int height, Progress progress, RowBody body) {
        int workers = threads;
        int bands = Math.max(1, Math.min(height / MIN_ROWS_PER_BAND, workers * 4));
        int bandSize = (height + bands - 1) / bands;
        AtomicInteger nextBand = new AtomicInteger();
        AtomicInteger doneRows = new AtomicInteger();
        Runnable worker = () -> {
            int band;
            while ((band = nextBand.getAndIncrement()) < bands) {
                int from = band * bandSize;
                int to = Math.min(height, from + bandSize);
                for (int y = from; y < to; y++) {
                    body.run(y);
                }
                progress.checkCancelled();
                progress.report((double) doneRows.addAndGet(to - from) / height);
            }
        };
        if (workers == 1 || bands == 1) {
            worker.run();
            return;
        }
        List<Future<?>> futures = new ArrayList<>(workers - 1);
        for (int i = 1; i < workers; i++) {
            futures.add(POOL.submit(worker));
        }
        RuntimeException failure = null;
        try {
            worker.run();
        } catch (RuntimeException e) {
            failure = e;
            progress.cancel();
        }
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            } catch (ExecutionException e) {
                if (failure == null && e.getCause() instanceof RuntimeException cause) {
                    failure = cause;
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
