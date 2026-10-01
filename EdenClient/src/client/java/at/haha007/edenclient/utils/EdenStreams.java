package at.haha007.edenclient.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import java.util.stream.IntStream;
import java.util.stream.Stream;

public enum EdenStreams {
    ;

    public static Stream<BlockPos> streamOutwards(BlockPos center) {
        return shellRadii()
                .flatMap(r -> offsets(-r, r)
                        .flatMap(dz -> offsets(-r, r)
                                .flatMap(dy -> dxOffsets(r, dz, dy)
                                        .map(dx -> center.offset(dx, dy, dz)))));
    }

    public static Stream<ChunkPos> streamOutwards(ChunkPos center) {
        return shellRadii()
                .flatMap(r -> offsets(-r, r)
                        .flatMap(dz -> dxOffsets(r, dz)
                                .map(dx -> new ChunkPos(center.x() + dx, center.z() + dz))));
    }

    private static Stream<Integer> shellRadii() {
        return IntStream.iterate(0, r -> r + 1).boxed();
    }

    private static Stream<Integer> offsets(int min, int max) {
        return IntStream.rangeClosed(min, max).boxed();
    }

    private static Stream<Integer> dxOffsets(int r, int dz, int dy) {
        if (Math.abs(dz) == r || Math.abs(dy) == r) {
            return offsets(-r, r);
        }
        return surfaceOffsets(r);
    }

    private static Stream<Integer> dxOffsets(int r, int dz) {
        if (Math.abs(dz) == r) {
            return offsets(-r, r);
        }
        return surfaceOffsets(r);
    }

    private static Stream<Integer> surfaceOffsets(int r) {
        return r == 0 ? Stream.of(0) : Stream.of(-r, r);
    }
}
