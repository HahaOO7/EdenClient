package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.particle.ItemBillboardParticle;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;
import static at.haha007.edenclient.utils.TextUtils.createGoldText;

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

    private void onTick(ClientPlayerEntity player) {
        if (!enabled) return;
        if (player.getInventory().getMainHandStack().getItem() == Items.BARRIER) return;
        for (int i = 0; i < counter; i++) {
            BlockPos pos = player.getBlockPos().add(rand.nextGaussian() * dist, rand.nextGaussian() * dist, rand.nextGaussian() * dist);
            if (player.clientWorld.getBlockState(pos).getBlock() != Blocks.BARRIER) continue;
            MinecraftClient.getInstance().particleManager.addParticle(new ItemBillboardParticle.BarrierFactory().createParticle(
                    null, player.clientWorld, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 0, 0, 0));
        }
    }

    private void registerCommand() {
        var node = literal("ebarrierdisplay");
        node.then(literal("count").then(argument("count", IntegerArgumentType.integer(0, 100000)).executes(c -> {
            counter = c.getArgument("count", Integer.class);
            sendModMessage(new LiteralText("Barrier display counter is " + counter).formatted(Formatting.GOLD));
            return 1;
        })));
        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            sendModMessage(enabled ? "Enabled barrierdisplay." : "Disabled barrierdisplay");
            return 1;
        }));
        register(node,
                createGoldText("BarrierDisplay displays barriers without being in creative with a barrier in hand. It also works in creative."),
                createGoldText("Use \"/ebarrierdisplay count <number>\" to set how many blocks the BarrierDisplay should check per tick. (max. of 10000 is advised)." ));
    }
}
