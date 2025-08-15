package at.haha007.edenclient.utils.tasks;

import at.haha007.edenclient.callbacks.InventoryOpenCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.utils.ContainerInfo;
import net.minecraft.network.chat.Component;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class WaitForInventoryTask implements Task {

    private static final Set<WaitForInventoryTask> listeners = new CopyOnWriteArraySet<>();

    static {
        InventoryOpenCallback.EVENT.register(i -> listeners.removeIf(r -> r.onInventoryOpen(i)), WaitForInventoryTask.class);
        LeaveWorldCallback.EVENT.register(() -> {
            listeners.forEach(WaitForInventoryTask::cancel);
            listeners.clear();
        }, WaitForInventoryTask.class);
    }

    private final Predicate<Component> matcher;
    private final Object lock = new Object();
    private final Consumer<ContainerInfo> listener;
    private ContainerInfo info = null;

    public WaitForInventoryTask(Pattern pattern, Consumer<ContainerInfo> listener) {
        this.listener = listener;
        this.matcher = c -> pattern.matcher(c.getString()).matches();
    }

    public WaitForInventoryTask(Pattern pattern) {
        this(pattern, i -> {
        });
    }

    private boolean onInventoryOpen(ContainerInfo containerInfo) {
        synchronized (lock) {
            if (matcher.test(containerInfo.getTitle())) {
                info = containerInfo;
                listener.accept(info);
                lock.notifyAll();
                return true;
            }
            return false;
        }
    }

    public void run() throws InterruptedException {
        synchronized (lock) {
            listeners.add(this);
            while (info == null) {
                lock.wait();
            }
        }
    }

    private void cancel(){
        synchronized (lock) {
            lock.notifyAll();
        }
    }
}
