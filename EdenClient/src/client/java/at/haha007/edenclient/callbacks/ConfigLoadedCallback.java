package at.haha007.edenclient.callbacks;


public interface ConfigLoadedCallback {
    Event<ConfigLoadedCallback> EVENT = new Event<>(
            listeners -> () -> {
                for (ConfigLoadedCallback listener : listeners) {
                    listener.configLoaded();
                }
            });

    void configLoaded();
}
