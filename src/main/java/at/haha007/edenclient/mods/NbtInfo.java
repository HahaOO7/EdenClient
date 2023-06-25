package at.haha007.edenclient.mods;

import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.ChatColor;
import at.haha007.edenclient.utils.PlayerUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TextComponentTagVisitor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class NbtInfo {

    public NbtInfo() {
        CommandManager.register(CommandManager.literal("enbt").executes(c -> {
                    LocalPlayer player = PlayerUtils.getPlayer();
                    Inventory inv = player.getInventory();
                    ItemStack stack = inv.getSelected();
                    if (stack.isEmpty()) {
                        sendModMessage(ChatColor.GOLD + "Take an item in your hand!");
                    } else {
                        CompoundTag tag = stack.getTag();
                        if(tag == null){
                            sendModMessage(ChatColor.GOLD + "This item has no tag.");
                            return 1;
                        }
                        sendModMessage(new TextComponentTagVisitor("", 1).visit(tag));
                    }
                    return 1;
                }),
                "E-NBT displays all the Nbt-Data of the item you are currently holding.");
    }
}
