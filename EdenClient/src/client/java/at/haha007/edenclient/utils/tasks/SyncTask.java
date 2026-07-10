package at.haha007.edenclient.utils.tasks;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.utils.Scheduler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class SyncTask implements Task {
    private final Task runnable;

    public SyncTask(Task runnable) {
        this.runnable = runnable;
    }

    public void run() throws InterruptedException {
        CompletableFuture<Integer> future = EdenClient.getMod(Scheduler.class).callSync(() -> {
            try {
                runnable.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return 1;
        });

        try {
            future.get();
        } catch (ExecutionException e) {
            future.completeExceptionally(e.getCause());
            throw new InterruptedException(e.getMessage());
        }
    }
}
