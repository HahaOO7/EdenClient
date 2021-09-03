package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.PlayerUtils;
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
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static at.haha007.edenclient.command.CommandManager.*;

public class Nuker {
    private double distance = 5;
    private boolean enabled = false;
    private boolean filterEnabled = false;
    private Set<Block> filter = new HashSet<>();
    private int limit = 20;
    private final int min = Integer.MIN_VALUE;
    private final int max = Integer.MAX_VALUE;
    private BlockBox area = BlockBox.create(new Vec3i(min, min, min), new Vec3i(max, max, max));

    public Nuker() {
        registerCommand();
        PlayerTickCallback.EVENT.register(this::onTick);
        ConfigLoadCallback.EVENT.register(this::load);
        ConfigSaveCallback.EVENT.register(this::save);
    }

    private void save(NbtCompound nbtCompound) {
        NbtCompound tag = new NbtCompound();
        NbtList filter = new NbtList();
        for (Block block : this.filter) {
            filter.add(NbtString.of(Registry.BLOCK.getId(block).toString()));
        }
        tag.put("filter", filter);
        tag.putBoolean("filterEnabled", filterEnabled);
        tag.putInt("limit", limit);
        tag.putDouble("distance", distance);
        tag.putIntArray("area", new int[]{area.getMinX(), area.getMaxX(), area.getMinY(), area.getMaxY(), area.getMinZ(), area.getMaxZ()});
    }

    private void load(NbtCompound nbtCompound) {
        filter = new HashSet<>();
        enabled = false;
        if (!nbtCompound.contains("nuker")) {
            distance = 5;
            filterEnabled = false;
            limit = 20;
            area = BlockBox.create(new Vec3i(min, min, min), new Vec3i(max, max, max));
            return;
        }

        NbtCompound tag = nbtCompound.getCompound("nuker");
        filterEnabled = tag.getBoolean("filterEnabled");
        tag.getList("filter", NbtElement.STRING_TYPE).stream().map(NbtElement::asString).map(Identifier::new).map(Registry.BLOCK::get).forEach(filter::add);
        limit = tag.getInt("limit");
        distance = tag.getDouble("distance");
        int[] a = tag.getIntArray("area");
        area = new BlockBox(a[0], a[3], a[1], a[4], a[2], a[5]);
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientCommandSource> cmd = literal("nuke");
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
                    filter = new HashSet<>();
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
        register(cmd);
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
        World world = player.clientWorld;
        if (nh == null) return;
        Stream<BlockPos> stream = getNearby(player);
        stream = stream.filter(p -> p.isWithinDistance(player.getEyePos(), distance));
        stream = stream.filter(p -> player.getBlockY() <= p.getY());
        stream = stream.filter(area::contains);
        stream = stream.filter(p -> !world.getBlockState(p).isAir());
        stream = stream.filter(p -> instantMinable(p, player));
        if (filterEnabled) {
            stream = stream.filter(p -> applyFilter(world.getBlockState(p).getBlock()));
        }
        stream = stream.limit(limit);
        stream.forEach(p -> {
            nh.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, p, Direction.UP));
            player.clientWorld.setBlockState(p, air);
        });
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
