package at.haha007.edenclient.utils.tasks;

import java.util.function.BooleanSupplier;

public class WaitingTask implements ITask {
    private final BooleanSupplier waitingFor;

    public WaitingTask(BooleanSupplier condition) {
        waitingFor = condition;
    }


    @Override
    public boolean run() {
        return waitingFor.getAsBoolean();
    }
}
