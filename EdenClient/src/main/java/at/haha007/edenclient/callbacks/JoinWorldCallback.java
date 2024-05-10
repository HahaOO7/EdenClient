package at.haha007.edenclient.callbacks;


public interface JoinWorldCallback {
    Event<JoinWorldCallback> EVENT = new Event<>(
            listeners -> () -> {
                for (JoinWorldCallback listener : listeners) {
                    listener.join();
                }
            });

    void join();
}
