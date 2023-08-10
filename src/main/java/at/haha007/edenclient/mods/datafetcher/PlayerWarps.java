package at.haha007.edenclient.mods.datafetcher;

import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.StringVec3iMap;
import at.haha007.edenclient.utils.tasks.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static at.haha007.edenclient.utils.MathUtils.integer;

public class PlayerWarps {

    @ConfigSubscriber
    private StringVec3iMap shops = new StringVec3iMap();
    @ConfigSubscriber
    private StringVec3iMap builds = new StringVec3iMap();
    @ConfigSubscriber
    private StringVec3iMap farms = new StringVec3iMap();
    @ConfigSubscriber
    private StringVec3iMap other = new StringVec3iMap();
    @ConfigSubscriber
    private StringVec3iMap all = new StringVec3iMap();
    @ConfigSubscriber
    private StringVec3iMap hidden = new StringVec3iMap();

    PlayerWarps() {
        PerWorldConfig.get().register(this, "dataFetcher.playerWarps");
    }

    LiteralArgumentBuilder<ClientSuggestionProvider> registerCommand() {
        LiteralArgumentBuilder<ClientSuggestionProvider> cmd = CommandManager.literal("playerwarps");
        cmd.then(CommandManager.literal("shops").executes(c -> fetchData(shops, 10)));
        cmd.then(CommandManager.literal("builds").executes(c -> fetchData(builds, 12)));
        cmd.then(CommandManager.literal("farms").executes(c -> fetchData(farms, 14)));
        cmd.then(CommandManager.literal("other").executes(c -> fetchData(other, 16)));
        cmd.then(CommandManager.literal("all").executes(c -> fetchData(all, 4)));
        cmd.then(CommandManager.literal("hidden").executes(c -> fetchData(hidden, 22)));
        cmd.then(CommandManager.literal("*").executes(c -> fetchData()));
        cmd.then(CommandManager.literal("clear").executes(c -> clearData()));
        return cmd;
    }

    private int clearData() {
        shops = new StringVec3iMap();
        builds = new StringVec3iMap();
        farms = new StringVec3iMap();
        other = new StringVec3iMap();
        all = new StringVec3iMap();
        hidden = new StringVec3iMap();
        PlayerUtils.sendModMessage("Cleared data!");
        return 1;
    }

    private int fetchData() {
        TaskManager tm = new TaskManager();
        Queue<Task> q = new LinkedList<>();
        q.add(new SyncTask(() -> fetchData(tm, q, shops, 10)));
        q.add(new SyncTask(() -> fetchData(tm, q, builds, 12)));
        q.add(new SyncTask(() -> fetchData(tm, q, farms, 14)));
        q.add(new SyncTask(() -> fetchData(tm, q, other, 16)));
        q.add(new SyncTask(() -> fetchData(tm, q, hidden, 22)));
        q.add(new SyncTask(() -> {
            Screen screen = Minecraft.getInstance().screen;
            if (screen == null) return;
            screen.onClose();
        }));
        fetchData(tm, q, all, 4);
        tm.start();
        return 1;
    }

    private int fetchData(Map<String, Vec3i> map, int slot) {
        TaskManager tm = new TaskManager();
        fetchData(tm, new LinkedList<>(List.of(new SyncTask(() -> {
            Screen screen = Minecraft.getInstance().screen;
            if (screen == null) return;
            screen.onClose();
        }))), map, slot);
        tm.start();
        return 1;
    }

    private void fetchData(TaskManager tm, Queue<Task> endTask, Map<String, Vec3i> map, int slot) {
        map.clear();
        tm.then(new SyncTask(() -> PlayerUtils.messageC2S("/pwarp")));
        tm.then(new WaitForInventoryTask(Pattern.compile(".*")));
        tm.then(new SyncTask(() -> PlayerUtils.clickSlot(slot)));
        tm.then(new WaitForInventoryTask(Pattern.compile(". PlayerWarps - Seite 1/[0-9]{1,2}")));
        tm.then(new SyncTask(() -> {
            Pattern pattern = Pattern.compile(". PlayerWarps - Seite 1/(?<pages>[0-9]{1,2})");
            Screen screen = Minecraft.getInstance().screen;
            if (screen == null) return;
            Matcher matcher = pattern.matcher(screen.getTitle().getString());
            if (!matcher.matches()) return;
            int pages = Integer.parseInt(matcher.group("pages"));

            tm.then(new WaitForTicksTask(5));
            tm.then(new SyncTask(() -> scanWarps(map)));
            for (int i = 2; i <= pages; i++) {
                tm.then(new SyncTask(() -> PlayerUtils.clickSlot(50)));
                tm.then(new WaitForInventoryTask(Pattern.compile(". PlayerWarps - Seite " + i + "/" + pages)));
                tm.then(new WaitForTicksTask(5));
                tm.then(new SyncTask(() -> scanWarps(map)));
            }
            tm.then(endTask.poll());
        }));
    }


    private void scanWarps(Map<String, Vec3i> map) {
        Screen sc = Minecraft.getInstance().screen;
        if (sc == null) return;
        if (!(sc instanceof ContainerScreen containerScreen)) return;
        Container inventory = containerScreen.getMenu().getContainer();
        for (int i = 0; i < 45; i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null) continue;
            String name = item.getHoverName().getString();
            item.getTooltipLines(null, TooltipFlag.Default.NORMAL)
                    .stream().map(Component::getString)
                    .filter(s -> s.startsWith("Ort: world, "))
                    .findAny().ifPresent(s -> map.put(name, getPos(s)));
        }
    }

    @NotNull
    private Vec3i getPos(String s) {
        s = s.substring(12);
        Pattern pattern = Pattern.compile("(?<x>-?[0-9]+), (?<y>-?[0-9]+), (?<z>-?[0-9]+)");
        Matcher matcher = pattern.matcher(s);
        if (!matcher.matches()) return Vec3i.ZERO;
        return new Vec3i(integer(matcher.group("x")),
                integer(matcher.group("y")),
                integer(matcher.group("z")));
    }

    public Map<String, Vec3i> getShops() {
        return shops;
    }

    public Map<String, Vec3i> getBuilds() {
        return builds;
    }

    public Map<String, Vec3i> getFarms() {
        return farms;
    }

    public Map<String, Vec3i> getOther() {
        return other;
    }

    public Map<String, Vec3i> getAll() {
        return all;
    }

    public Map<String, Vec3i> getHidden() {
        return hidden;
    }
}
