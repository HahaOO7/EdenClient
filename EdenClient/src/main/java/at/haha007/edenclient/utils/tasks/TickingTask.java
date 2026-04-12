package at.haha007.edenclient.utils.tasks;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.utils.Scheduler;
import com.mojang.logging.LogUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BooleanSupplier;

/**
 * Runs a task each tick as long as the expression being run returns true
 */
public class TickingTask implements Task {
    private final BooleanSupplier supplier;
    private int index = 0;

    public TickingTask(BooleanSupplier supplier) {
        this.supplier = supplier;
    }

    @Override
    public void run() throws InterruptedException {
        Scheduler scheduler = EdenClient.getMod(Scheduler.class);
        CompletableFuture<Boolean> future = scheduler.callSyncDelayed(supplier::getAsBoolean, 1);
        try {
            while (Boolean.TRUE.equals(future.get())) {
                future = scheduler.callSyncDelayed(supplier::getAsBoolean, 1);
            }
        } catch (ExecutionException e) {
            LogUtils.getLogger().warn("ExecutionException while executing TickingTask! ", e);
        }
    }
}
