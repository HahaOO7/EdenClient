package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.BlockSet;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.stream.Stream;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.TextUtils.createGoldText;

public class Nuker {
    @ConfigSubscriber("5")
    private double distance = 5;
    @ConfigSubscriber("false")
    private boolean enabled = false;
    @ConfigSubscriber("false")
    private boolean filterEnabled = false;
    @ConfigSubscriber("stone")
    private BlockSet filter;
    @ConfigSubscriber("20")
    private int limit = 20;
    @ConfigSubscriber("-1000000,1000000,-1000000,1000000,-1000000,1000000")
    private BlockBox area;
    private BlockPos target = null;

    public Nuker() {
        registerCommand();
        PlayerTickCallback.EVENT.register(this::onTick);
        PerWorldConfig.get().register(this, "nuker");
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientCommandSource> cmd = literal("enuker");
        cmd.then(literal("distance").then(argument("distance", DoubleArgumentType.doubleArg(0, 20)).executes(c -> {
                    distance = c.getArgument("distance", Double.class);
                    PlayerUtils.sendModMessage("Nuker distance is " + distance);
                    return 1;
                }
        )));
        cmd.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            PlayerUtils.sendModMessage(enabled ? "Nuker enabled" : "Nuker disabled");
            return 1;
        }));
        cmd.then(literal("limit").then(argument("limit", IntegerArgumentType.integer(-1)).executes(c -> {
            limit = c.getArgument("limit", Integer.class);
            PlayerUtils.sendModMessage("Nuker limit per tick is " + limit);
            return 1;
        })));
        cmd.then(literal("area").then(argument("min", BlockPosArgumentType.blockPos()).then(argument("max", BlockPosArgumentType.blockPos()).executes(c -> {
            ClientPlayerEntity player = PlayerUtils.getPlayer();
            ServerCommandSource cs = player.getCommandSource();
            BlockPos min = c.getArgument("min", PosArgument.class).toAbsoluteBlockPos(cs);
            BlockPos max = c.getArgument("max", PosArgument.class).toAbsoluteBlockPos(cs);
            area = BlockBox.create(min, max);
            PlayerUtils.sendModMessage("Nuke area updated.");
            return 1;
        }))));
        cmd.then(literal("filter")
                .then(literal("toggle").executes(c -> {
                    filterEnabled = !filterEnabled;
                    PlayerUtils.sendModMessage(filterEnabled ? "Filter enabled" : "Filter disabled");
                    return 1;
                }))
                .then(literal("add").executes(c -> {
                    Block hand = getMainHandStack();
                    if (hand == Blocks.AIR) {
                        PlayerUtils.sendModMessage("You have to have a block in your hand.");
                        return -1;
                    }
                    if (applyFilter(hand)) {
                        PlayerUtils.sendModMessage("Block already in list.");
                        return -1;
                    }
                    filter.add(hand);
                    PlayerUtils.sendModMessage("Block added to list.");
                    return 1;
                }))
                .then(literal("remove").executes(c -> {
                    Block hand = getMainHandStack();
                    if (hand == Blocks.AIR) {
                        PlayerUtils.sendModMessage("You have to have a block in your hand.");
                        return -1;
                    }
                    if (!applyFilter(hand)) {
                        PlayerUtils.sendModMessage("Block not in list.");
                        return -1;
                    }
                    filter.remove(hand);
                    PlayerUtils.sendModMessage("Block removed list.");
                    return 1;
                }))
                .then(literal("clear").executes(c -> {
                    filter = new BlockSet();
                    PlayerUtils.sendModMessage("List cleared.");
                    return 1;
                })).executes(c -> {
                    MutableText text = new LiteralText("Nuke blocks: ").formatted(Formatting.GOLD);
                    for (Block block : filter) {
                        text.append(block.getName().formatted(Formatting.AQUA)).append(" ");
                    }
                    PlayerUtils.sendModMessage(text);
                    return 1;
                }));

        register(cmd,
                createGoldText("Nuker destroys all blocks in reach (above your feet) in minimal time."));
    }

    private Block getMainHandStack() {
        ClientPlayerEntity player = PlayerUtils.getPlayer();
        ItemStack stack = player.getInventory().getMainHandStack();
        if (stack == null) return Blocks.AIR;
        Item item = stack.getItem();
        return Block.getBlockFromItem(item);
    }

    private void onTick(ClientPlayerEntity player) {
        if (!enabled) return;
        ClientPlayNetworkHandler nh = MinecraftClient.getInstance().getNetworkHandler();
        BlockState air = Blocks.AIR.getDefaultState();
        ClientPlayerInteractionManager im = MinecraftClient.getInstance().interactionManager;
        if (im == null) return;
        if (nh == null) return;
        if (limit > 1) {
            List<BlockPos> minableBlocks = getInstantMinableBlocksInRange(player);
            if (!minableBlocks.isEmpty()) {
                target = null;
                minableBlocks.forEach(p -> {
                    nh.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, p, getHitDirectionForBlock(player, p)));
                    nh.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, p, getHitDirectionForBlock(player, p)));
                    player.clientWorld.setBlockState(p, air);
                });
                nh.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                return;
            }
        }

        if (target == null || Vec3d.ofCenter(target).distanceTo(player.getEyePos()) > distance) findTarget(player);
        if (target == null) return;
        Direction dir = getHitDirectionForBlock(player, target);
        if (im.updateBlockBreakingProgress(target, dir))
            nh.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        else
            target = null;
    }

    private Direction getHitDirectionForBlock(ClientPlayerEntity player, BlockPos target) {
        Vec3d playerPos = player.getEyePos();
        Optional<Direction> direction = Arrays.stream(Direction.values())
                .min(Comparator.comparingDouble(
                        dir -> Vec3d.of(dir.getVector()).multiply(.5, .5, .5).add(Vec3d.of(target)).distanceTo(playerPos)));

        return direction.orElse(null);
    }


    private void findTarget(ClientPlayerEntity player) {
        ClientWorld world = player.clientWorld;
        Vec3d playerPos = player.getEyePos();
        Stream<BlockPos> stream = getNearby(player);
        stream = stream.filter(p -> Vec3d.ofCenter(p).isInRange(playerPos, distance));
        stream = stream.filter(p -> player.getBlockY() <= p.getY());
        stream = stream.filter(area::contains);
        stream = stream.filter(p -> !world.getBlockState(p).isAir());
        if (filterEnabled) {
            stream = stream.filter(p -> applyFilter(world.getBlockState(p).getBlock()));
        }
        target = stream.map(BlockPos::new).min(Comparator.comparingDouble(p -> Vec3d.ofCenter(p).distanceTo(playerPos))).orElse(null);
    }

    private List<BlockPos> getInstantMinableBlocksInRange(ClientPlayerEntity player) {
        ClientWorld world = player.clientWorld;
        Stream<BlockPos> stream = getNearby(player);
        stream = stream.filter(p -> Vec3d.ofCenter(p).isInRange(player.getEyePos(), distance));
        stream = stream.filter(p -> player.getBlockY() <= p.getY());
        stream = stream.filter(area::contains);
        stream = stream.filter(p -> !world.getBlockState(p).isAir());
        stream = stream.filter(p -> instantMinable(p, player));
        if (filterEnabled) {
            stream = stream.filter(p -> applyFilter(world.getBlockState(p).getBlock()));
        }
        stream = stream.limit(limit);
        return stream.map(BlockPos::new).toList();
    }

    private boolean applyFilter(Block block) {
        return filter.contains(block);
    }

    private boolean instantMinable(BlockPos pos, ClientPlayerEntity player) {
        ClientWorld world = player.clientWorld;
        BlockState state = world.getBlockState(pos);
        float delta = state.calcBlockBreakingDelta(player, world, pos);
        return delta >= 1;
    }

    private Stream<BlockPos> getNearby(ClientPlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        int dist = (int) (distance + 1);
        return BlockPos.streamOutwards(pos, dist, dist, dist);
    }
}
