package at.haha007.edenclient.callbacks;


import net.minecraft.world.level.chunk.LevelChunk;

public interface UpdateLevelChunkCallback {
    Event<UpdateLevelChunkCallback> EVENT = new Event<>(
            listeners -> c -> {
                for (UpdateLevelChunkCallback listener : listeners) {
                    listener.updateLevelChunk(c);
                }
            });

    void updateLevelChunk(LevelChunk chunk);
}
