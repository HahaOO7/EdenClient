package at.haha007.edenclient.utils.tasks;

public class WaitForTicksTask implements Task {
    private int ticks;

    public WaitForTicksTask(int ticks) {
        this.ticks = ticks;
    }

    public boolean run() {
        return --ticks < 0;
    }
}
