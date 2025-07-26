package at.haha007.edenclient.render;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.EdenRenderUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

@Mod
public class TracerRenderer {
    private final TreeMap<Integer, Set<Vec3>> tracers = new TreeMap<>();
    private long tick = 0;

    public TracerRenderer() {
        GameRenderCallback.EVENT.register(this::render, getClass());
        PlayerTickCallback.EVENT.register(this::tick, getClass());
        LeaveWorldCallback.EVENT.register(tracers::clear, getClass());
    }

    private void tick(LocalPlayer player) {
        if (tracers.isEmpty()) return;
        tick++;
        while (!tracers.isEmpty() && tracers.firstKey() < tick) {
            tracers.pollFirstEntry();
        }
    }

    private void render(float delta) {
        if (tracers.isEmpty()) return;
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        EdenRenderUtils.drawTracers(tracers.values().stream().flatMap(Set::stream).toList(), Color4f.WHITE);
    }

    public void add(Vec3 target, int ticks) {
        ticks += (int) this.tick;
        Set<Vec3> set = tracers.computeIfAbsent(ticks, k -> new HashSet<>());
        set.add(target);
    }

    public void clear() {
        tracers.clear();
    }
}
