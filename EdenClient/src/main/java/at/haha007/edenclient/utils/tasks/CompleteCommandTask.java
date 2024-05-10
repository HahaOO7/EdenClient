package at.haha007.edenclient.utils.tasks;

import at.haha007.edenclient.callbacks.CommandSuggestionCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class CompleteCommandTask implements Task {

    private static final Set<CompleteCommandTask> listeners = new HashSet<>();

    static {
        CommandSuggestionCallback.EVENT.register((s, i) -> listeners.removeIf(r -> r.onCommandSuggestion(s, i)), CompleteCommandTask.class);
        LeaveWorldCallback.EVENT.register(listeners::clear, CompleteCommandTask.class);
    }

    private final String command;
    private final Object lock = new Object();
    private final int id = new Random().nextInt(100, 10000);
    private List<String> suggestions;
    private boolean started = false;


    public CompleteCommandTask(String command) {
        this.command = "/" + command;
    }


    private boolean onCommandSuggestion(Suggestions suggestions, int id) {
        synchronized (lock) {
            if (id != this.id)
                return false;
            this.suggestions = suggestions.getList().stream().map(s -> s.apply(command)).toList();
            lock.notifyAll();
            return true;
        }
    }

    public void run() {
        synchronized (lock) {
            if (started) throw new IllegalStateException("Allready running!");
            started = true;
            try {
                listeners.add(this);
                var con = PlayerUtils.getPlayer().connection;
                //unsafe if the server connection is faster than your cpu!
                con.send(new ServerboundCommandSuggestionPacket(id, command));
                lock.wait();
            } catch (InterruptedException ignored) {
            }
        }
    }

    public List<String> getSuggestions() {
        if (suggestions != null) return suggestions;
        synchronized (lock) {
            try {
                lock.wait();
                return suggestions;
            } catch (InterruptedException ignored) {
                return List.of();
            }
        }
    }
}
