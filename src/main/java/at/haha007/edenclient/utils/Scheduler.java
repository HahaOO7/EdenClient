package at.haha007.edenclient.utils;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import net.minecraft.client.player.LocalPlayer;

public class Scheduler {

    private static final Scheduler instance = new Scheduler();

    private final Set<Runnable> sync = Collections.synchronizedSet(new HashSet<>());
    private final TreeMap<Long, Set<Runnable>> delayedSync = new TreeMap<>();
    private final TreeMap<Long, Set<RepeatingRunnable>> repeatingSync = new TreeMap<>();
    private long tick;

    public static Scheduler get() {
        return instance;
    }


    private static record RepeatingRunnable(int delta, BooleanSupplier runnable) {
    }

    private Scheduler() {
        if (EdenClient.INSTANCE == null)
            throw new ExceptionInInitializerError("Scheduler cant be called before initializing EdenClient");
        JoinWorldCallback.EVENT.register(this::cleanup);
        PlayerTickCallback.EVENT.register(this::tick);
    }

    private synchronized void cleanup() {
        sync.clear();
        delayedSync.clear();
        repeatingSync.clear();
        tick = 0;
        System.out.println("Scheduler tasks cleared");
    }

    private synchronized void tick(LocalPlayer clientPlayerEntity) {
        tick++;
        for (Runnable runnable : sync) {
            runnable.run();
        }
        sync.clear();

        while (delayedSync.size() > 0 && delayedSync.firstKey() <= tick) {
            delayedSync.pollFirstEntry().getValue().forEach(Runnable::run);
        }

        while (repeatingSync.size() > 0 && repeatingSync.firstKey() <= tick) {
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
        sync.add(runnable);
    }
}