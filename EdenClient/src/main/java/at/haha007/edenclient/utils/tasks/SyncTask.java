package at.haha007.edenclient.utils.tasks;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.utils.Scheduler;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class SyncTask implements Task {
    private final Runnable runnable;

    public SyncTask(@NotNull Runnable runnable) {
        if (runnable == null) {
            throw new IllegalArgumentException("Runnable must not be null");
        }
        this.runnable = runnable;
    }

    public void run() throws InterruptedException {
        CompletableFuture<Integer> future = EdenClient.getMod(Scheduler.class).callSync(() -> {
            runnable.run();
            return 1;
        });

        try {
            future.get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
