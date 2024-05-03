package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TextComponentTagVisitor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
public class NbtInfo {

    public NbtInfo() {
        CommandManager.register(CommandManager.literal("enbt").executes(c -> {
                    LocalPlayer player = PlayerUtils.getPlayer();
                    Inventory inv = player.getInventory();
                    ItemStack stack = inv.getSelected();
                    if (stack.isEmpty()) {
                        sendModMessage("Take an item in your hand!");
                    } else {
                        Tag tag = stack.save(RegistryAccess.EMPTY);
                        String json = Component.Serializer.toJson(new TextComponentTagVisitor("").visit(tag), RegistryAccess.EMPTY);
                        sendModMessage(GsonComponentSerializer.gson().deserialize(json));
                    }
                    return 1;
                }),
                "E-NBT displays all the Nbt-Data of the item you are currently holding.");
    }
}
