package com.zybooks.eventplanning_danpham;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * BackgroundRunner runs database work on one shared background thread and
 * sends each result back to the main thread. Both repositories use it, so
 * every database operation in the app runs one at a time, in order.
 */
public final class BackgroundRunner {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private BackgroundRunner() {
        // Prevent creating instances of this class
    }

    /**
     * Run work on the background thread, then send the result or error
     * back to the main thread through the callback
     */
    public static <T> void run(Callable<T> work, RepositoryCallback<T> callback) {
        EXECUTOR.execute(() -> {
            try {
                T result = work.call();
                MAIN_HANDLER.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                MAIN_HANDLER.post(() -> callback.onError(e));
            }
        });
    }
}
