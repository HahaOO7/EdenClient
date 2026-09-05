package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.PlayerInteractBlockCallback;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.getPlayer;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
public class PlaceDirection {
    @ConfigSubscriber
    private boolean enabled = false;
    @ConfigSubscriber
    private Direction direction = Direction.NORTH;

    public PlaceDirection() {
        PlayerInteractBlockCallback.EVENT.register(this::onInteractBlock, getClass());
        PerWorldConfig.get().register(this, "placeDirection");
        registerCommand();
    }


    private void registerCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> cmd = literal("eplacedirection");
        cmd.then(literal("toggle").executes(_ -> {
            enabled = !enabled;
            sendModMessage(enabled ? "PlaceDirection enabled" : "PlaceDirection disabled");
            return 1;
        }));
        cmd.then(literal("direction").then(argument("direction", StringArgumentType.word())
                .suggests((_, b) -> {
                    for (Direction d : Direction.values()) {
                        b.suggest(d.getName());
                    }
                    return b.buildFuture();
                })
                .executes(c -> {
                    String name = c.getArgument("direction", String.class);
                    Direction dir = Direction.byName(name);
                    if (dir == null) {
                        sendModMessage("Unknown direction: " + name);
                        return 0;
                    }
                    direction = dir;
                    sendModMessage("PlaceDirection direction set to " + dir.getName());
                    return 1;
                })));

        register(cmd, "PlaceDirection forces blocks to be placed facing a fixed direction.");
    }

    private InteractionResult onInteractBlock(LocalPlayer player, ClientLevel world, InteractionHand hand, BlockHitResult hitResult) {
        if (!enabled || hitResult.getDirection() == direction) {
            return InteractionResult.PASS;
        }
        clickPos(hitResult.getBlockPos().offset(hitResult.getDirection().getUnitVec3i()));
        return InteractionResult.FAIL;
    }

    private void clickPos(Vec3i target) {
        BlockPos bp = new BlockPos(target);
        MultiPlayerGameMode im = Minecraft.getInstance().gameMode;
        if (im == null) {
            return;
        }
        Vec3 opposite = direction.getUnitVec3();
        Vec3 middle = Vec3.atCenterOf(bp).add(opposite.x() / 2, opposite.y() / 2, opposite.z() / 2);

        BlockHitResult hitResult = new BlockHitResult(middle, direction, bp, false);
        im.useItemOn(getPlayer(), InteractionHand.MAIN_HAND, hitResult);
    }

}
