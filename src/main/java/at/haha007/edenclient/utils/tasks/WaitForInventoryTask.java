package at.haha007.edenclient.utils.tasks;

import at.haha007.edenclient.callbacks.InventoryOpenCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import net.minecraft.network.chat.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class WaitForInventoryTask implements Task {

    private static final Set<WaitForInventoryTask> listeners = new HashSet<>();

    static {
        InventoryOpenCallback.EVENT.register(i -> listeners.removeIf(r -> r.onInventoryOpen(i.getTitle())));
        LeaveWorldCallback.EVENT.register(listeners::clear);
    }

    private final Predicate<Component> matcher;
    private final Object lock = new Object();

    public WaitForInventoryTask(Predicate<Component> matcher) {
        this.matcher = matcher;
    }

    public WaitForInventoryTask() {
        this(c -> true);
    }

    public WaitForInventoryTask(Pattern pattern) {
        this.matcher = c -> pattern.matcher(c.getString()).matches();
    }

    private boolean onInventoryOpen(Component name) {
        synchronized (lock) {
            if (matcher.test(name)) {
                lock.notify();
                return true;
            }
            return false;
        }
    }

    public void run() throws InterruptedException {
        synchronized (lock) {
            listeners.add(this);
            lock.wait();
        }
    }
}
