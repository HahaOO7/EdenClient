package at.haha007.edenclient.mods;

import at.haha007.edenclient.Mod;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;
import static at.haha007.edenclient.utils.PlayerUtils.getPlayer;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
public class Bridge {
    private boolean enabled;

    public Bridge() {
        PlayerTickCallback.EVENT.register(this::tick);
        JoinWorldCallback.EVENT.register(this::build);
        registerCommand();
    }

    private void tick(LocalPlayer player) {
        if (!enabled) {
            return;
        }
        Item item = player.getInventory().getSelected().getItem();
        if (!(item instanceof BlockItem blockItem)) return;
        Block block = blockItem.getBlock();
        BlockState defaultState = block.defaultBlockState();
        if (!defaultState.isCollisionShapeFullBlock(player.clientLevel, player.blockPosition())) return;

        ClientLevel world = player.clientLevel;
        int y = player.blockPosition().getY() - 1;
        Optional<BlockPos> target = Optional.of(player).map(Entity::blockPosition)
                .map(bp -> bp.relative(Direction.DOWN))
                .filter(bp -> world.getBlockState(bp).canBeReplaced())
                .filter(bp -> bp.getY() == y);
        target.ifPresent(this::clickPos);
    }

    private void clickPos(Vec3i target) {
        BlockPos bp = new BlockPos(target);
        Direction dir = Direction.UP;
        MultiPlayerGameMode im = Minecraft.getInstance().gameMode;
        if (im == null) return;
        im.useItemOn(getPlayer(), InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(bp.relative(dir)), dir, bp, false));
    }

    private void build() {
        enabled = false;
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientSuggestionProvider> cmd = literal("ebridge");
        cmd.executes(c -> {
            enabled = !enabled;
            sendModMessage(enabled ? "Bridge enabled" : "Bridge disabled");
            return 1;
        });

        register(cmd, "Bridge is used to build bridges.");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
