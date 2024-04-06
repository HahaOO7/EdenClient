package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.PlayerAttackBlockCallback;
import at.haha007.edenclient.callbacks.PlayerEditSignCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.StringList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;

import java.util.Arrays;
import java.util.Objects;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
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
        var node = literal("esignedit");
        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            sendModMessage((enabled ? "SignCopy enabled" : "SignCopy disabled"));
            return 1;
        }));
        register(node, "SignCopy lets you copy signs and place them again without opening" +
                " the dialogue/having to type each line again.");
    }

    @SuppressWarnings("ConstantConditions")
    private InteractionResult onAttackBlock(LocalPlayer entity, BlockPos pos, Direction side) {
        if (!enabled) return InteractionResult.PASS;
        BlockEntity b = Minecraft.getInstance().level.getBlockEntity(pos);
        Registry<Item> registry = entity.clientLevel.registryAccess().registryOrThrow(ItemTags.SIGNS.registry());
        if (!registry.containsKey(BuiltInRegistries.ITEM.getKey(PlayerUtils.getPlayer().getInventory().getSelected().getItem())))
            return InteractionResult.PASS;
        if (!(b instanceof SignBlockEntity sign)) {
            shouldCopy = false;
            return InteractionResult.PASS;
        }
        shouldCopy = true;
        CompoundTag tag = new CompoundTag();
        sign.load(tag);
        String[] copiedLines = new String[4];
        copiedLines[0] = getString(tag.getString("Text1"));
        copiedLines[1] = getString(tag.getString("Text2"));
        copiedLines[2] = getString(tag.getString("Text3"));
        copiedLines[3] = getString(tag.getString("Text4"));
        this.copy = new StringList();
        this.copy.addAll(Arrays.asList(copiedLines));
        return InteractionResult.FAIL;
    }

    private InteractionResult onEditSign(LocalPlayer player, SignBlockEntity sign, boolean front) {
        if (!enabled) return InteractionResult.PASS;
        if (!shouldCopy) return InteractionResult.PASS;
        ServerboundSignUpdatePacket packet = new ServerboundSignUpdatePacket(sign.getBlockPos(),
                true,
                copy.get(0).substring(0, copy.get(0).length() - 2),
                copy.get(1).substring(0, copy.get(1).length() - 2),
                copy.get(2).substring(0, copy.get(2).length() - 2),
                copy.get(3).substring(0, copy.get(3).length() - 2));
        Objects.requireNonNull(Minecraft.getInstance().getConnection()).send(packet);
        return InteractionResult.FAIL;
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
