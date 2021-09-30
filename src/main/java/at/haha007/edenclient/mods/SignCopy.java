package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.PlayerAttackBlockCallback;
import at.haha007.edenclient.callbacks.PlayerEditSignCallback;
import at.haha007.edenclient.utils.ChatColor;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.StringList;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.tag.ItemTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Arrays;
import java.util.Objects;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class SignCopy {
    @ConfigSubscriber("0;0;0;0")
    private StringList copy;
    @ConfigSubscriber("false")
    private boolean shouldCopy = false;
    @ConfigSubscriber("false")
    private boolean enabled = false;

    public SignCopy() {
        PlayerEditSignCallback.EVENT.register(this::onEditSign);
        PlayerAttackBlockCallback.EVENT.register(this::onAttackBlock);
        registerCommand();
        PerWorldConfig.get().register(this, "signCopy");
    }

    private void registerCommand() {
        var node = literal("esigncopy");
        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            sendModMessage(ChatColor.GOLD + (enabled ? "SignCopy enabled" : "SignCopy disabled"));
            return 1;
        }));
        register(node,
               "SignCopy lets you copy signs and place them again without opening the dialogue/having to type each line again.");
    }

    @SuppressWarnings("ConstantConditions")
    private ActionResult onAttackBlock(ClientPlayerEntity entity, BlockPos pos, Direction side) {
        if (!enabled) return ActionResult.PASS;
        BlockEntity b = MinecraftClient.getInstance().world.getBlockEntity(pos);
        if (!ItemTags.SIGNS.contains(PlayerUtils.getPlayer().getInventory().getMainHandStack().getItem()))
            return ActionResult.PASS;
        if (!(b instanceof SignBlockEntity sign)) {
            shouldCopy = false;
            return ActionResult.PASS;
        }
        shouldCopy = true;
        NbtCompound tag = new NbtCompound();
        sign.readNbt(tag);
        String[] copy = new String[4];
        copy[0] = getString(tag.getString("Text1"));
        copy[1] = getString(tag.getString("Text2"));
        copy[2] = getString(tag.getString("Text3"));
        copy[3] = getString(tag.getString("Text4"));
        this.copy = new StringList();
        this.copy.addAll(Arrays.asList(copy));
        return ActionResult.FAIL;
    }

    private ActionResult onEditSign(ClientPlayerEntity player, SignBlockEntity sign) {
        if (!enabled) return ActionResult.PASS;
        if (!shouldCopy) return ActionResult.PASS;
        UpdateSignC2SPacket packet = new UpdateSignC2SPacket(sign.getPos(),
                copy.get(0).substring(0, copy.get(0).length() - 2),
                copy.get(1).substring(0, copy.get(1).length() - 2),
                copy.get(2).substring(0, copy.get(2).length() - 2),
                copy.get(3).substring(0, copy.get(3).length() - 2));
        Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).sendPacket(packet);
        return ActionResult.FAIL;
    }


    private String getString(String string) {
        return string.
                replaceFirst("\\{", "").
                replaceFirst("\\{", "").
                replaceFirst("\"text\":\"", "").
                replaceFirst("\"text\":\"", "").
                replaceFirst("\"}],", "").
                replaceFirst("\"extra\":\\[", "");
    }
}
