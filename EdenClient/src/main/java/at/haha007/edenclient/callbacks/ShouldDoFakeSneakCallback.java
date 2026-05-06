package at.haha007.edenclient.callbacks;

/**
 * Callback for when a container screen is about to be opened.
 * Return true to cancel the opening of the container.
 */
public interface ShouldDoFakeSneakCallback {
    Event<ShouldDoFakeSneakCallback> EVENT = new Event<>(
            listeners -> () -> {
                for (ShouldDoFakeSneakCallback listener : listeners) {
                    if (listener.shouldDoFakeSneak()) {
                        return true;
                    }
                }
                return false;
            });

    boolean shouldDoFakeSneak();
}
