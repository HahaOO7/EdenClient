package at.haha007.edenclient.utils.tasks;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.utils.Scheduler;

import java.util.concurrent.atomic.AtomicBoolean;

public class MaxTimeTask implements Task {

    private final Task task;
    private final long millis;

    public MaxTimeTask(Task task, long millis) {
        this.task = task;
        this.millis = millis;
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
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        });
        task.run();
        done.set(true);
    }
}
