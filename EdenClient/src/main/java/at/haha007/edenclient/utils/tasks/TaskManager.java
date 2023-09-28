package at.haha007.edenclient.utils.tasks;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.utils.Scheduler;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;


public class TaskManager implements Cloneable, Task {
    private static final Set<TaskManager> running = Collections.synchronizedSet(new HashSet<>());

    private final Queue<Task> tasks = new LinkedList<>();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private Thread thread = null;

    static {
        LeaveWorldCallback.EVENT.register(() -> {
            running.forEach(TaskManager::cancel);
            running.clear();
        });

    }

    public TaskManager() {
    }

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
        if (started.get()) return;
        started.set(true);
        EdenClient.getMod(Scheduler.class).runAsync(() -> {
            try {
                run();
            } catch (InterruptedException e) {
                System.err.println("Task interrupted!");
                //noinspection CallToPrintStackTrace
                e.printStackTrace();
            }
        });
    }

    @Override
    public TaskManager clone() {
        try {
            return (TaskManager) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public void cancel() {
        tasks.clear();
        if (thread != null)
            thread.interrupt();
        running.remove(this);
    }
}
