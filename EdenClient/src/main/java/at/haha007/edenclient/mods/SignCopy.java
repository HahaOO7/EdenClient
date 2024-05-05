package at.haha007.edenclient.mods;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.PlayerAttackBlockCallback;
import at.haha007.edenclient.callbacks.PlayerEditSignCallback;
import at.haha007.edenclient.mods.datafetcher.ContainerInfo;
import at.haha007.edenclient.mods.datafetcher.DataFetcher;
import at.haha007.edenclient.utils.EdenUtils;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.StringList;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod(dependencies = DataFetcher.class)
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
        if (b instanceof SignBlockEntity sign) {
            shouldCopy = true;
            SignText frontText = sign.getFrontText();
            String[] copiedLines = new String[4];
            copiedLines[0] = frontText.getMessage(0, true).getString();
            copiedLines[1] = frontText.getMessage(1, true).getString();
            copiedLines[2] = frontText.getMessage(2, true).getString();
            copiedLines[3] = frontText.getMessage(3, true).getString();
            this.copy = new StringList();
            this.copy.addAll(Arrays.asList(copiedLines));
            return InteractionResult.FAIL;
        }else if(b instanceof Container){
            DataFetcher dataFetcher = EdenClient.getMod(DataFetcher.class);
            ContainerInfo containerInfoMod = dataFetcher.getContainerInfo();
            if(containerInfoMod == null) return InteractionResult.PASS;
            ContainerInfo.ChestMap chunkMap = containerInfoMod.getContainerInfo(entity.clientLevel.getChunk(pos).getPos());
            if (chunkMap == null) return InteractionResult.PASS;
            ContainerInfo.ChestInfo chestInfo = chunkMap.get(pos);
            if (chestInfo == null) return InteractionResult.PASS;
            List<Item> items = chestInfo.items();
            StringJoiner stringJoiner = new StringJoiner(",");
            for (Item item : items) {
                String itemKey = BuiltInRegistries.ITEM.getKey(item).getPath();
                stringJoiner.add(itemKey);
            }
            if(stringJoiner.length() > 80){
                PlayerUtils.sendActionBar(Component.text("Failed to create pipe sign, too many items: " + stringJoiner.length(), NamedTextColor.RED));
                return InteractionResult.PASS;
            }
            shouldCopy = true;
            PlayerUtils.sendActionBar(Component.text("Created pipe sign: " + items, NamedTextColor.GOLD));
            this.copy = new StringList();
            copy.add("");
            copy.add("[pipe]");
            copy.add(stringJoiner.toString());
            copy.add("");
            return InteractionResult.FAIL;
        } else {
            shouldCopy = false;
            return InteractionResult.PASS;
        }
    }

    private InteractionResult onEditSign(LocalPlayer player, SignBlockEntity sign, boolean front) {
        if (!enabled) return InteractionResult.PASS;
        if (!shouldCopy) return InteractionResult.PASS;
        ServerboundSignUpdatePacket packet = new ServerboundSignUpdatePacket(sign.getBlockPos(),
                true,
                copy.get(0),
                copy.get(1),
                copy.get(2),
                copy.get(3));
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
