package at.haha007.edenclient.mods.datafetcher;

import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.StringVec3iMap;
import at.haha007.edenclient.utils.tasks.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3i;
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

    LiteralArgumentBuilder<ClientCommandSource> registerCommand() {
        LiteralArgumentBuilder<ClientCommandSource> cmd = CommandManager.literal("playerwarps");
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
        TaskManager tm = new TaskManager(1000);
        Queue<ITask> q = new LinkedList<>();
        q.add(new RunnableTask(() -> fetchData(tm, q, shops, 10)));
        q.add(new RunnableTask(() -> fetchData(tm, q, builds, 12)));
        q.add(new RunnableTask(() -> fetchData(tm, q, farms, 14)));
        q.add(new RunnableTask(() -> fetchData(tm, q, other, 16)));
        q.add(new RunnableTask(() -> fetchData(tm, q, hidden, 22)));
        q.add(new RunnableTask(() -> {
            Screen screen = MinecraftClient.getInstance().currentScreen;
            if (screen == null) return;
            screen.close();
        }));
        fetchData(tm, q, all, 4);
        tm.start();
        return 1;
    }

    private int fetchData(Map<String, Vec3i> map, int slot) {
        TaskManager tm = new TaskManager(1000);
        fetchData(tm, new LinkedList<>(List.of(new RunnableTask(() -> {
            Screen screen = MinecraftClient.getInstance().currentScreen;
            if (screen == null) return;
            screen.close();
        }))), map, slot);
        tm.start();
        return 1;
    }

    private void fetchData(TaskManager tm, Queue<ITask> endTask, Map<String, Vec3i> map, int slot) {
        map.clear();
        tm.then(new RunnableTask(() -> PlayerUtils.messageC2S("/pw")));
        tm.then(new WaitForInventoryNameTask(Pattern.compile(". PlayerWarps - Kategorien")));
        tm.then(new RunnableTask(() -> PlayerUtils.clickSlot(slot)));
        tm.then(new WaitForInventoryNameTask(Pattern.compile(". PlayerWarps - Seite 1/[0-9]{1,2}")));
        tm.then(new RunnableTask(() -> {
            Pattern pattern = Pattern.compile(". PlayerWarps - Seite 1/(?<pages>[0-9]{1,2})");
            Screen screen = MinecraftClient.getInstance().currentScreen;
            if (screen == null) return;
            Matcher matcher = pattern.matcher(screen.getTitle().getString());
            if (!matcher.matches()) return;
            int pages = Integer.parseInt(matcher.group("pages"));

            tm.then(new WaitForTicksTask(5));
            tm.then(new RunnableTask(() -> scanWarps(map)));
            for (int i = 2; i <= pages; i++) {
                tm.then(new RunnableTask(() -> PlayerUtils.clickSlot(50)));
                tm.then(new WaitForInventoryNameTask(Pattern.compile(". PlayerWarps - Seite " + i + "/" + pages)));
                tm.then(new WaitForTicksTask(5));
                tm.then(new RunnableTask(() -> scanWarps(map)));
            }
            tm.then(endTask.poll());
        }));
    }


    private void scanWarps(Map<String, Vec3i> map) {
        Screen sc = MinecraftClient.getInstance().currentScreen;
        if (sc == null) return;
        if (!(sc instanceof GenericContainerScreen containerScreen)) return;
        Inventory inventory = containerScreen.getScreenHandler().getInventory();
        for (int i = 0; i < 45; i++) {
            ItemStack item = inventory.getStack(i);
            if (item == null) continue;
            String name = item.getName().getString();
            item.getTooltip(null, TooltipContext.Default.NORMAL)
                    .stream().map(Text::getString)
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
