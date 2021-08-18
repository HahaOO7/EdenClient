package at.haha007.edenclient.utils;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BooleanSupplier;

public class Scheduler {

    private static final Scheduler instance = new Scheduler();

    private final Set<Runnable> sync = Collections.synchronizedSet(new HashSet<>());
    private final TreeMap<Long, Set<Runnable>> delayedSync = new TreeMap<>();
    private final TreeMap<Long, RepeatingRunnable> repeatingSync = new TreeMap<>();
    private long tick;

    public static Scheduler get() {
        return instance;
    }


    private static record RepeatingRunnable(int delta, BooleanSupplier runnable) {
    }

    private Scheduler() {
        if (EdenClient.INSTANCE == null)
            throw new ExceptionInInitializerError("Scheduler cant be called before initializing EdenClient");
        ConfigLoadCallback.EVENT.register(this::onConfigLoad);
        PlayerTickCallback.EVENT.register(this::tick);
    }

    private synchronized void onConfigLoad(NbtCompound nbtCompound) {
        sync.clear();
        delayedSync.clear();
        repeatingSync.clear();
        tick = 0;
        System.out.println("Scheduler tasks cleared");
    }

    private synchronized void tick(ClientPlayerEntity clientPlayerEntity) {
        tick++;
        for (Runnable runnable : sync) {
            runnable.run();
        }
        sync.clear();

        while (delayedSync.size() > 0 && delayedSync.firstKey() < tick) {
            delayedSync.pollFirstEntry().getValue().forEach(Runnable::run);
        }

        while (repeatingSync.size() > 0 && repeatingSync.firstKey() < tick) {
            RepeatingRunnable run = repeatingSync.pollFirstEntry().getValue();
            if (run.runnable().getAsBoolean()) {
                repeatingSync.put(tick + run.delta(), run);
            }
        }
    }

    //*************************
    //PUBLIC METHODS START HERE
    //*************************

    // stops running if return value is FALSE
    public synchronized void scheduleSyncRepeating(@NotNull BooleanSupplier runnable, int tickDelta, int startDelay) {
        if (tickDelta <= 0) throw new IllegalArgumentException("tickDelta has to be >= 1");
        repeatingSync.put(startDelay + tick, new RepeatingRunnable(tickDelta, runnable));
    }

    public synchronized void scheduleSyncDelayed(@NotNull Runnable runnable, int delay) {
        Set<Runnable> set = delayedSync.get(delay + tick);
        if (set == null) set = new HashSet<>();
        set.add(runnable);
        delayedSync.put(delay + tick, set);
    }

    public void runAsync(@NotNull Runnable runnable) {
        new Thread(runnable).start();
    }

    public synchronized void runSync(@NotNull Runnable runnable) {
        sync.add(runnable);
    }
}