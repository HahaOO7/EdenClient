package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.EdenRenderUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BlockMarker;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
public class BarrierDisplay {
    //Barrier particle lasts about 4 seconds, repeat every 2?
    private static final int DELAY = 40;
    private int tickCounter = 0;
    @ConfigSubscriber("5")
    private int range = 20;
    @ConfigSubscriber("false")
    private boolean enabled;

    public BarrierDisplay() {
        registerCommand();
        PlayerTickCallback.EVENT.register(this::onTick, getClass());
        PerWorldConfig.get().register(this, "barrierDisplay");
    }

    private void onTick(LocalPlayer player) {
        if (!enabled) return;
        tickCounter++;
        if (player.getInventory().getSelectedItem().getItem() == Items.BARRIER) return;
        Vec3 cameraPos = EdenRenderUtils.getCameraPos();
        if (cameraPos == null) return;
        BlockPos center = new BlockPos((int) cameraPos.x, (int) cameraPos.y, (int) cameraPos.z);
        BlockParticleOption effect = new BlockParticleOption(ParticleTypes.BLOCK_MARKER, Blocks.BARRIER.defaultBlockState());
        ClientLevel level = Minecraft.getInstance().level;
        ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;
        BlockPos.withinManhattanStream(center, range, range, range)
                .filter(bp -> level.getBlockState(bp).getBlock() == Blocks.BARRIER)
                .filter(bp -> (bp.hashCode() + tickCounter) % DELAY == 0)
                .forEach(pos -> particleEngine.add(new BlockMarker.Provider().createParticle(
                        effect,
                        level,
                        pos.getX() + .5,
                        pos.getY() + .5,
                        pos.getZ() + .5,
                        0,
                        0,
                        0)));
    }

    private void registerCommand() {
        var node = literal("ebarrierdisplay");
        node.then(literal("range").then(argument("range", IntegerArgumentType.integer(0, 100000)).executes(c -> {
            range = c.getArgument("range", Integer.class);
            sendModMessage(("Barrier display range is " + range));
            return 1;
        })).executes(c -> {
            sendModMessage(("Barrier display range is " + range));
            return 1;
        }));
        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            sendModMessage(enabled ? "Enabled barrierdisplay." : "Disabled barrierdisplay");
            return 1;
        }));
        register(node,
                "BarrierDisplay displays barriers without being in creative with a barrier in hand. It also works in creative.",
                "Use \"/ebarrierdisplay range <number>\"");
    }
}
