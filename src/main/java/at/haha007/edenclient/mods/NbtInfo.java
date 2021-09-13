package at.haha007.edenclient.mods;

import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.visitor.NbtTextFormatter;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class NbtInfo {

    public NbtInfo() {
        CommandManager.register(CommandManager.literal("nbt").executes(c -> {
            ClientPlayerEntity player = PlayerUtils.getPlayer();
            PlayerInventory inv = player.getInventory();
            ItemStack stack = inv.getMainHandStack();
            if (stack.isEmpty()) {
                sendModMessage(new LiteralText("Take an item in your hand!").formatted(Formatting.GOLD));
            } else {
                sendModMessage(new NbtTextFormatter("", 1).apply(stack.getNbt()));
            }
            return 1;
        }));
    }
}
