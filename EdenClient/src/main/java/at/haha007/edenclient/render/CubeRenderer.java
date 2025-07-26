package at.haha007.edenclient.render;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.EdenRenderUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import org.lwjgl.opengl.GL11;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

@Mod
public class CubeRenderer {
    private final TreeMap<Integer, Set<AABB>> cubes = new TreeMap<>();
    private long tick = 0;

    public CubeRenderer() {
        GameRenderCallback.EVENT.register(this::render, getClass());
        PlayerTickCallback.EVENT.register(this::tick, getClass());
    }

    private void tick(LocalPlayer player) {
        if (cubes.isEmpty()) return;
        tick++;
        while (!cubes.isEmpty() && cubes.firstKey() < tick) {
            cubes.pollFirstEntry();
        }
    }

    private void render(float v) {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        cubes.values().forEach(s -> s.forEach(boundingBox ->
                EdenRenderUtils.drawAreaOutline(boundingBox.getMinPosition(), boundingBox.getMaxPosition(), Color4f.WHITE)));
    }

    public void add(AABB box, int ticks) {
        ticks += (int) this.tick;
        Set<AABB> set = cubes.computeIfAbsent(ticks, k -> new HashSet<>());
        set.add(box);
    }

    public void clear() {
        cubes.clear();
    }
}
