package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
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
                        CompoundTag tag = stack.getTag();
                        if (tag == null) {
                            sendModMessage("This item has no tag.");
                            return 1;
                        }
                        String json = Component.Serializer.toJson(new TextComponentTagVisitor("", 1).visit(tag));
                        sendModMessage(GsonComponentSerializer.gson().deserialize(json));
                    }
                    return 1;
                }),
                "E-NBT displays all the Nbt-Data of the item you are currently holding.");
    }
}
