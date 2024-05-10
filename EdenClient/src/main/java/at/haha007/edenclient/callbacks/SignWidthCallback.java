package at.haha007.edenclient.callbacks;


public interface SignWidthCallback {
    Event<SignWidthCallback> EVENT = new Event<>(
            listeners -> (width, signWidth, edgeReached) -> {
                for (SignWidthCallback listener : listeners) {
                    edgeReached = listener.canContinueWriting(width, signWidth, edgeReached);
                }
                return edgeReached;
            });

    boolean canContinueWriting(int textWidth, int signWidth, boolean edgeReached);
}
