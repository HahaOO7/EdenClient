package at.haha007.edenclient.mods.worldedithelper;

import at.haha007.edenclient.utils.Scheduler;
import net.minecraft.block.*;
import net.minecraft.block.enums.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class WorldEditHelperReplace {

    public static int replaceCommandRequest(Block fromBlock, Block toBlock, int delay, boolean sendMessage) {
        if (fromBlock instanceof StairsBlock && toBlock instanceof StairsBlock) {
            sendReplaceStairCommand((StairsBlock) fromBlock, (StairsBlock) toBlock, delay);
        } else if (fromBlock instanceof SlabBlock && toBlock instanceof SlabBlock) {
            sendReplaceSlabCommand((SlabBlock) fromBlock, (SlabBlock) toBlock, delay);
        } else if (fromBlock instanceof TrapdoorBlock && toBlock instanceof TrapdoorBlock) {
            sendReplaceTrapdoorBlockCommand((TrapdoorBlock) fromBlock, (TrapdoorBlock) toBlock, delay);
        } else if (fromBlock instanceof DoorBlock && toBlock instanceof DoorBlock) {
            sendReplaceDoorBlockCommand((DoorBlock) fromBlock, (DoorBlock) toBlock, delay);
        } else if (fromBlock instanceof SignBlock && toBlock instanceof SignBlock) {
            sendReplaceSignBlockCommand((SignBlock) fromBlock, (SignBlock) toBlock, delay);
        } else if (fromBlock instanceof WallSignBlock && toBlock instanceof WallSignBlock) {
            sendReplaceWallSignBlockCommand((WallSignBlock) fromBlock, (WallSignBlock) toBlock, delay);
        } else if (fromBlock instanceof FenceBlock && toBlock instanceof FenceBlock) {
            sendReplaceFenceBlockCommand((FenceBlock) fromBlock, (FenceBlock) toBlock, delay);
        } else if (fromBlock instanceof FenceGateBlock && toBlock instanceof FenceGateBlock) {
            sendReplaceFenceGateBlockCommand((FenceGateBlock) fromBlock, (FenceGateBlock) toBlock, delay);
        } else if (fromBlock instanceof WallBlock && toBlock instanceof WallBlock) {
            sendReplaceWallBlockCommand((WallBlock) fromBlock, (WallBlock) toBlock, delay);
        } else if (fromBlock instanceof PillarBlock && toBlock instanceof PillarBlock) {
            sendReplacePillarBlockCommand((PillarBlock) fromBlock, (PillarBlock) toBlock, delay);
        } else if (fromBlock instanceof HorizontalFacingBlock && toBlock instanceof HorizontalFacingBlock) {
            sendReplaceHorizontalFacingBlockCommand((HorizontalFacingBlock) fromBlock, (HorizontalFacingBlock) toBlock, delay);
        } else {
            sendModMessage("Can't replace these blocks with Eden-WE.");
            return 0;
        }
        if (sendMessage)
            sendModMessage(new LiteralText("Replacing ").formatted(Formatting.GOLD).
                    append(new LiteralText(getBlockIDFromBlock(fromBlock)).formatted(Formatting.AQUA)).
                    append(new LiteralText(" with ").formatted(Formatting.GOLD)).
                    append(new LiteralText(getBlockIDFromBlock(toBlock)).formatted(Formatting.AQUA)));
        return 1;
    }

    public static void replaceUndoRequest(Block fromBlock, Block toBlock, int delay) {
        sendModMessage(new LiteralText("Undoing").formatted(Formatting.AQUA)
                .append(new LiteralText(" last request.").formatted(Formatting.GOLD)));
        replaceCommandRequest(fromBlock, toBlock, delay, false);
    }

    public static void replaceRedoRequest(Block fromBlock, Block toBlock, int delay) {
        sendModMessage(new LiteralText("Redoing").formatted(Formatting.AQUA)
                .append(new LiteralText(" last request.").formatted(Formatting.GOLD)));
        replaceCommandRequest(fromBlock, toBlock, delay, false);
    }

    public static void sendReplaceStairCommand(StairsBlock fromBlock, StairsBlock toBlock, int delay) {
        List<String> half = StairsBlock.HALF.getValues().stream().map(BlockHalf::toString).collect(Collectors.toList());
        List<String> shape = StairsBlock.SHAPE.getValues().stream().map(StairShape::toString).collect(Collectors.toList());
        List<String> facing = StairsBlock.FACING.getValues().stream().map(Direction::getName).collect(Collectors.toList());
        List<String> waterlogged = StairsBlock.WATERLOGGED.getValues().stream().map(Object::toString).collect(Collectors.toList());

        List<List<String>> inputs = List.of(half, shape, facing, waterlogged);
        List<String> names = List.of("half", "shape", "facing", "waterlogged");

        sendAllReplacementCommandsForParameters(fromBlock, toBlock, inputs, names, delay);
    }

    public static void sendReplaceSlabCommand(SlabBlock fromBlock, SlabBlock toBlock, int delay) {
        List<String> type = SlabBlock.TYPE.getValues().stream().map(SlabType::toString).collect(Collectors.toList());
        List<String> waterlogged = SlabBlock.WATERLOGGED.getValues().stream().map(Object::toString).collect(Collectors.toList());

        List<List<String>> inputs = List.of(type, waterlogged);
        List<String> names = List.of("type", "waterlogged");

        sendAllReplacementCommandsForParameters(fromBlock, toBlock, inputs, names, delay);
    }

    private static void sendReplaceTrapdoorBlockCommand(TrapdoorBlock fromBlock, TrapdoorBlock toBlock, int delay) {
        List<String> open = TrapdoorBlock.OPEN.getValues().stream().map(Object::toString).collect(Collectors.toList());
        List<String> powered = TrapdoorBlock.POWERED.getValues().stream().map(Object::toString).collect(Collectors.toList());
        List<String> half = TrapdoorBlock.HALF.getValues().stream().map(BlockHalf::toString).collect(Collectors.toList());
        List<String> facing = TrapdoorBlock.FACING.getValues().stream().map(Direction::getName).collect(Collectors.toList());
        List<String> waterlogged = TrapdoorBlock.WATERLOGGED.getValues().stream().map(Object::toString).collect(Collectors.toList());

        List<List<String>> inputs = List.of(open, powered, half, facing, waterlogged);
        List<String> names = List.of("open", "powered", "half", "facing", "waterlogged");

        sendAllReplacementCommandsForParameters(fromBlock, toBlock, inputs, names, delay);
    }

    private static void sendReplaceDoorBlockCommand(DoorBlock fromBlock, DoorBlock toBlock, int delay) {
        List<String> open = DoorBlock.OPEN.getValues().stream().map(Object::toString).collect(Collectors.toList());
        List<String> powered = DoorBlock.POWERED.getValues().stream().map(Object::toString).collect(Collectors.toList());
        List<String> half = DoorBlock.HALF.getValues().stream().map(DoubleBlockHalf::toString).collect(Collectors.toList());
        List<String> facing = DoorBlock.FACING.getValues().stream().map(Direction::getName).collect(Collectors.toList());
        List<String> hinge = DoorBlock.HINGE.getValues().stream().map(DoorHinge::toString).collect(Collectors.toList());

        List<List<String>> inputs = List.of(open, powered, half, facing, hinge);
        List<String> names = List.of("open", "powered", "half", "facing", "hinge");

        sendAllReplacementCommandsForParameters(fromBlock, toBlock, inputs, names, delay);
    }

    public static void sendReplaceSignBlockCommand(SignBlock fromBlock, SignBlock toBlock, int delay) {
        List<String> rotation = SignBlock.ROTATION.getValues().stream().map(v -> Integer.toString(v)).collect(Collectors.toList());
        List<String> waterlogged = SignBlock.WATERLOGGED.getValues().stream().map(Object::toString).collect(Collectors.toList());

        List<List<String>> inputs = List.of(rotation, waterlogged);
        List<String> names = List.of("rotation", "waterlogged");

        sendAllReplacementCommandsForParameters(fromBlock, toBlock, inputs, names, delay);
    }

    public static void sendReplaceWallSignBlockCommand(WallSignBlock fromBlock, WallSignBlock toBlock, int delay) {
        List<String> facing = WallSignBlock.FACING.getValues().stream().map(Direction::getName).collect(Collectors.toList());
        List<String> waterlogged = WallSignBlock.WATERLOGGED.getValues().stream().map(Object::toString).collect(Collectors.toList());

        List<List<String>> inputs = List.of(facing, waterlogged);
        List<String> names = List.of("facing", "waterlogged");

        sendAllReplacementCommandsForParameters(fromBlock, toBlock, inputs, names, delay);
    }

    private static void sendReplaceFenceBlockCommand(FenceBlock fromBlock, FenceBlock toBlock, int delay) {
        List<String> north = FenceBlock.NORTH.getValues().stream().map(Object::toString).collect(Collectors.toList());
        List<String> east = FenceBlock.EAST.getValues().stream().map(Object::toString).collect(Collectors.toList());
        List<String> south = FenceBlock.SOUTH.getValues().stream().map(Object::toString).collect(Collectors.toList());
        List<String> west = FenceBlock.WEST.getValues().stream().map(Object::toString).collect(Collectors.toList());
        List<String> waterlogged = FenceBlock.WATERLOGGED.getValues().stream().map(Object::toString).collect(Collectors.toList());

        List<List<String>> inputs = List.of(north, east, south, west, waterlogged);
        List<String> names = List.of("north", "east", "south", "west", "waterlogged");

        sendAllReplacementCommandsForParameters(fromBlock, toBlock, inputs, names, delay);
    }

    private static void sendReplaceFenceGateBlockCommand(FenceGateBlock fromBlock, FenceGateBlock toBlock, int delay) {
        List<String> facing = FenceGateBlock.FACING.getValues().stream().map(Direction::getName).collect(Collectors.toList());
        List<String> in_wall = FenceGateBlock.IN_WALL.getValues().stream().map(Object::toString).collect(Collectors.toList());
        List<String> open = FenceGateBlock.OPEN.getValues().stream().map(Object::toString).collect(Collectors.toList());
        List<String> powered = FenceGateBlock.POWERED.getValues().stream().map(Object::toString).collect(Collectors.toList());

        List<List<String>> inputs = List.of(facing, in_wall, open, powered);
        List<String> names = List.of("facing", "in_wall", "open", "powered");

        sendAllReplacementCommandsForParameters(fromBlock, toBlock, inputs, names, delay);
    }

    private static void sendReplaceWallBlockCommand(WallBlock fromBlock, WallBlock toBlock, int delay) {
        List<String> north = WallBlock.NORTH_SHAPE.getValues().stream().map(WallShape::asString).collect(Collectors.toList());
        List<String> east = WallBlock.EAST_SHAPE.getValues().stream().map(WallShape::asString).collect(Collectors.toList());
        List<String> south = WallBlock.SOUTH_SHAPE.getValues().stream().map(WallShape::asString).collect(Collectors.toList());
        List<String> west = WallBlock.WEST_SHAPE.getValues().stream().map(WallShape::asString).collect(Collectors.toList());
        List<String> waterlogged = WallBlock.WATERLOGGED.getValues().stream().map(Object::toString).collect(Collectors.toList());
        List<String> up = WallBlock.UP.getValues().stream().map(Object::toString).collect(Collectors.toList());

        List<List<String>> inputs = List.of(north, east, south, west, waterlogged, up);
        List<String> names = List.of("north", "east", "south", "west", "waterlogged", "up");

        sendAllReplacementCommandsForParameters(fromBlock, toBlock, inputs, names, delay);
    }

    private static void sendReplacePillarBlockCommand(PillarBlock fromBlock, PillarBlock toBlock, int delay) {
        List<String> axes = PillarBlock.AXIS.getValues().stream().map(Direction.Axis::toString).collect(Collectors.toList());

        List<List<String>> inputs = List.of(axes);
        List<String> names = new ArrayList<>(List.of("axis"));

        sendAllReplacementCommandsForParameters(fromBlock, toBlock, inputs, names, delay);
    }

    public static void sendReplaceHorizontalFacingBlockCommand(HorizontalFacingBlock fromBlock, HorizontalFacingBlock toBlock, int delay) {
        List<String> facing = HorizontalFacingBlock.FACING.getValues().stream().map(Direction::getName).collect(Collectors.toList());

        List<List<String>> inputs = List.of(facing);
        List<String> names = List.of("facing");

        sendAllReplacementCommandsForParameters(fromBlock, toBlock, inputs, names, delay);
    }

    private static void sendAllReplacementCommandsForParameters(Block fromBlock, Block toBlock, List<List<String>> lists, List<String> namesOfProperties, int delay) {
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

            Scheduler.get().scheduleSyncDelayed(() -> sendStandardReplaceCommand(fromBlock, toBlock, appendix.toString()), delayFactor.intValue() * delay);
        });
        Scheduler.get().scheduleSyncDelayed(() -> sendModMessage("All commands have been sent. Replacing might take longer depending on the size of the selection."), delayFactor.intValue() * delay + 1);
    }

    private static void generatePermutations(List<List<String>> lists, List<String> result, int depth, String current) {
        if (depth == lists.size()) {
            result.add(current);
            return;
        }

        for (int i = 0; i < lists.get(depth).size(); i++) {
            generatePermutations(lists, result, depth + 1, current + " " + lists.get(depth).get(i));
        }
    }

    private static void sendStandardReplaceCommand(Block fromBlock, Block toBlock, String appendix) {
        ClientPlayerEntity entityPlayer = MinecraftClient.getInstance().player;
        if (entityPlayer == null) {
            sendModMessage("Error");
            return;
        }
        String message = "//replace " + getBlockIDFromBlock(fromBlock) + appendix + " " + getBlockIDFromBlock(toBlock) + appendix;
        if (message.length() > 256)
            sendModMessage("Cannot execute: " + message + " because this command too long.");
        entityPlayer.sendChatMessage(message);
        System.out.println("[EC]" + message);
    }

    private static String getBlockIDFromBlock(Block block) {
        return block.toString().replace("Block{minecraft:", "").replace("}", "");
    }
}
