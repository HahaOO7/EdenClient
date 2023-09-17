package at.haha007.edenclient.utils.tasks;

import java.util.function.Supplier;

public interface Task {
    //return true if the task is finished
    void run() throws InterruptedException;

    default Task then(Task other) {
        return () -> {
            Task.this.run();
            other.run();
        };
    }

    default Task then(Task other, Supplier<Boolean> requirement) {
        return () -> {
            Task.this.run();
            if (!requirement.get()) return;
            other.run();
        };
    }
}
