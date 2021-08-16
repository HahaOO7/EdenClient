package at.haha007.edenclient.mods.worldedithelper;

import at.haha007.edenclient.utils.Scheduler;
import net.minecraft.block.*;
import net.minecraft.block.enums.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Direction;

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
        } else if (fromBlock instanceof PillarBlock && toBlock instanceof PillarBlock) {
            sendReplacePillarBlockCommand((PillarBlock) fromBlock, (PillarBlock) toBlock, delay);
        } else if (fromBlock instanceof HorizontalConnectingBlock && toBlock instanceof HorizontalConnectingBlock) {
            sendReplaceHorizontalConnectingBlockCommand((HorizontalConnectingBlock) fromBlock, (HorizontalConnectingBlock) toBlock, delay);
        } else if (fromBlock instanceof HorizontalFacingBlock && toBlock instanceof HorizontalFacingBlock) {
            sendReplaceHorizontalFacingBlockCommand((HorizontalFacingBlock) fromBlock, (HorizontalFacingBlock) toBlock, delay);
        } else {
            sendModMessage("Can't replace these blocks with Eden-WE.");
        }
        if (sendMessage)
            sendModMessage(new LiteralText("Replacing ").formatted(Formatting.GOLD).
                    append(new LiteralText(fromBlock.getName().getString()).formatted(Formatting.AQUA)).
                    append(new LiteralText(" with ").formatted(Formatting.GOLD)).
                    append(new LiteralText(toBlock.getName().getString()).formatted(Formatting.AQUA)));
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
        var half = StairsBlock.HALF.getValues();
        var shape = StairsBlock.SHAPE.getValues();
        var direction = StairsBlock.FACING.getValues();
        var waterlogged = StairsBlock.WATERLOGGED.getValues();

        int delayFactor = 1;

        for (BlockHalf currHalf : half) {
            for (StairShape currShape : shape) {
                for (Direction currDirection : direction) {
                    for (Boolean currWaterlogged : waterlogged) {
                        String appendix = "[" + "half=" + currHalf + "," +
                                "shape=" + currShape + "," +
                                "facing=" + currDirection + "," +
                                "waterlogged=" + currWaterlogged + "]";

                        delayFactor++;

                        Scheduler.get().scheduleSyncDelayed(() -> sendStandardReplaceCommand(fromBlock, toBlock, appendix), delayFactor * delay + 1);
                    }
                }
            }
        }
        Scheduler.get().scheduleSyncDelayed(() -> sendModMessage("All commands have been sent. Replacing might take longer depending on the size of the selection."), delayFactor * delay + 2);
    }

    public static void sendReplaceSlabCommand(SlabBlock fromBlock, SlabBlock toBlock, int delay) {
        var type = SlabBlock.TYPE.getValues();
        var waterlogged = SlabBlock.WATERLOGGED.getValues();

        int delayFactor = 1;

        for (SlabType currType : type) {
            for (Boolean currWaterlogged : waterlogged) {
                String appendix = "[" + "type=" + currType + "," +
                        "waterlogged=" + currWaterlogged + "]";

                delayFactor++;

                Scheduler.get().scheduleSyncDelayed(() -> sendStandardReplaceCommand(fromBlock, toBlock, appendix), delayFactor * delay + 1);
            }
        }
        Scheduler.get().scheduleSyncDelayed(() -> sendModMessage("All commands have been sent. Replacing might take longer depending on the size of the selection."), delayFactor * delay + 2);
    }

    private static void sendReplaceTrapdoorBlockCommand(TrapdoorBlock fromBlock, TrapdoorBlock toBlock, int delay) {
        var open = TrapdoorBlock.OPEN.getValues();
        var powered = TrapdoorBlock.POWERED.getValues();
        var half = TrapdoorBlock.HALF.getValues();
        var facing = TrapdoorBlock.FACING.getValues();
        var waterlogged = TrapdoorBlock.WATERLOGGED.getValues();

        int delayFactor = 1;

        for (Boolean currOpen : open) {
            for (Boolean currPowered : powered) {
                for (BlockHalf currHalf : half) {
                    for (Direction currFacing : facing) {
                        for (Boolean currWaterlogged : waterlogged) {
                            String appendix = "[" + "open=" + currOpen + "," +
                                    "powered=" + currPowered + "," +
                                    "half=" + currHalf + "," +
                                    "facing=" + currFacing + "," +
                                    "waterlogged=" + currWaterlogged + "]";

                            delayFactor++;

                            Scheduler.get().scheduleSyncDelayed(() -> sendStandardReplaceCommand(fromBlock, toBlock, appendix), delayFactor * delay + 1);
                        }
                    }
                }
            }
        }
        Scheduler.get().scheduleSyncDelayed(() -> sendModMessage("All commands have been sent. Replacing might take longer depending on the size of the selection."), delayFactor * delay + 2);
    }

    private static void sendReplaceDoorBlockCommand(DoorBlock fromBlock, DoorBlock toBlock, int delay) {
        var open = DoorBlock.OPEN.getValues();
        var powered = DoorBlock.POWERED.getValues();
        var half = DoorBlock.HALF.getValues();
        var facing = DoorBlock.FACING.getValues();
        var hinge = DoorBlock.HINGE.getValues();

        int delayFactor = 1;

        for (Boolean currOpen : open) {
            for (Boolean currPowered : powered) {
                for (DoubleBlockHalf currHalf : half) {
                    for (Direction currFacing : facing) {
                        for (DoorHinge currHinge : hinge) {
                            String appendix = "[" + "open=" + currOpen + "," +
                                    "powered=" + currPowered + "," +
                                    "half=" + currHalf + "," +
                                    "facing=" + currFacing + "," +
                                    "hinge=" + currHinge + "]";

                            delayFactor++;

                            Scheduler.get().scheduleSyncDelayed(() -> sendStandardReplaceCommand(fromBlock, toBlock, appendix), delayFactor * delay + 1);
                        }
                    }
                }
            }
        }
        Scheduler.get().scheduleSyncDelayed(() -> sendModMessage("All commands have been sent. Replacing might take longer depending on the size of the selection."), delayFactor * delay + 2);
    }

    public static void sendReplaceSignBlockCommand(SignBlock fromBlock, SignBlock toBlock, int delay) {
        var rotation = SignBlock.ROTATION.getValues();
        var waterlogged = SignBlock.WATERLOGGED.getValues();

        int delayFactor = 1;

        for (Integer currRotation : rotation) {
            for (Boolean currWaterlogged : waterlogged) {
                String appendix = "[" + "rotation=" + currRotation + "," +
                        "waterlogged=" + currWaterlogged + "]";

                delayFactor++;

                Scheduler.get().scheduleSyncDelayed(() -> sendStandardReplaceCommand(fromBlock, toBlock, appendix), delayFactor * delay + 1);
            }
        }
        Scheduler.get().scheduleSyncDelayed(() -> sendModMessage("All commands have been sent. Replacing might take longer depending on the size of the selection."), delayFactor * delay + 2);
    }

    public static void sendReplaceWallSignBlockCommand(WallSignBlock fromBlock, WallSignBlock toBlock, int delay) {
        var facing = WallSignBlock.FACING.getValues();
        var waterlogged = WallSignBlock.WATERLOGGED.getValues();

        int delayFactor = 1;

        for (Direction currFacing : facing) {
            for (Boolean currWaterlogged : waterlogged) {
                String appendix = "[" + "facing=" + currFacing + "," +
                        "waterlogged=" + currWaterlogged + "]";

                delayFactor++;

                Scheduler.get().scheduleSyncDelayed(() -> sendStandardReplaceCommand(fromBlock, toBlock, appendix), delayFactor * delay + 1);
            }
        }

        Scheduler.get().scheduleSyncDelayed(() -> sendModMessage("All commands have been sent. Replacing might take longer depending on the size of the selection."), delayFactor * delay + 2);
    }

    private static void sendReplacePillarBlockCommand(PillarBlock fromBlock, PillarBlock toBlock, int delay) {
        var axis = PillarBlock.AXIS.getValues();

        int delayFactor = 1;

        for (Direction.Axis axi : axis) {

            delayFactor++;

            Scheduler.get().scheduleSyncDelayed(() -> sendStandardReplaceCommand(fromBlock, toBlock, "[" + "axis=" + axi + "]"), delayFactor * delay + 20);
        }

        Scheduler.get().scheduleSyncDelayed(() -> sendModMessage("All commands have been sent. Replacing might take longer depending on the size of the selection."), delayFactor * delay + 2);
    }

    public static void sendReplaceHorizontalConnectingBlockCommand(HorizontalConnectingBlock fromBlock, HorizontalConnectingBlock toBlock, int delay) {
        var east = HorizontalConnectingBlock.EAST.getValues();
        var north = HorizontalConnectingBlock.NORTH.getValues();
        var south = HorizontalConnectingBlock.SOUTH.getValues();
        var west = HorizontalConnectingBlock.WEST.getValues();
        var waterlogged = HorizontalConnectingBlock.WATERLOGGED.getValues();

        int delayFactor = 1;

        for (Boolean currEast : east) {
            for (Boolean currNorth : north) {
                for (Boolean currSouth : south) {
                    for (Boolean currWest : west) {
                        for (Boolean currWaterlogged : waterlogged) {
                            String appendix = "[" + "east=" + currEast + "," +
                                    "north=" + currNorth + "," +
                                    "south=" + currSouth + "," +
                                    "west=" + currWest + "," +
                                    "waterlogged=" + currWaterlogged + "]";

                            delayFactor++;

                            Scheduler.get().scheduleSyncDelayed(() -> sendStandardReplaceCommand(fromBlock, toBlock, appendix), delayFactor * delay + 1);
                        }
                    }
                }
            }
        }
        Scheduler.get().scheduleSyncDelayed(() -> sendModMessage("All commands have been sent. Replacing might take longer depending on the size of the selection."), delayFactor * delay + 2);
    }

    public static void sendReplaceHorizontalFacingBlockCommand(HorizontalFacingBlock fromBlock, HorizontalFacingBlock toBlock, int delay) {
        var facing = HorizontalFacingBlock.FACING.getValues();

        int delayFactor = 1;

        for (Direction currFacing : facing) {

            delayFactor++;

            Scheduler.get().scheduleSyncDelayed(() -> sendStandardReplaceCommand(fromBlock, toBlock, "[" + "facing=" + currFacing + "]"), delayFactor * delay + 20);
        }
        Scheduler.get().scheduleSyncDelayed(() -> sendModMessage("All commands have been sent. Replacing might take longer depending on the size of the selection."), delayFactor * delay + 2);
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
        System.out.println(("//replace " + getBlockIDFromBlock(fromBlock) + appendix + " " + getBlockIDFromBlock(toBlock) + appendix));
    }

    private static String getBlockIDFromBlock(Block block) {
        return block.getName().getString().replace(" ", "_").toLowerCase();
    }
}
