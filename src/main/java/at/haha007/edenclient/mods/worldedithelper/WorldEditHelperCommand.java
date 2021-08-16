package at.haha007.edenclient.mods.worldedithelper;

import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;

import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;


public class WorldEditHelperCommand {

    int delay = 0;
    LinkedList<String[]> undoCommandStack = new LinkedList<>();
    LinkedList<String[]> redoCommandStack = new LinkedList<>();

    public WorldEditHelperCommand() {
        registerCommand("ewe");
        registerCommand("edenwe");
        ConfigLoadCallback.EVENT.register(this::loadConfig);
        ConfigSaveCallback.EVENT.register(this::saveConfig);
    }

    private void registerCommand(String command) {
        LiteralArgumentBuilder<ClientCommandSource> node = literal(command);

        node.then(literal("replace").then(argument("from", StringArgumentType.word()).suggests(this::suggestValidBlocks).then(argument("to", StringArgumentType.word()).suggests(this::suggestValidBlocks)
                .executes(c -> {
                    Optional<Block> fromBlockOpt = Registry.BLOCK.getOrEmpty(new Identifier(c.getArgument("from", String.class)));
                    Optional<Block> toBlockOpt = Registry.BLOCK.getOrEmpty(new Identifier(c.getArgument("to", String.class)));

                    if (fromBlockOpt.isEmpty() || toBlockOpt.isEmpty()) {
                        sendModMessage("One of your block-inputs doesn't exist.");
                        return 0;
                    }

                    Block fromBlock = fromBlockOpt.get();
                    Block toBlock = toBlockOpt.get();

                    if (fromBlock.equals(toBlock)) {
                        sendModMessage("Both input-blocks can't be the same!");
                        return 0;
                    }

                    String[] currentOperation = new String[]{getBlockIDFromBlock(toBlock), getBlockIDFromBlock(fromBlock)};
                    undoCommandStack.addFirst(currentOperation);

                    return WorldEditHelperReplace.replaceCommandRequest(fromBlock, toBlock, delay, true);
                }))));

        node.then(literal("undo").executes(c -> {
            if (undoCommandStack.size() == 0) {
                sendModMessage("Nothing left to undo.");
                return 0;
            }

            ClientPlayerEntity entityPlayer = MinecraftClient.getInstance().player;
            if (entityPlayer == null) {
                sendModMessage("Error");
                return 1;
            }

            WorldEditHelperReplace.replaceUndoRequest(Registry.BLOCK.get(new Identifier(undoCommandStack.getFirst()[0])), Registry.BLOCK.get(new Identifier(undoCommandStack.getFirst()[1])), delay);
            redoCommandStack.addFirst(new String[]{undoCommandStack.getFirst()[1], undoCommandStack.getFirst()[0]});
            undoCommandStack.removeFirst();
            return 1;
        }));

        node.then(literal("redo").executes(c -> {
            if (redoCommandStack.size() == 0) {
                sendModMessage("Nothing left to redo.");
                return 0;
            }

            ClientPlayerEntity entityPlayer = MinecraftClient.getInstance().player;
            if (entityPlayer == null) {
                sendModMessage("Error");
                return 1;
            }

            WorldEditHelperReplace.replaceRedoRequest(Registry.BLOCK.get(new Identifier(redoCommandStack.getFirst()[0])), Registry.BLOCK.get(new Identifier(redoCommandStack.getFirst()[1])), delay);
            undoCommandStack.addFirst(new String[]{redoCommandStack.getFirst()[1], redoCommandStack.getFirst()[0]});
            redoCommandStack.removeFirst();
            return 1;
        }));

        node.then(literal("delay").then(argument("delay", IntegerArgumentType.integer(0, 40)).executes(c -> {
            this.delay = c.getArgument("delay", Integer.class);
            sendModMessage(new LiteralText("Set delay to ").formatted(Formatting.GOLD)
                    .append(new LiteralText(Integer.toString(delay)).formatted(Formatting.AQUA))
                    .append(new LiteralText(" ticks.").formatted(Formatting.GOLD)));
            return 1;
        })));

        node.then(literal("togglemessages").executes(c -> {

            ClientPlayerEntity entityPlayer = MinecraftClient.getInstance().player;
            if (entityPlayer == null) {
                sendModMessage("Error");
                return 1;
            }

            entityPlayer.sendChatMessage("/im predefined worldedit");

            return 1;
        }));

        register(node);
    }

    private CompletableFuture<Suggestions> suggestValidBlocks(CommandContext<ClientCommandSource> clientCommandSourceCommandContext, SuggestionsBuilder suggestionsBuilder) {
        DefaultedRegistry<Block> blockRegistry = Registry.BLOCK;
        blockRegistry.stream()
                .map(blockRegistry::getId)
                .map(Identifier::toString)
                .map(itemName -> itemName.split(":")[1])
                .map(String::toLowerCase).toList().forEach(suggestionsBuilder::suggest);
        return suggestionsBuilder.buildFuture();
    }

    private String getBlockIDFromBlock(Block block) {
        return block.getName().getString().replace(" ", "_").toLowerCase();
    }

    private void loadConfig(NbtCompound nbtCompound) {
        if (nbtCompound.contains("worldEditHelper")) {
            NbtCompound compound = nbtCompound.getCompound("worldEditHelper");
            delay = compound.getInt("delay");
        }
    }

    private void saveConfig(NbtCompound nbtCompound) {
        NbtCompound comp = new NbtCompound();
        comp.putInt("delay", delay);
        nbtCompound.put("worldEditHelper", comp);
    }
}
