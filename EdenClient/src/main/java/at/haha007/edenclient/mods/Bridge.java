package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.stream.Stream;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.getPlayer;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
public class Bridge {
    @Setter
    @Getter
    private boolean enabled;
    private int range;

    public Bridge() {
        PlayerTickCallback.EVENT.register(this::tick, getClass());
        JoinWorldCallback.EVENT.register(this::build, getClass());
        registerCommand();
    }

    private void tick(LocalPlayer player) {
        if (!enabled) {
            return;
        }
        Item item = player.getInventory().getSelectedItem().getItem();
        if (!(item instanceof BlockItem blockItem)) return;
        Block block = blockItem.getBlock();
        BlockState defaultState = block.defaultBlockState();
        if (!defaultState.isCollisionShapeFullBlock(player.clientLevel, player.blockPosition())) return;

        streamPlaceBlocks().limit(3).forEach(this::clickPos);
    }

    private void clickPos(Vec3i target) {
        BlockPos bp = new BlockPos(target);
        Direction dir = Direction.UP;
        MultiPlayerGameMode im = Minecraft.getInstance().gameMode;
        if (im == null) return;
        BlockHitResult hitResult = new BlockHitResult(Vec3.atLowerCornerOf(bp.relative(dir)), dir, bp, false);
        im.useItemOn(getPlayer(), InteractionHand.MAIN_HAND, hitResult);
    }

    private void build() {
        enabled = false;
    }

    private Stream<BlockPos> streamPlaceBlocks() {
        LocalPlayer player = PlayerUtils.getPlayer();
        ClientLevel level = player.clientLevel;
        BlockPos center = player.blockPosition().below();
        Stream<BlockPos> stream = BlockPos.withinManhattanStream(center, range, 1, range);
        stream = stream.map(BlockPos::new);
        stream = stream.filter(p -> p.getY() == center.getY());
        stream = stream.filter(p -> level.getBlockState(p).isAir() || level.getBlockState(p).canBeReplaced());
        stream = stream.sorted(Comparator.comparingInt(p -> p.distManhattan(center)));
        return stream;
    }

    private void registerCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> cmd = literal("ebridge");
        cmd.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            sendModMessage(enabled ? "Bridge enabled" : "Bridge disabled");
            return 1;
        }));
        cmd.then(literal("range").then(argument("range", IntegerArgumentType.integer(1, 10)).executes(c -> {
            range = c.getArgument("range", Integer.class);
            sendModMessage("Bridge range set to " + range);
            return 1;
        })));

        register(cmd, "Bridge is used to build bridges.");
    }
}
