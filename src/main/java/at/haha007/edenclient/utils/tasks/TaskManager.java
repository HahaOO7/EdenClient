package at.haha007.edenclient.utils.tasks;

import at.haha007.edenclient.utils.Scheduler;

import java.util.LinkedList;
import java.util.Queue;

public class TaskManager implements Cloneable {

    private final Queue<ITask> tasks = new LinkedList<>();
    private int maxIter;
    boolean started = false;

    public TaskManager(int maxFailedIter) {
        maxIter = maxFailedIter;
    }

    public TaskManager then(ITask task) {
        tasks.add(task);
        return this;
    }

    public void start() {
        if (started) return;
        started = true;
        if (!tick()) return;
        Scheduler.get().scheduleSyncRepeating(this::tick, 1, 1);
    }

    private boolean tick() {
        if (--maxIter < 0) {
            tasks.clear();
            return false;
        }
        while (!tasks.isEmpty() && tasks.peek().run()) {
            tasks.poll();
        }
        return !tasks.isEmpty();
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
        maxIter = -1;
    }
}
