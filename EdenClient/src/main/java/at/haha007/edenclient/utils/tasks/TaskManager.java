
package at.haha007.edenclient.utils.tasks;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.utils.Scheduler;
import com.mojang.logging.LogUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;


public class TaskManager implements  Task {
    private static final Set<TaskManager> running = Collections.synchronizedSet(new HashSet<>());

    private final Queue<Task> tasks = new LinkedList<>();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private Thread thread = null;

    static {
        LeaveWorldCallback.EVENT.register(() -> {
            new HashSet<>(running).forEach(TaskManager::cancel);
            running.clear();
        }, TaskManager.class);

    }

    public TaskManager() {
    }

    @Override
    public TaskManager then(Task task) {
        tasks.add(task);
        return this;
    }

    public void run() throws InterruptedException {
        running.add(this);
        thread = Thread.currentThread();
        while (!tasks.isEmpty()) {
            tasks.poll().run();
        }
        running.remove(this);
    }

    public void start() {
        if (started.getAndSet(true)) return;
        EdenClient.getMod(Scheduler.class).runAsync(() -> {
            try {
                run();
            } catch (InterruptedException e) {
                LogUtils.getLogger().error("Task interrupted!",e);
                Thread.currentThread().interrupt();
            }
        });
    }

    public void cancel() {
        tasks.clear();
        if (thread != null)
            thread.interrupt();
        running.remove(this);
    }
}
