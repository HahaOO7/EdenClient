package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.SignWidthCallback;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
public class SignWidthOverride {
    @ConfigSubscriber("false")
    private boolean enabled = true;

    public SignWidthOverride() {
        SignWidthCallback.EVENT.register(this::onSignWidth, getClass());
        PerWorldConfig.get().register(this, "signWidth");
        registerCommand();
    }

    private boolean onSignWidth(int textWith, int signWidth, boolean withReached) {
        if (!enabled) return withReached;
        return true;
    }

    private void registerCommand() {
        var node = literal("esignwith");
        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            sendModMessage((enabled ? "Enabled SignWidth Override." : "Disabled SignWidth Override."));
            return 1;
        }));

        register(node, "Toggle the sign width override. Lets you write longer sign lines.");
    }
}
