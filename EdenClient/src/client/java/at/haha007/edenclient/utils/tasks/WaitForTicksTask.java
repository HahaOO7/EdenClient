package at.haha007.edenclient.utils.tasks;

import at.haha007.edenclient.callbacks.PlayerTickCallback;

public class WaitForTicksTask implements Task {

    static {
        PlayerTickCallback.EVENT.register(e -> tick(), WaitForTicksTask.class);
    }

    static long tick = 0;

    private static void tick() {
        tick++;
    }

    private long lastTick;

    public WaitForTicksTask(int ticks) {
        lastTick = ticks;
    }

    public void run() throws InterruptedException {
        lastTick += tick;
        while (lastTick > tick) {
            long estimation = (lastTick - tick) * 40;
            estimation = Math.max(estimation, 10);
            //noinspection BusyWait
            Thread.sleep(estimation);
        }
    }
}
