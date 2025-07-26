package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import com.mojang.logging.LogUtils;
import fi.dy.masa.malilib.util.nbt.SimpleNbtStringifier;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
public class NbtInfo {
    @SuppressWarnings("UnstableApiUsage")
    private NbtInfo() {
        CommandManager.register(CommandManager.literal("enbt").executes(c -> {
                    LocalPlayer player = PlayerUtils.getPlayer();
                    Inventory inv = player.getInventory();
                    ItemStack stack = inv.getSelectedItem();
                    if (stack.isEmpty()) {
                        sendModMessage("Take an item in your hand!");
                    } else {
                        try {
                            RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, player.registryAccess());
                            Tag itemTag = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
                            SimpleNbtStringifier stringifier = new SimpleNbtStringifier();
                            String snbt = stringifier.getNbtString(itemTag.asCompound().orElseGet(() -> {
                                CompoundTag emptyTag = new CompoundTag();
                                emptyTag.put("id", itemTag);
                                return emptyTag;
                            }));
                            sendModMessage(snbt);
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
