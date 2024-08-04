package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import com.mojang.logging.LogUtils;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TextComponentTagVisitor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
public class NbtInfo {
    private NbtInfo() {
        CommandManager.register(CommandManager.literal("enbt").executes(c -> {
                    LocalPlayer player = PlayerUtils.getPlayer();
                    Inventory inv = player.getInventory();
                    ItemStack stack = inv.getSelected();
                    if (stack.isEmpty()) {
                        sendModMessage("Take an item in your hand!");
                    } else {
                        try {
                            CompoundTag playerTag = new EntityDataAccessor(player).getData();
                            ListTag inventoryTag = playerTag.getList("Inventory", Tag.TAG_COMPOUND);
                            CompoundTag itemTag = (CompoundTag) inventoryTag.get(inv.selected);

                            String json = Component.Serializer.toJson(new TextComponentTagVisitor("").visit(itemTag), RegistryAccess.EMPTY);
                            sendModMessage(GsonComponentSerializer.gson().deserialize(json));
                        } catch (IllegalStateException e) {
                            sendModMessage("An error occurred while parsing the Nbt-Data!");
                            LogUtils.getLogger().error("An error occurred while parsing the Nbt-Data!", e);
                        }
                    }
                    return 1;
                }),
                "E-NBT displays all the Nbt-Data of the item you are currently holding.");
    }
}
