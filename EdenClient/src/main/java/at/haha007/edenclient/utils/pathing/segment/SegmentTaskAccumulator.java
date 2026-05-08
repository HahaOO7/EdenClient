package at.haha007.edenclient.utils.pathing.segment;

import at.haha007.edenclient.utils.tasks.Task;

import java.util.ArrayDeque;
import java.util.Deque;

public class SegmentTaskAccumulator implements Task {
    private final Object lock = new Object();
    private final Deque<Task> tasks = new ArrayDeque<>();
    private boolean closed = false;

    @Override
    public void run() throws InterruptedException {
        while (true) {
            Task task;
            synchronized (lock) {
                while (tasks.isEmpty() && !closed) {
                    lock.wait();
                }
                if (tasks.isEmpty()) {
                    return;
                }
                task = tasks.removeFirst();
            }
            task.run();
        }
    }

    public void addSegment(PathSegment segment) {
        synchronized (lock) {
            if (closed) {
                throw new IllegalStateException("Cannot add segment after closing the accumulator");
            }
            tasks.addLast(segment.follower());
            lock.notifyAll();
        }
    }

    public void addClosingSegment() {
        synchronized (lock) {
            closed = true;
            lock.notifyAll();
        }
    }
}
