package at.haha007.edenclient.utils;

import at.haha007.edenclient.Mod;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Mod
public class Scheduler {

    private final Set<Runnable> sync = Collections.synchronizedSet(new HashSet<>());
    private final TreeMap<Long, Set<Runnable>> delayedSync = new TreeMap<>();
    private final TreeMap<Long, Set<RepeatingRunnable>> repeatingSync = new TreeMap<>();
    private long tick;
    private Thread mainThread = Thread.currentThread();

    private record RepeatingRunnable(int delta, BooleanSupplier runnable) {
    }

    private Scheduler() {
        LeaveWorldCallback.EVENT.register(this::cleanup);
        PlayerTickCallback.EVENT.register(this::tick);
    }

    private synchronized void cleanup() {
        sync.clear();
        delayedSync.clear();
        repeatingSync.clear();
        tick = 0;
        StringUtils.getLogger().info("Scheduler tasks cleared");
    }

    private synchronized void tick(LocalPlayer clientPlayerEntity) {
        if (mainThread != Thread.currentThread()) {
            mainThread = Thread.currentThread();
        }
        tick++;

        for (Runnable runnable : sync) {
            runnable.run();
        }
        sync.clear();

        while (!delayedSync.isEmpty() && delayedSync.firstKey() <= tick) {
            delayedSync.pollFirstEntry().getValue().forEach(Runnable::run);
        }

        while (!repeatingSync.isEmpty() && repeatingSync.firstKey() <= tick) {
            Map.Entry<Long, Set<RepeatingRunnable>> entry = repeatingSync.pollFirstEntry();
            entry.getValue().forEach(run -> {
                if (run.runnable().getAsBoolean()) {
                    Set<RepeatingRunnable> set = repeatingSync.computeIfAbsent(tick + run.delta(), (l) -> new HashSet<>());
                    set.add(run);
                }
            });
        }
    }

    //*************************
    //PUBLIC METHODS START HERE
    //*************************

    // stops running if return value is FALSE
    public synchronized void scheduleSyncRepeating(@NotNull BooleanSupplier runnable, int tickDelta, int startDelay) {
        if (tickDelta <= 0) throw new IllegalArgumentException("tickDelta has to be >= 1");
        Set<RepeatingRunnable> set = repeatingSync.computeIfAbsent(tick + startDelay, (l) -> new HashSet<>());
        set.add(new RepeatingRunnable(tickDelta, runnable));
    }

    public synchronized void scheduleSyncDelayed(@NotNull Runnable runnable, int delay) {
        if (delay <= 0) throw new IllegalArgumentException("tickDelta has to be >= 1");
        Set<Runnable> set = delayedSync.computeIfAbsent(delay + tick, k -> new HashSet<>());
        set.add(runnable);
    }

    public <T> CompletableFuture<T> callSync(Supplier<T> callable) {
        //on the main thread, return a completed future
        if (isMainThreadActive()) {
            T value = callable.get();
            return CompletableFuture.completedFuture(value);
        }
        //async thread, complete future on next tick
        CompletableFuture<T> future = new CompletableFuture<>();
        Runnable runnable = () -> future.complete(callable.get());
        runSync(runnable);
        return future;
    }

    public boolean cancelTask(Runnable runnable) {
        AtomicBoolean found = new AtomicBoolean(false);
        delayedSync.values().forEach(c -> found.set(found.get() || c.remove(runnable)));
        return found.get();
    }

    public boolean cancelTask(BooleanSupplier runnable) {
        AtomicBoolean found = new AtomicBoolean(false);
        repeatingSync.values().forEach(c -> found.set(found.get() || c.removeIf(e -> e.runnable == runnable)));
        return found.get();
    }

    public void runAsync(@NotNull Runnable runnable) {
        new Thread(runnable).start();
    }

    public synchronized void runSync(@NotNull Runnable runnable) {
        if (isMainThreadActive())
            runnable.run();
        else
            sync.add(runnable);
    }

    private boolean isMainThreadActive() {
        return Thread.currentThread() == mainThread;
    }
}