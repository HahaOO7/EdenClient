package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.callbacks.PlayerInvChangeCallback;
import at.haha007.edenclient.command.Command;
import at.haha007.edenclient.command.CommandManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AutoSell {
    private final Set<Item> autoSellItems = new HashSet<>();
    private long lastSell = 0;

    public AutoSell() {
        CommandManager.registerCommand(new Command(this::onCommand), "autosell", "as");
        ConfigSaveCallback.EVENT.register(this::onSave);
        ConfigLoadCallback.EVENT.register(this::onLoad);
        PlayerInvChangeCallback.EVENT.register(this::onInventoryChange);

    }

    public void onCommand(Command command, String label, String[] args) {

        if (args.length != 1) {
            sendChatMessage("/autosell add");
            sendChatMessage("/autosell remove");
            sendChatMessage("/autosell reset");
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;
        PlayerInventory inventory = player.getInventory();

        switch (args[0]) {
            case "add":
                autoSellItems.add(inventory.getMainHandStack().getItem());
                sendChatMessage("added /sell " + inventory.getMainHandStack().getItem().getName().getString());
                break;
            case "remove":
                autoSellItems.remove(inventory.getMainHandStack().getItem());
                sendChatMessage("removed /sell " + inventory.getMainHandStack().getItem().getName().getString());
                break;
            case "list":
                sendChatMessage(autoSellItems.toString());
                break;
            case "reset":
                autoSellItems.clear();
                sendChatMessage("Removed all entries");
                break;
            default:
                sendChatMessage("/autosell add");
                sendChatMessage("/autosell remove");
                sendChatMessage("/autosell reset");
                sendChatMessage("/autosell list");
        }
    }


    private ActionResult onLoad(NbtCompound compoundTag) {
        NbtCompound tag = compoundTag.getCompound("autoSell");
        NbtList itemIdentifierList = tag.getList("items", 8);
        autoSellItems.clear();
        for (NbtElement key : itemIdentifierList) {
            Item item = Registry.ITEM.get(new Identifier(key.asString()));
            if (item == Items.AIR) continue;
            autoSellItems.add(item);
        }
        return ActionResult.PASS;
    }

    private ActionResult onSave(NbtCompound compoundTag) {
        NbtCompound tag = compoundTag.getCompound("autoSell");
        List<NbtString> itemIds = autoSellItems.stream().map(item -> Registry.ITEM.getId(item).toString()).map(NbtString::of).collect(Collectors.toList());
        NbtList itemsTag = new NbtList();
        itemsTag.addAll(itemIds);
        tag.put("items", itemsTag);
        compoundTag.put("autoSell", tag);
        return ActionResult.PASS;
    }


    public ActionResult onInventoryChange(PlayerInventory inventory) {
        if (!isFullInventory(inventory)) return ActionResult.PASS;
        executeAutoSell();
        return ActionResult.PASS;
    }

    @SuppressWarnings("ConstantConditions")
    private void executeAutoSell() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        long time = System.currentTimeMillis();
        if (time - 200 < lastSell) return;
        autoSellItems
                .stream()
                .filter(item -> player.getInventory().containsAny(Collections.singleton(item)))
                .forEach(item -> player.sendChatMessage("/sell " + item.getName().getString().replace(' ', '_')));
        lastSell = time;
    }

    private boolean isFullInventory(PlayerInventory inventory) {
        return inventory.getEmptySlot() == -1;
    }


    private void sendChatMessage(String text) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(new LiteralText(text));
    }
}
