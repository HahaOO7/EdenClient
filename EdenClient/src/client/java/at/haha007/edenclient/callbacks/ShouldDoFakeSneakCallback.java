package at.haha007.edenclient.callbacks;

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
