package at.haha007.edenclient.mixin;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.callbacks.CommandSuggestionCallback;
import at.haha007.edenclient.callbacks.InventoryOpenCallback;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.ContainerInfo;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Shadow @Nullable
    public ClientLevel level;

    @Shadow public abstract boolean sendUnsignedCommand(String string);

    @Shadow private CommandDispatcher<SharedSuggestionProvider> commands;

    @Inject(method = "handleLogin", at = @At("RETURN"), cancellable = true)
    private void onLogin(ClientboundLoginPacket clientboundLoginPacket, CallbackInfo ci) {
        boolean connect = level != null;
        if (connect) {
            EdenClient.onJoin();
            JoinWorldCallback.EVENT.invoker().join();
        } else {
            LeaveWorldCallback.EVENT.invoker().leave();
            EdenClient.onQuit();
        }
    }

    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
    private void onSendCommand(String message, CallbackInfo ci) {
        if (!CommandManager.isClientSideCommand(message.split(" ")[0])) return;
        CommandManager.execute(message, new ClientSuggestionProvider(
                Minecraft.getInstance().getConnection(), Minecraft.getInstance()));
        ci.cancel();
    }

    @Inject(method = "sendUnsignedCommand", at = @At("HEAD"), cancellable = true)
    private void onSendUnsignedCommand(String message, CallbackInfoReturnable<Boolean> ci) {
        if (!CommandManager.isClientSideCommand(message.split(" ")[0])) return;
        CommandManager.execute(message, new ClientSuggestionProvider(
                Minecraft.getInstance().getConnection(), Minecraft.getInstance()));
        ci.setReturnValue(true);
    }

    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void onSendChat(String message, CallbackInfo ci) {
        if (!message.startsWith("/")) return;
        ci.cancel();
        sendUnsignedCommand(message.substring(1));
    }

    @Inject(method = "handleCommandSuggestions", at = @At("HEAD"))
    private void onSuggestCommands(ClientboundCommandSuggestionsPacket packet, CallbackInfo ci) {
        Suggestions sug = packet.toSuggestions();
        int id = packet.id();
        CommandSuggestionCallback.EVENT.invoker().suggest(sug, id);
    }

    @Inject(method = "handleCommands", at = @At("RETURN"))
    private void onCommandTree(ClientboundCommandsPacket packet, CallbackInfo info) {
        ecAddCommands();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstruct(Minecraft minecraft, Connection connection, CommonListenerCookie commonListenerCookie, CallbackInfo ci) {
        ecAddCommands();
    }

    @Inject(method = "handleContainerContent", at = @At("HEAD"))
    private void onInventoryContent(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        //get items, remove player items
        List<ItemStack> items = packet.getItems();
        items = items.subList(0, items.size() - 36);
        int id = packet.getContainerId();
        ContainerInfo containerInfo = ContainerInfo.update(id, items);
        if (containerInfo.isComplete()) InventoryOpenCallback.EVENT.invoker().open(containerInfo);
    }

    @Inject(method = "handleOpenScreen", at = @At("HEAD"))
    private void onInventoryOpen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        MenuType<?> type = packet.getType();
        int id = packet.getContainerId();
        Component title = packet.getTitle();
        ContainerInfo containerInfo = ContainerInfo.update(id, type, title);
        if (containerInfo.isComplete()) InventoryOpenCallback.EVENT.invoker().open(containerInfo);
    }

    @Inject(method = "handleSystemChat", at = @At("HEAD"), cancellable = true)
    void onGameMessage(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        ci.cancel();
        Minecraft.getInstance().getChatListener().handleSystemMessage(packet.content(), packet.overlay());
    }

    @Unique
    @SuppressWarnings("unchecked")
    private void ecAddCommands() {
        CommandManager.register((CommandDispatcher<ClientSuggestionProvider>) (Object) commands);
    }
}