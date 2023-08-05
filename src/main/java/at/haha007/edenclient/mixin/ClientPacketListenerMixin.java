package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.CommandSuggestionCallback;
import at.haha007.edenclient.callbacks.InventoryOpenCallback;
import at.haha007.edenclient.command.CommandManager;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Shadow
    private CommandDispatcher<SharedSuggestionProvider> commands;
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    public abstract void sendCommand(String string2);

    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
    private void onSendCommand(String message, CallbackInfo ci) {
        if (!CommandManager.isClientSideCommand(message.split(" ")[0]))
            return;
        CommandManager.execute(message, new ClientSuggestionProvider(Minecraft.getInstance().getConnection(), minecraft));
        ci.cancel();
    }

    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void onSendChat(String message, CallbackInfo ci) {
        if (!message.startsWith("/")) return;
        ci.cancel();
        sendCommand(message.substring(1));
    }


    @Inject(method = "handleCommandSuggestions", at=@At("HEAD"))
    private void onSuggestCommands(ClientboundCommandSuggestionsPacket packet, CallbackInfo ci){
        Suggestions sug = packet.getSuggestions();
        CommandSuggestionCallback.EVENT.invoker().suggest(sug);
    }

    @Inject(method = "handleCommands", at = @At("RETURN"))
    private void onCommandTree(ClientboundCommandsPacket packet, CallbackInfo info) {
        addCommands();
    }

    @SuppressWarnings("unchecked")
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstruct(Minecraft client, Screen screen, Connection connection, ServerData serverInfo, GameProfile profile, WorldSessionTelemetryManager worldSession, CallbackInfo ci) {
        addCommands();
        CommandManager.register((CommandDispatcher<ClientSuggestionProvider>) (Object) commands);
    }

    @Inject(method = "handleContainerContent", at = @At("HEAD"))
    private void onInventory(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        List<ItemStack> items = packet.getItems();
        items = items.subList(0, items.size() - 36);
        InventoryOpenCallback.EVENT.invoker().open(items);
    }

    @Inject(method = "handleSystemChat", at = @At("HEAD"), cancellable = true)
    void onGameMessage(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        ci.cancel();
        minecraft.getChatListener().handleSystemMessage(packet.content(), packet.overlay());
    }

    @Unique
    @SuppressWarnings("unchecked")
    private void addCommands() {
        CommandManager.register((CommandDispatcher<ClientSuggestionProvider>) (Object) commands);
    }
}
