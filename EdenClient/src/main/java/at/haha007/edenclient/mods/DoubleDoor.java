package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.PlayerInteractBlockCallback;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
public class DoubleDoor {

    @ConfigSubscriber("false")
    private boolean enabled = false;

    public DoubleDoor() {
        PlayerInteractBlockCallback.EVENT.register(this::onInteractBlock, getClass());
        PerWorldConfig.get().register(this, "doubleDoor");
        registerCommand();
    }

    private void registerCommand() {
        var node = literal("edoubledoor");
        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            sendModMessage((enabled ? "Enabled DoubleDoor." : "Disabled DoubleDoor."));
            return 1;
        }));

        register(node, "DoubleDoor opens multiple doors with one click.");
    }

    private InteractionResult onInteractBlock(LocalPlayer player, ClientLevel world, InteractionHand hand, BlockHitResult blockHitResult) {
        onInteract(player, world, hand, blockHitResult);
        return InteractionResult.PASS;
    }

    private void onInteract(LocalPlayer player, ClientLevel world, InteractionHand hand, BlockHitResult blockHitResult) {
        if (!enabled) return;
        if (player.isShiftKeyDown()) return;
        if (hand != InteractionHand.MAIN_HAND) return;
        BlockPos bp = blockHitResult.getBlockPos();

        BlockState doorState = world.getBlockState(bp);
        if (!(doorState.getBlock() instanceof DoorBlock)) return;


        Direction facing = doorState.getValue(DoorBlock.FACING);
        Boolean open = doorState.getValue(DoorBlock.OPEN);
        DoorHingeSide hingeSide = doorState.getValue(DoorBlock.HINGE);


        BlockPos neighbor = bp.relative(hingeSide == DoorHingeSide.RIGHT ?
                facing.getCounterClockWise() : facing.getClockWise());

        doorState = world.getBlockState(neighbor);
        if (!(doorState.getBlock() instanceof DoorBlock)) return;
        Boolean neighborOpen = doorState.getValue(DoorBlock.OPEN);
        if (!neighborOpen.equals(open)) return;
        if (doorState.getValue(DoorBlock.HINGE) == hingeSide) return;
        if (doorState.getValue(DoorBlock.FACING) != facing) return;

        clickPos(neighbor);
    }

    private void clickPos(BlockPos target) {
        BlockPos bp = new BlockPos(target);
        Direction dir = Direction.UP;
        var nh = Minecraft.getInstance().getConnection();
        if (nh == null) return;
        nh.send(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(bp.relative(dir)), dir, bp, false), 1));
    }
}
