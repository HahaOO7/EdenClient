package at.haha007.edenclient.callbacks;


public interface LeaveWorldCallback {
    Event<LeaveWorldCallback> EVENT = new Event<>(
            listeners -> () -> {
                for (LeaveWorldCallback listener : listeners) {
                    listener.leave();
                }
            });

    void leave();
}
