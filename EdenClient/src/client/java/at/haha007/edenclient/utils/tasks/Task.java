package at.haha007.edenclient.utils.tasks;

import java.util.function.BooleanSupplier;

public interface Task {
    //return true if the task is finished
    void run() throws InterruptedException;

    default Task then(Task other) {
        Task self = this;
        return () -> {
            try {
                self.run();
                other.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
    }

    default Task then(Task other, BooleanSupplier requirement) {
        return () -> {
            Task.this.run();
            if (!requirement.getAsBoolean()) return;
            other.run();
        };
    }
}
