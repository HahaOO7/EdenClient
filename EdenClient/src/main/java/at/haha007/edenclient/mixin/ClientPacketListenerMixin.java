package at.haha007.edenclient.mixin;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.callbacks.*;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.ContainerInfo;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.Suggestions;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Shadow
    @Nullable
    private ClientLevel level;

    @Shadow
    private CommandDispatcher<SharedSuggestionProvider> commands;

    @Shadow
    public abstract void sendUnattendedCommand(String string, @Nullable Screen screen);

    @Inject(method = "handleLogin", at = @At("RETURN"))
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
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) return;
        FabricClientCommandSource suggestionsProvider = (FabricClientCommandSource) connection.getSuggestionsProvider();
        CommandManager.execute(message, suggestionsProvider);
        ci.cancel();
    }

    @Inject(method = "sendUnattendedCommand", at = @At("HEAD"))
    private void sendUnattendedCommand(String message, Screen screen, CallbackInfo ci) {
        if (!CommandManager.isClientSideCommand(message.split(" ")[0])) return;
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) return;
        FabricClientCommandSource suggestionsProvider = (FabricClientCommandSource) connection.getSuggestionsProvider();
        CommandManager.execute(message, suggestionsProvider);
    }

    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void onSendChat(String message, CallbackInfo ci) {
        if (!message.startsWith("/")) return;
        ci.cancel();
        sendUnattendedCommand(message.substring(1), null);
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
        List<ItemStack> items = packet.items();
        items = items.subList(0, items.size() - 36);
        int id = packet.containerId();
        ContainerInfo containerInfo = ContainerInfo.update(id, items);
        if (!containerInfo.isComplete()) return;
        InventoryOpenCallback.EVENT.invoker().open(containerInfo);
        ContainerInfo.remove(id);
    }

    @Inject(method = "updateLevelChunk", at = @At("RETURN"))
    private void onUpdateLevelChunk(int i, int j, ClientboundLevelChunkPacketData clientboundLevelChunkPacketData, CallbackInfo ci) {
        LevelChunk chunk = Objects.requireNonNull(this.level).getChunkSource().getChunkNow(i, j);
        UpdateLevelChunkCallback.EVENT.invoker().updateLevelChunk(chunk);
    }

    @Inject(method = "handleBlockDestruction", at = @At("RETURN"))
    private void onBlockDestruction(ClientboundBlockDestructionPacket packet, CallbackInfo ci) {
        BlockPos pos = packet.getPos();
        LevelChunk chunk = Objects.requireNonNull(this.level).getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        UpdateLevelChunkCallback.EVENT.invoker().updateLevelChunk(chunk);
    }

    @Inject(method = "handleOpenScreen", at = @At("HEAD"), cancellable = true)
    private void onInventoryOpen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        MenuType<?> type = packet.getType();
        int id = packet.getContainerId();
        Component title = packet.getTitle();

        ContainerInfo containerInfo = ContainerInfo.update(id, type, title);
        if (containerInfo.isComplete()) {
            InventoryOpenCallback.EVENT.invoker().open(containerInfo);
            ContainerInfo.remove(id);
        }

        // Check if any listener wants to cancel the container opening
        if (ContainerOpenCallback.EVENT.invoker().onContainerOpen(type, id, title)) {
            ci.cancel();
        }
    }

    @Inject(method = "handleSystemChat", at = @At("HEAD"), cancellable = true)
    void onGameMessage(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        ci.cancel();
        Minecraft.getInstance().getChatListener().handleSystemMessage(packet.content(), packet.overlay());
    }

    @Unique
    @SuppressWarnings("unchecked")
    private void ecAddCommands() {
        CommandManager.register((CommandDispatcher<FabricClientCommandSource>) (Object) commands);
    }
}