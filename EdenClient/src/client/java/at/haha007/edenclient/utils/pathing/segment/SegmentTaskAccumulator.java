package at.haha007.edenclient.utils.pathing.segment;

import at.haha007.edenclient.utils.tasks.Task;
import com.mojang.logging.LogUtils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class SegmentTaskAccumulator implements Task {
    private final Object lock = new Object();
    private final Deque<PathSegment> tasks = new ArrayDeque<>();
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
                task = tasks.removeFirst().follower();
            }
            task.run();
        }
    }

    public PathSegment getScheduledPathSegments() {
        synchronized (lock) {
            List<PathSegment> children = List.copyOf(tasks);
            return children.size() > 2 ? new MasterPathSegment(children) : null;
        }
    }

    public void addSegment(PathSegment segment) {
        synchronized (lock) {
            if (closed) {
                throw new IllegalStateException("Cannot add segment after closing the accumulator");
            }
            if(!tasks.isEmpty()) {
                PathSegment last = tasks.peekLast();
                if(last.to().distanceTo(segment.from()) > 0.01) {
                    LogUtils.getLogger().warn("Adding a segment that does not start where the last one ended! This may cause issues in the path optimization. Please report this to the mod author.");
                }
            }
            tasks.addLast(segment);
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
