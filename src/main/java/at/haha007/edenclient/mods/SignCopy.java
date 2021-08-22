package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.callbacks.PlayerAttackBlockCallback;
import at.haha007.edenclient.callbacks.PlayerEditSignCallback;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.tag.ItemTags;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Objects;

import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class SignCopy {
    private final String[] copy;
    private boolean shouldCopy = false;
    private boolean enabled = false;

    public SignCopy() {
        PlayerEditSignCallback.EVENT.register(this::onEditSign);
        PlayerAttackBlockCallback.EVENT.register(this::onAttackBlock);
        registerCommand();
        ConfigSaveCallback.EVENT.register(this::saveCfg);
        ConfigLoadCallback.EVENT.register(this::loadCfg);
        copy = new String[]{"", "", "", ""};
    }

    private void loadCfg(NbtCompound compoundTag) {
        NbtCompound tag = compoundTag.getCompound("signCopy");
        copy[0] = tag.getString("0");
        copy[1] = tag.getString("1");
        copy[2] = tag.getString("2");
        copy[3] = tag.getString("3");
        enabled = tag.contains("enabled") && tag.getBoolean("enabled");
        shouldCopy = tag.contains("copy") && tag.getBoolean("copy");
    }

    private void saveCfg(NbtCompound compoundTag) {
        NbtCompound tag = new NbtCompound();
        tag.putString("0", copy[0]);
        tag.putString("1", copy[1]);
        tag.putString("2", copy[2]);
        tag.putString("3", copy[3]);
        tag.putBoolean("enabled", enabled);
        tag.putBoolean("copy", shouldCopy);
        compoundTag.put("signCopy", tag);
    }


    private void registerCommand() {
        CommandManager.register(CommandManager.literal("signcopy").executes(c -> {
            enabled = !enabled;
            sendModMessage(new LiteralText(enabled ? "SignCopy enabled" : "SignCopy disabled"));
            return 1;
        }));
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
        copy[0] = getString(tag.getString("Text1"));
        copy[1] = getString(tag.getString("Text2"));
        copy[2] = getString(tag.getString("Text3"));
        copy[3] = getString(tag.getString("Text4"));
        return ActionResult.FAIL;
    }

    private ActionResult onEditSign(ClientPlayerEntity player, SignBlockEntity sign) {
        if (!enabled) return ActionResult.PASS;
        if (!shouldCopy) return ActionResult.PASS;
        UpdateSignC2SPacket packet = new UpdateSignC2SPacket(sign.getPos(),
                copy[0].substring(0, copy[0].length() - 2),
                copy[1].substring(0, copy[1].length() - 2),
                copy[2].substring(0, copy[2].length() - 2),
                copy[3].substring(0, copy[3].length() - 2));
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
