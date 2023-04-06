package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.InventoryOpenCallback;
import at.haha007.edenclient.command.CommandManager;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.telemetry.WorldSession;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Shadow
    private CommandDispatcher<CommandSource> commandDispatcher;
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "sendChatCommand", at = @At("HEAD"), cancellable = true)
    private void onSendCommand(String message, CallbackInfo ci) {
        if (!CommandManager.isClientSideCommand(message.split(" ")[0]))
            return;
        CommandManager.execute(message, new ClientCommandSource(MinecraftClient.getInstance().getNetworkHandler(), client));
        ci.cancel();
    }


    @Inject(method = "onCommandTree", at = @At("RETURN"))
    private void onCommandTree(CommandTreeS2CPacket packet, CallbackInfo info) {
        addCommands();
    }

    @SuppressWarnings("unchecked")
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstruct(MinecraftClient client, Screen screen, ClientConnection connection, ServerInfo serverInfo, GameProfile profile, WorldSession worldSession, CallbackInfo ci) {
        addCommands();
        CommandManager.register((CommandDispatcher<ClientCommandSource>) (Object) commandDispatcher);
    }

    @Inject(method = "onInventory", at = @At("HEAD"))
    private void onInventory(InventoryS2CPacket packet, CallbackInfo ci) {
        List<ItemStack> items = packet.getContents();
        items = items.subList(0, items.size() - 36);
        InventoryOpenCallback.EVENT.invoker().open(items);
    }

    @Inject(method = "onGameMessage", at = @At("HEAD"), cancellable = true)
    void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        ci.cancel();
        client.getMessageHandler().onGameMessage(packet.content(), packet.overlay());
    }

    @Inject(method = "onChatMessage", at = @At("HEAD"), cancellable = true)
    void onGameMessage(ChatMessageS2CPacket packet, CallbackInfo ci) {
        ci.cancel();
        client.getMessageHandler().onGameMessage(packet.unsignedContent(), false);
    }

    @Unique
    @SuppressWarnings("unchecked")
    private void addCommands() {
        CommandManager.register((CommandDispatcher<ClientCommandSource>) (Object) commandDispatcher);
    }
}
