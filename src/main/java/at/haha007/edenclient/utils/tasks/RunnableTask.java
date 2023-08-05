package at.haha007.edenclient.utils.tasks;

public class RunnableTask implements Task {
    private final Runnable runnable;

    public RunnableTask(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public boolean run() {
        runnable.run();
        return true;
    }
}
