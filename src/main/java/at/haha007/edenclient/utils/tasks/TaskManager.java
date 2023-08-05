package at.haha007.edenclient.utils.tasks;

import java.util.LinkedList;
import java.util.Queue;

import static at.haha007.edenclient.utils.Scheduler.scheduler;

public class TaskManager implements Cloneable {

    private final Queue<Task> tasks = new LinkedList<>();
    private int maxIter;
    private boolean started = false;

    public TaskManager(int maxFailedIter) {
        maxIter = maxFailedIter;
    }

    public TaskManager then(Task task) {
        tasks.add(task);
        return this;
    }

    public void run(){
        while(!tasks.isEmpty()){

        }
    }

    public void start() {
//        scheduler().runAsync(this::run);
        if (started) return;
        started = true;
        if (!tick()) return;
        scheduler().scheduleSyncRepeating(this::tick, 1, 1);
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
