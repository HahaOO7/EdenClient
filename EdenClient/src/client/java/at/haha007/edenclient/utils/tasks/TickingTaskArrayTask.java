package at.haha007.edenclient.utils.tasks;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.utils.Scheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class TickingTaskArrayTask implements Task {
    private final Task[] tasks;
    private int index = 0;

    public TickingTaskArrayTask(Task[] tasks) {
        this.tasks = Arrays.copyOf(tasks, tasks.length);
    }

    @Override
    public void run() throws InterruptedException {
        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        //schedule all
        for (int i = 0; i < tasks.length; i++) {
            Task task = tasks[i];
            futures.add(EdenClient.getMod(Scheduler.class).callSyncDelayed(() -> {
                try {
                    task.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return 1;
            }, i));
        }

        for (CompletableFuture<Integer> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                for (CompletableFuture<Integer> cancel : futures) {
                    cancel.completeExceptionally(e);
                }
                throw new InterruptedException(e.getMessage());
            }
        }
    }
}
