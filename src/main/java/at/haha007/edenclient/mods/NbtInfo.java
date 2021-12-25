package at.haha007.edenclient.mods;

import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.ChatColor;
import at.haha007.edenclient.utils.PlayerUtils;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.visitor.NbtTextFormatter;

import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class NbtInfo {

    public NbtInfo() {
        CommandManager.register(CommandManager.literal("enbt").executes(c -> {
                    ClientPlayerEntity player = PlayerUtils.getPlayer();
                    PlayerInventory inv = player.getInventory();
                    ItemStack stack = inv.getMainHandStack();
                    if (stack.isEmpty()) {
                        sendModMessage(ChatColor.GOLD + "Take an item in your hand!");
                    } else {
                        NbtCompound tag = stack.getNbt();
                        if(tag == null){
                            sendModMessage(ChatColor.GOLD + "This item has no tag.");
                            return 1;
                        }
                        sendModMessage(new NbtTextFormatter("", 1).apply(tag));
                    }
                    return 1;
                }),
                "E-NBT displays all the Nbt-Data of the item you are currently holding.");
    }
}
