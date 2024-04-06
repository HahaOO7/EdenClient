package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.ConfigLoadedCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.ChatColor;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.area.BlockArea;
import at.haha007.edenclient.utils.area.CubeArea;
import at.haha007.edenclient.utils.area.SavableBlockArea;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.BlockSet;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import javax.naming.Name;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.getHitDirectionForBlock;

@Mod
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
    private SavableBlockArea area;
    private BlockPos target = null;
    @ConfigSubscriber("true")
    private boolean filterHeight;

    public Nuker() {
        registerCommand();
        PlayerTickCallback.EVENT.register(this::onTick);
        ConfigLoadedCallback.EVENT.register(this::configLoaded);
        PerWorldConfig.get().register(this, "nuker");
    }

    private void configLoaded() {
        if (area == null) {
            area = new SavableBlockArea(new CubeArea(new Vec3i(-1000000, -1000000, -1000000), new Vec3i(1000000, 1000000, 1000000)));
        }
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientSuggestionProvider> cmd = literal("enuker");
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
        var areaCmd = literal("area");
        areaCmd.then(literal("max").executes(c -> {
            Vec3i min = new Vec3i(-1000000, -1000000, -1000000);
            Vec3i max = new Vec3i(1000000, 1000000, 1000000);
            this.area = new SavableBlockArea(new CubeArea(min, max));
            PlayerUtils.sendModMessage("Nuke area updated.");
            return 1;
        }));
        BlockArea.commands((c, blockArea) -> {
            this.area = new SavableBlockArea(blockArea);
            PlayerUtils.sendModMessage("Nuke area updated.");
        }).forEach(areaCmd::then);
        cmd.then(areaCmd);
        cmd.then(literal("filter")
                .then(literal("toggle").executes(c -> {
                    filterEnabled = !filterEnabled;
                    PlayerUtils.sendModMessage(filterEnabled ? "Filter enabled" : "Filter disabled");
                    return 1;
                }))
                .then(literal("height").executes(c -> {
                    this.filterHeight = !this.filterHeight;
                    PlayerUtils.sendModMessage(this.filterHeight ? "Filter height enabled" : "Filter height disabled");
                    return 1;
                }))
                .then(addCommand())
                .then(removeCommand())
                .then(literal("clear").executes(c -> {
                    filter = new BlockSet();
                    PlayerUtils.sendModMessage("List cleared.");
                    return 1;
                })).executes(c -> {
                    Component text = Component.text("Nuke Blocks: ", NamedTextColor.GOLD);
                    StringBuilder sb = new StringBuilder();
                    for (Block block : filter) {
                        sb.append(block.getName()).append(" ");
                    }
                    text = text.append(Component.text(sb.toString().trim(), NamedTextColor.AQUA));
                    PlayerUtils.sendModMessage(text);
                    return 1;
                }));

        register(cmd,
                "Nuker destroys all blocks in reach (above your feet) in minimal time.");
    }

    private ArgumentBuilder<ClientSuggestionProvider, ?> addCommand() {
        LiteralArgumentBuilder<ClientSuggestionProvider> cmd = literal("add");
        BuiltInRegistries.BLOCK.forEach(block -> {
            String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
            cmd.then(literal(name).executes(context -> {
                filter.add(block);
                PlayerUtils.sendModMessage("Added " + name);
                return 1;
            }));
        });
        return cmd;
    }

    private ArgumentBuilder<ClientSuggestionProvider, ?> removeCommand() {
        LiteralArgumentBuilder<ClientSuggestionProvider> cmd = literal("remove");
        cmd.then(argument("type", StringArgumentType.word()).suggests((context, builder) -> {
            for (Block block : filter) {
                builder.suggest(BuiltInRegistries.BLOCK.getKey(block).getPath());
            }
            return builder.buildFuture();
        }).executes(context -> {
            String name = context.getArgument("type", String.class);
            ResourceLocation identifier = new ResourceLocation(name);
            filter.remove(BuiltInRegistries.BLOCK.get(identifier));
            PlayerUtils.sendModMessage("Removed " + name);
            return 1;
        }));
        return cmd;
    }

    private void onTick(LocalPlayer player) {
        if (!enabled) return;
        ClientPacketListener nh = Minecraft.getInstance().getConnection();
        BlockState air = Blocks.AIR.defaultBlockState();
        MultiPlayerGameMode im = Minecraft.getInstance().gameMode;
        if (im == null) return;
        if (nh == null) return;
        if (target == null) {
            List<BlockPos> minableBlocks = getInstantMinableBlocksInRange(player);
            if (!minableBlocks.isEmpty()) {
                minableBlocks.stream().limit(limit).forEach(p -> {
                    nh.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, p, getHitDirectionForBlock(player, p)));
                    player.clientLevel.setBlockAndUpdate(p, air);
                });
                return;
            }
        }

        if (target == null || Vec3.atCenterOf(target).distanceTo(player.getEyePosition()) > distance)
            findTarget(player);
        if (target == null) return;
        Direction dir = getHitDirectionForBlock(player, target);

        if (im.continueDestroyBlock(target, dir))
            nh.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
        else
            target = null;
    }

    private void findTarget(LocalPlayer player) {
        ClientLevel world = player.clientLevel;
        Vec3 playerPos = player.getEyePosition();
        Stream<BlockPos> stream = getNearby(player);
        stream = stream.filter(p -> Vec3.atCenterOf(p).closerThan(playerPos, distance));
        if (filterHeight)
            stream = stream.filter(p -> player.getBlockY() <= p.getY());
        stream = stream.filter(area::contains);
        stream = stream.filter(p -> !world.getBlockState(p).isAir());
        stream = stream.filter(Predicate.not(this::isNextToLiquid));
        if (filterEnabled) {
            stream = stream.filter(p -> applyFilter(world.getBlockState(p).getBlock()));
        }
        target = stream.map(BlockPos::new).min(Comparator.comparingDouble(p -> Vec3.atCenterOf(p).distanceTo(playerPos))).orElse(null);
    }

    private List<BlockPos> getInstantMinableBlocksInRange(LocalPlayer player) {
        ClientLevel world = player.clientLevel;
        Stream<BlockPos> stream = getNearby(player);
        stream = stream.filter(p -> Vec3.atCenterOf(p).closerThan(player.getEyePosition(), distance));
        if (filterHeight)
            stream = stream.filter(p -> player.getBlockY() <= p.getY());
        stream = stream.filter(area::contains);
        stream = stream.filter(p -> !world.getBlockState(p).isAir());
        stream = stream.filter(p -> instantMinable(p, player));
        stream = stream.filter(Predicate.not(this::isNextToLiquid));
        if (filterEnabled) {
            stream = stream.filter(p -> applyFilter(world.getBlockState(p).getBlock()));
        }
        stream = stream.limit(limit);
        return stream.map(BlockPos::new).toList();

    }

    private boolean applyFilter(Block block) {
        return filter.contains(block);
    }

    private boolean instantMinable(BlockPos pos, LocalPlayer player) {
        ClientLevel world = player.clientLevel;
        BlockState state = world.getBlockState(pos);
        float delta = state.getDestroyProgress(player, world, pos);
        return delta >= 1;
    }

    private boolean isNextToLiquid(BlockPos pos) {
        LocalPlayer player = PlayerUtils.getPlayer();
        ClientLevel world = player.clientLevel;
        //don't care about down
        FluidState state = world.getFluidState(pos.relative(Direction.UP));
        if (!state.isEmpty()) return true;
        state = world.getFluidState(pos.relative(Direction.NORTH));
        if (!state.isEmpty()) return true;
        state = world.getFluidState(pos.relative(Direction.SOUTH));
        if (!state.isEmpty()) return true;
        state = world.getFluidState(pos.relative(Direction.WEST));
        if (!state.isEmpty()) return true;
        state = world.getFluidState(pos.relative(Direction.EAST));
        return !state.isEmpty();
    }

    private Stream<BlockPos> getNearby(LocalPlayer player) {
        BlockPos pos = player.blockPosition();
        int dist = (int) (distance + 1);
        return BlockPos.withinManhattanStream(pos, dist, dist, dist);
    }
}
