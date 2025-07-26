package at.haha007.edenclient.callbacks;

public interface GameRenderCallback {
    Event<GameRenderCallback> EVENT = new Event<>(
            listeners -> tickDelta -> {
                for (GameRenderCallback listener : listeners) {
                    listener.render(tickDelta);

                }
            });

    void render(float tickDelta);
}
