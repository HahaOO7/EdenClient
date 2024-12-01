package at.haha007.edenclient.mods;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.PluginSignature;
import at.haha007.edenclient.utils.Scheduler;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
public class WorldEditReplaceHelper {

    /*
    WorldEditHelper Command Manager starts here
    */

    @ConfigSubscriber("0")
    private int delay = 0;
    private final Deque<String[]> undoCommandStack = new LinkedList<>();
    private final Deque<String[]> redoCommandStack = new LinkedList<>();

    public WorldEditReplaceHelper() {
        registerCommand("eworldedithelper");
        registerCommand("edenwe");
        PerWorldConfig.get().register(this, "worldEditHelper");
    }

    private void registerCommand(String command) {
        LiteralArgumentBuilder<FabricClientCommandSource> node = literal(command);

        node.then(literal("replace").then(argument("from", StringArgumentType.word()).suggests(this::suggestValidBlocks).then(argument("to", StringArgumentType.word()).suggests(this::suggestValidBlocks)
                .executes(c -> {
                    Optional<Block> fromBlockOpt = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(c.getArgument("from", String.class)));
                    Optional<Block> toBlockOpt = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(c.getArgument("to", String.class)));

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
                    undoCommandStack.add(currentOperation);

                    return replaceCommandRequest(fromBlock, toBlock, delay, true);
                }))));

        node.then(literal("undo").executes(c -> {
            if (undoCommandStack.isEmpty()) {
                sendModMessage("Nothing left to undo.");
                return 0;
            }

            replaceUndoRequest(BuiltInRegistries.BLOCK.getValue(ResourceLocation.parse(undoCommandStack.peek()[0])),
                    BuiltInRegistries.BLOCK.getValue( ResourceLocation.parse(Objects.requireNonNull(undoCommandStack.peek())[1])), delay);
            redoCommandStack.add(new String[]{Objects.requireNonNull(undoCommandStack.peek())[1], Objects.requireNonNull(undoCommandStack.peek())[0]});
            undoCommandStack.pop();
            return 1;
        }));

        node.then(literal("redo").executes(c -> {
            if (redoCommandStack.isEmpty()) {
                sendModMessage("Nothing left to redo.");
                return 0;
            }

            replaceRedoRequest(BuiltInRegistries.BLOCK.getValue(ResourceLocation.parse(redoCommandStack.peek()[0])),
                    BuiltInRegistries.BLOCK.getValue( ResourceLocation.parse(Objects.requireNonNull(redoCommandStack.peek())[1])), delay);
            undoCommandStack.add(new String[]{Objects.requireNonNull(redoCommandStack.peek())[1], Objects.requireNonNull(redoCommandStack.peek())[0]});
            redoCommandStack.pop();
            return 1;
        }));

        node.then(literal("delay").then(argument("delay", IntegerArgumentType.integer(0, 40)).executes(c -> {
            this.delay = c.getArgument("delay", Integer.class);
            sendModMessage(Component.text("Set delay to ", NamedTextColor.GOLD)
                    .append(Component.text(delay, NamedTextColor.AQUA))
                    .append(Component.text(" ticks.", NamedTextColor.GOLD)));
            return 1;
        })));

        node.then(literal("togglemessages").executes(c -> {

            LocalPlayer entityPlayer = PlayerUtils.getPlayer();

            entityPlayer.connection.sendChat("/eignoremessage predefined worldedit");

            return 1;
        }));

        node.requires(c -> PluginSignature.WORLDEDIT.isPluginPresent());

        register(node,
                "The WorldEditReplaceHelper helps you replace blocks that have specific properties which normal WorldEdit doesn't take into consideration when replacing blocks.",
                "Blocks like stairs, slabs, panes, walls, trapdoors, etc. can be replaced by other blocks of their type with their properties (waterlogged, shape, direction, etc.) unaffected.");
    }

    private CompletableFuture<Suggestions> suggestValidBlocks(CommandContext<FabricClientCommandSource> clientCommandSourceCommandContext, SuggestionsBuilder suggestionsBuilder) {
        DefaultedRegistry<Block> blockRegistry = BuiltInRegistries.BLOCK;
        blockRegistry.stream()
                .map(blockRegistry::getKey)
                .map(ResourceLocation::toString)
                .map(itemName -> itemName.split(":")[1])
                .map(String::toLowerCase).toList().forEach(suggestionsBuilder::suggest);
        return suggestionsBuilder.buildFuture();
    }

    /*
    WorldEditHelper Replace Methods start here
     */

    private int replaceCommandRequest(Block fromBlock, Block toBlock, int delay, boolean sendMessage) {
        switch (fromBlock) {
            case StairBlock stairBlock when toBlock instanceof StairBlock ->
                    sendReplaceStairCommand(stairBlock, (StairBlock) toBlock, delay);
            case SlabBlock slabBlock when toBlock instanceof SlabBlock ->
                    sendReplaceSlabCommand(slabBlock, (SlabBlock) toBlock, delay);
            case TrapDoorBlock trapDoorBlock when toBlock instanceof TrapDoorBlock ->
                    sendReplaceTrapdoorBlockCommand(trapDoorBlock, (TrapDoorBlock) toBlock, delay);
            case DoorBlock doorBlock when toBlock instanceof DoorBlock ->
                    sendReplaceDoorBlockCommand(doorBlock, (DoorBlock) toBlock, delay);
            case StandingSignBlock standingSignBlock when toBlock instanceof StandingSignBlock ->
                    sendReplaceSignBlockCommand(standingSignBlock, (StandingSignBlock) toBlock, delay);
            case WallSignBlock wallSignBlock when toBlock instanceof WallSignBlock ->
                    sendReplaceWallSignBlockCommand(wallSignBlock, (WallSignBlock) toBlock, delay);
            case FenceBlock fenceBlock when toBlock instanceof FenceBlock ->
                    sendReplaceFenceBlockCommand(fenceBlock, (FenceBlock) toBlock, delay);
            case FenceGateBlock fenceGateBlock when toBlock instanceof FenceGateBlock ->
                    sendReplaceFenceGateBlockCommand(fenceGateBlock, (FenceGateBlock) toBlock, delay);
            case WallBlock wallBlock when toBlock instanceof WallBlock ->
                    sendReplaceWallBlockCommand(wallBlock, (WallBlock) toBlock, delay);
            case RotatedPillarBlock rotatedPillarBlock when toBlock instanceof RotatedPillarBlock ->
                    sendReplacePillarBlockCommand(rotatedPillarBlock, (RotatedPillarBlock) toBlock, delay);
            case LanternBlock lanternBlock when toBlock instanceof LanternBlock ->
                    sendReplaceLanternBlockCommand(lanternBlock, (LanternBlock) toBlock, delay);
            case HorizontalDirectionalBlock horizontalDirectionalBlock when toBlock instanceof HorizontalDirectionalBlock ->
                    sendReplaceHorizontalFacingBlockCommand(horizontalDirectionalBlock, (HorizontalDirectionalBlock) toBlock, delay);
            case null, default -> {
                sendModMessage("Can't replace these blocks with Eden-WE.");
                return 0;
            }
        }
        if (sendMessage)
            sendModMessage(Component.text("Replacing ", NamedTextColor.GOLD)
                    .append(Component.text(getBlockIDFromBlock(fromBlock), NamedTextColor.AQUA))
                    .append(Component.text(" with ", NamedTextColor.GOLD))
                    .append(Component.text(getBlockIDFromBlock(toBlock), NamedTextColor.AQUA)));
        return 1;
    }

    private void replaceUndoRequest(Block fromBlock, Block toBlock, int delay) {
        sendModMessage("Undoing last request");
        replaceCommandRequest(fromBlock, toBlock, delay, false);
    }

    private void replaceRedoRequest(Block fromBlock, Block toBlock, int delay) {
        sendModMessage("Redoing  last request.");

        replaceCommandRequest(fromBlock, toBlock, delay, false);
    }

    private void sendReplaceStairCommand(StairBlock fromBlock, StairBlock toBlock, int delay) {
        List<String> half = StairBlock.HALF.getPossibleValues().stream().map(Half::toString).toList();
        List<String> shape = StairBlock.SHAPE.getPossibleValues().stream().map(StairsShape::toString).toList();
        List<String> facing = StairBlock.FACING.getPossibleValues().stream().map(Direction::getName).toList();
        List<String> waterlogged = StairBlock.WATERLOGGED.getPossibleValues().stream().map(Object::toString).toList();

        List<List<String>> inputs = List.of(half, shape, facing, waterlogged);
        List<String> names = List.of("half", "shape", "facing", "waterlogged");

        sendAllReplacementCommandsForParameters(fromBlock, toBlock, inputs, names, delay);
    }

    private void sendReplaceSlabCommand(SlabBlock fromBlock, SlabBlock toBlock, int delay) {
        List<String> type = SlabBlock.TYPE.getPossibleValues().stream().map(SlabType::toString).toList();
        List<String> waterlogged = SlabBlock.WATERLOGGED.getPossibleValues().stream().map(Object::toString).toList();

        List<List<String>> inputs = List.of(type, waterlogged);
        List<String> names = List.of("type", "waterlogged");

        sendAllReplacementCommandsForParameters(fromBlock, toBlock, inputs, names, delay);
    }

    private void sendReplaceTrapdoorBlockCommand(TrapDoorBlock fromBlock, TrapDoorBlock toBlock, int delay) {
        List<String> open = TrapDoorBlock.OPEN.getPossibleValues().stream().map(Object::toString).toList();
        List<String> powered = TrapDoorBlock.POWERED.getPossibleValues().stream().map(Object::toString).toList();
        List<String> half = TrapDoorBlock.HALF.getPossibleValues().stream().map(Half::toString).toList();
        List<String> facing = TrapDoorBlock.FACING.getPossibleValues().stream().map(Direction::getName).toList();
        List<String> waterlogged = TrapDoorBlock.WATERLOGGED.getPossibleValues().stream().map(Object::toString).toList();

        List<List<String>> inputs = List.of(open, powered, half, facing, waterlogged);
        List<String> names = List.of("open", "powered", "half", "facing", "waterlogged");

        sendAllReplacementCommandsForParameters(fromBlock, toBlock, inputs, names, delay);
    }

    private void sendReplaceDoorBlockCommand(DoorBlock fromBlock, DoorBlock toBlock, int delay) {
        List<String> open = DoorBlock.OPEN.getPossibleValues().stream().map(Object::toString).toList();
        List<String> powered = DoorBlock.POWERED.getPossibleValues().stream().map(Object::toString).toList();
        List<String> half = DoorBlock.HALF.getPossibleValues().stream().map(DoubleBlockHalf::toString).toList();
        List<String> facing = DoorBlock.FACING.getPossibleValues().stream().map(Direction::getName).toList();
        List<String> hinge = DoorBlock.HINGE.getPossibleValues().stream().map(DoorHingeSide::toString).toList();

        List<List<String>> inputs = List.of(open, powered, half, facing, hinge);
        List<String> names = List.of("open", "powered", "half", "facing", "hinge");

        sendAllReplacementCommandsForParameters(fromBlock, toBlock, inputs, names, delay);
    }

    public void sendReplaceSignBlockCommand(StandingSignBlock fromBlock, StandingSignBlock toBlock, int delay) {
        List<String> rotation = StandingSignBlock.ROTATION.getPossibleValues().stream().map(v -> Integer.toString(v)).toList();
        List<String> waterlogged = StandingSignBlock.WATERLOGGED.getPossibleValues().stream().map(Object::toString).toList();

        List<List<String>> inputs = List.of(rotation, waterlogged);
        List<String> names = List.of("rotation", "waterlogged");

        sendAllReplacementCommandsForParameters(fromBlock, toBlock, inputs, names, delay);
    }

    public void sendReplaceWallSignBlockCommand(WallSignBlock fromBlock, WallSignBlock toBlock, int delay) {
        List<String> facing = WallSignBlock.FACING.getPossibleValues().stream().map(Direction::getName).toList();
        List<String> waterlogged = WallSignBlock.WATERLOGGED.getPossibleValues().stream().map(Object::toString).toList();

        List<List<String>> inputs = List.of(facing, waterlogged);
        List<String> names = List.of("facing", "waterlogged");

        sendAllReplacementCommandsForParameters(fromBlock, toBlock, inputs, names, delay);
    }

    private void sendReplaceFenceBlockCommand(FenceBlock fromBlock, FenceBlock toBlock, int delay) {
        List<String> north = FenceBlock.NORTH.getPossibleValues().stream().map(Object::toString).toList();
        List<String> east = FenceBlock.EAST.getPossibleValues().stream().map(Object::toString).toList();
        List<String> south = FenceBlock.SOUTH.getPossibleValues().stream().map(Object::toString).toList();
        List<String> west = FenceBlock.WEST.getPossibleValues().stream().map(Object::toString).toList();
        List<String> waterlogged = FenceBlock.WATERLOGGED.getPossibleValues().stream().map(Object::toString).toList();

        List<List<String>> inputs = List.of(north, east, south, west, waterlogged);
        List<String> names = List.of("north", "east", "south", "west", "waterlogged");

        sendAllReplacementCommandsForParameters(fromBlock, toBlock, inputs, names, delay);
    }

    private void sendReplaceFenceGateBlockCommand(FenceGateBlock fromBlock, FenceGateBlock toBlock, int delay) {
        List<String> facing = FenceGateBlock.FACING.getPossibleValues().stream().map(Direction::getName).toList();
        List<String> inWall = FenceGateBlock.IN_WALL.getPossibleValues().stream().map(Object::toString).toList();
        List<String> open = FenceGateBlock.OPEN.getPossibleValues().stream().map(Object::toString).toList();
        List<String> powered = FenceGateBlock.POWERED.getPossibleValues().stream().map(Object::toString).toList();

        List<List<String>> inputs = List.of(facing, inWall, open, powered);
        List<String> names = List.of("facing", "in_wall", "open", "powered");

        sendAllReplacementCommandsForParameters(fromBlock, toBlock, inputs, names, delay);
    }

    private void sendReplaceWallBlockCommand(WallBlock fromBlock, WallBlock toBlock, int delay) {
        List<String> north = WallBlock.NORTH_WALL.getPossibleValues().stream().map(WallSide::getSerializedName).toList();
        List<String> east = WallBlock.EAST_WALL.getPossibleValues().stream().map(WallSide::getSerializedName).toList();
        List<String> south = WallBlock.SOUTH_WALL.getPossibleValues().stream().map(WallSide::getSerializedName).toList();
        List<String> west = WallBlock.WEST_WALL.getPossibleValues().stream().map(WallSide::getSerializedName).toList();
        List<String> waterlogged = WallBlock.WATERLOGGED.getPossibleValues().stream().map(Object::toString).toList();
        List<String> up = WallBlock.UP.getPossibleValues().stream().map(Object::toString).toList();

        List<List<String>> inputs = List.of(north, east, south, west, waterlogged, up);
        List<String> names = List.of("north", "east", "south", "west", "waterlogged", "up");

        sendAllReplacementCommandsForParameters(fromBlock, toBlock, inputs, names, delay);
    }

    private void sendReplacePillarBlockCommand(RotatedPillarBlock fromBlock, RotatedPillarBlock toBlock, int delay) {
        List<String> axes = RotatedPillarBlock.AXIS.getPossibleValues().stream().map(Direction.Axis::toString).toList();

        List<List<String>> inputs = List.of(axes);
        List<String> names = new ArrayList<>(List.of("axis"));

        sendAllReplacementCommandsForParameters(fromBlock, toBlock, inputs, names, delay);
    }


    private void sendReplaceLanternBlockCommand(LanternBlock fromBlock, LanternBlock toBlock, int delay) {
        List<String> hanging = LanternBlock.HANGING.getPossibleValues().stream().map(Object::toString).toList();
        List<String> waterlogged = LanternBlock.WATERLOGGED.getPossibleValues().stream().map(Object::toString).toList();

        List<List<String>> inputs = List.of(hanging, waterlogged);
        List<String> names = new ArrayList<>(List.of("hanging", "waterlogged"));

        sendAllReplacementCommandsForParameters(fromBlock, toBlock, inputs, names, delay);
    }

    private void sendReplaceHorizontalFacingBlockCommand(HorizontalDirectionalBlock fromBlock, HorizontalDirectionalBlock toBlock, int delay) {
        List<String> facing = HorizontalDirectionalBlock.FACING.getPossibleValues().stream().map(Direction::getName).toList();

        List<List<String>> inputs = List.of(facing);
        List<String> names = List.of("facing");

        sendAllReplacementCommandsForParameters(fromBlock, toBlock, inputs, names, delay);
    }

    private void sendAllReplacementCommandsForParameters(Block fromBlock, Block toBlock, List<List<String>> lists, List<String> namesOfProperties, int delay) {
        List<String> result = new ArrayList<>();
        generatePermutations(lists, result, 0, "");

        AtomicInteger delayFactor = new AtomicInteger(1);

        result.forEach(string -> {
            delayFactor.getAndIncrement();

            StringBuilder appendix = new StringBuilder().append("[");
            String[] inputs = string.trim().split(" ");
            for (int i = 0; i < inputs.length; i++) {
                appendix.append(namesOfProperties.get(i)).append("=").append(inputs[i]);
                if (i < inputs.length - 1)
                    appendix.append(",");
            }
            appendix.append("]");

            EdenClient.getMod(Scheduler.class).scheduleSyncDelayed(() -> sendStandardReplaceCommand(fromBlock, toBlock, appendix.toString()), delayFactor.intValue() * delay + 1);
        });
        EdenClient.getMod(Scheduler.class).scheduleSyncDelayed(() -> sendModMessage("All commands have been sent. Replacing might take longer depending on the size of the selection."), delayFactor.intValue() * delay + 2);
    }

    private void generatePermutations(List<List<String>> lists, List<String> result, int depth, String current) {
        if (depth == lists.size()) {
            LogUtils.getLogger().info("Permutation created: {}", current);
            result.add(current);
            return;
        }

        for (int i = 0; i < lists.get(depth).size(); i++) {
            generatePermutations(lists, result, depth + 1, current + " " + lists.get(depth).get(i));
        }
    }

    private void sendStandardReplaceCommand(Block fromBlock, Block toBlock, String appendix) {
        LocalPlayer entityPlayer = PlayerUtils.getPlayer();

        String message = "//replace " + getBlockIDFromBlock(fromBlock) + appendix + " " + getBlockIDFromBlock(toBlock) + appendix;
        if (message.length() > 256)
            sendModMessage("Cannot execute: " + message + " because this command too long.");
        entityPlayer.connection.sendChat(message);
        LogUtils.getLogger().info("Sent command: {}", message);
    }

    private String getBlockIDFromBlock(Block block) {
        return block.toString().replace("Block{minecraft:", "").replace("}", "");
    }
}
