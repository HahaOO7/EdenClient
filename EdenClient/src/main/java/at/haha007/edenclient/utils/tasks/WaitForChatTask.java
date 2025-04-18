package at.haha007.edenclient.utils.tasks;

import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.callbacks.AddChatMessageCallback.ChatAddEvent;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import net.minecraft.network.chat.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class WaitForChatTask implements Task {

    private static final Set<WaitForChatTask> listeners = new HashSet<>();

    static {
        AddChatMessageCallback.EVENT.register(event -> listeners.removeIf(r -> r.onChatMessage(event)), WaitForChatTask.class);
        LeaveWorldCallback.EVENT.register(listeners::clear, WaitForChatTask.class);
    }

    private final Object lock = new Object();
    private final Function<ChatAddEvent, Boolean> matchFunction;
    private boolean started = false;


    public WaitForChatTask(Function<ChatAddEvent, Boolean> matchFunction) {
        this.matchFunction = matchFunction;
    }


    private boolean onChatMessage(ChatAddEvent event) {
        synchronized (lock) {
            boolean matches = matchFunction.apply(event);
            if (matches) {
                lock.notifyAll();
                return true;
            }
            return false;
        }
    }

    public void run() {
        synchronized (lock) {
            if (started) throw new IllegalStateException("Allready running!");
            started = true;
            try {
                listeners.add(this);
                lock.wait();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                listeners.remove(this);
            }
        }
    }
}
