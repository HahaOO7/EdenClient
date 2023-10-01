package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.ChatColor;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.BlockMarker;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.Random;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
public class BarrierDisplay {
    private static final int dist = 5;
    private final Random rand = new Random();
    @ConfigSubscriber("0")
    private int counter = 20;
    @ConfigSubscriber("false")
    private boolean enabled;

    public BarrierDisplay() {
        registerCommand();
        PlayerTickCallback.EVENT.register(this::onTick);
        PerWorldConfig.get().register(this, "barrierDisplay");
    }

    private void onTick(LocalPlayer player) {
        if (!enabled) return;
        if (player.getInventory().getSelected().getItem() == Items.BARRIER) return;
        for (int i = 0; i < counter; i++) {
            BlockPos pos = player.blockPosition().offset((int) (rand.nextGaussian() * dist), (int) (rand.nextGaussian() * dist), (int) (rand.nextGaussian() * dist));
            if (player.clientLevel.getBlockState(pos).getBlock() != Blocks.BARRIER) continue;
            var effect = new BlockParticleOption(ParticleTypes.BLOCK_MARKER, Blocks.BARRIER.defaultBlockState());
            Minecraft.getInstance().particleEngine.add(new BlockMarker.Provider().createParticle(effect,
                    player.clientLevel, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 0, 0, 0));
        }
    }

    private void registerCommand() {
        var node = literal("ebarrierdisplay");
        node.then(literal("count").then(argument("count", IntegerArgumentType.integer(0, 100000)).executes(c -> {
            counter = c.getArgument("count", Integer.class);
            sendModMessage(ChatColor.GOLD + ("Barrier display counter is " + counter));
            return 1;
        })));
        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            sendModMessage(enabled ? "Enabled barrierdisplay." : "Disabled barrierdisplay");
            return 1;
        }));
        register(node,
                "BarrierDisplay displays barriers without being in creative with a barrier in hand. It also works in creative.",
                "Use \"/ebarrierdisplay count <number>\" to set how many blocks the BarrierDisplay should check per tick. (max. of 10000 is advised).");
    }
}
