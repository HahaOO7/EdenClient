package at.haha007.edenclient.utils.tasks;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.utils.Scheduler;

import java.util.concurrent.atomic.AtomicBoolean;

public class MaxTimeTask implements Task {

    private final Task task;
    private final Task onTimeout;
    private final long millis;

    public MaxTimeTask(Task task, long millis) {
        this(task, millis, () -> {});
    }
    public MaxTimeTask(Task task, long millis, Task onTimeout) {
        this.task = task;
        this.millis = millis;
        this.onTimeout = onTimeout;
    }


    @Override
    public void run() throws InterruptedException {
        AtomicBoolean done = new AtomicBoolean(false);
        Thread thread = Thread.currentThread();
        EdenClient.getMod(Scheduler.class).runAsync(() -> {
            try {
                Thread.sleep(millis);
                if (!done.get()) {
                    thread.interrupt();
                    onTimeout.run();
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        });
        task.run();
        done.set(true);
    }
}
