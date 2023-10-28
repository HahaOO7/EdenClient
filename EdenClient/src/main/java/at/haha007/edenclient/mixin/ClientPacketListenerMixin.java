package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.CommandSuggestionCallback;
import at.haha007.edenclient.callbacks.InventoryOpenCallback;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.ContainerInfo;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
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
    @Unique
    private Minecraft minecraft;

    @Shadow
    public abstract void sendCommand(String string2);

    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
    private void onSendCommand(String message, CallbackInfo ci) {
        if (!CommandManager.isClientSideCommand(message.split(" ")[0]))
            return;
        CommandManager.execute(message, new ClientSuggestionProvider(minecraft.getConnection(), minecraft));
        ci.cancel();
    }

    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void onSendChat(String message, CallbackInfo ci) {
        if (!message.startsWith("/")) return;
        ci.cancel();
        sendCommand(message.substring(1));
    }

    @Inject(method = "handleCommandSuggestions", at = @At("HEAD"))
    private void onSuggestCommands(ClientboundCommandSuggestionsPacket packet, CallbackInfo ci) {
        Suggestions sug = packet.getSuggestions();
        int id = packet.getId();
        CommandSuggestionCallback.EVENT.invoker().suggest(sug, id);
    }

    @Inject(method = "handleCommands", at = @At("RETURN"))
    private void onCommandTree(ClientboundCommandsPacket packet, CallbackInfo info) {
        addCommands();
    }

    @SuppressWarnings("unchecked")
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstruct(Minecraft minecraft, Connection connection, CommonListenerCookie commonListenerCookie, CallbackInfo ci) {
        this.minecraft = Minecraft.getInstance();
        addCommands();
        CommandManager.register((CommandDispatcher<ClientSuggestionProvider>) (Object) commands);
    }

    @Inject(method = "handleContainerContent", at = @At("HEAD"))
    private void onInventoryContent(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        //get items, remove player items
        List<ItemStack> items = packet.getItems();
        items = items.subList(0, items.size() - 36);
        int id = packet.getContainerId();
        ContainerInfo containerInfo = ContainerInfo.update(id, items);
        if (containerInfo.isComplete())
            InventoryOpenCallback.EVENT.invoker().open(containerInfo);
    }

    @Inject(method = "handleOpenScreen", at = @At("HEAD"))
    private void onInventoryOpen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        MenuType<?> type = packet.getType();
        int id = packet.getContainerId();
        Component title = packet.getTitle();
        ContainerInfo containerInfo = ContainerInfo.update(id, type, title);
        if (containerInfo.isComplete())
            InventoryOpenCallback.EVENT.invoker().open(containerInfo);
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
