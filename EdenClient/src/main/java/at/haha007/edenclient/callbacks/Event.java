package at.haha007.edenclient.callbacks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class Event<T> {
    private final List<T> listeners = new ArrayList<>();
    private final Function<List<T>, T> invoker;

    public Event(Function<List<T>, T> invoker) {
        this.invoker = invoker;
    }

    public void register(T listener) {
        listeners.add(listener);
    }

    public void unregister(T listener) {
        listeners.remove(listener);
    }

    public void unregisterIf(Predicate<T> filter) {
        listeners.removeIf(filter);
    }

    public T invoker() {
        return invoker.apply(listeners);
    }

}
