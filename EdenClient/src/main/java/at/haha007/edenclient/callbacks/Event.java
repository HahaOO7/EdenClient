package at.haha007.edenclient.callbacks;

import at.haha007.edenclient.utils.EdenUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Event<T> {
    private static final List<Event<?>> EVENTS = new ArrayList<>();
    private final List<Entry<T>> listeners = new ArrayList<>();
    private final Function<List<T>, T> invoker;

    private record Entry<T>(T listener, Class<?> registeredBy, Supplier<Boolean> filter) {
    }

    public Event(Function<List<T>, T> invoker) {
        this.invoker = invoker;
        EVENTS.add(this);
    }

    public void register(T listener, Class<?> registeredBy) {
        register(listener, registeredBy, () -> true);
    }

    public void register(T listener, Class<?> registeredBy, Supplier<Boolean> filter) {
        listeners.add(new Entry<>(listener, registeredBy, filter));
    }

    public T invoker() {
        return invoker.apply(listeners.stream().filter(e -> e.filter.get()).map(Entry::listener).toList());
    }

    public static void unregisterAll(Predicate<Class<?>> filter) {
        EVENTS.forEach(e -> e.listeners.removeIf(c -> filter.test(c.registeredBy())));
    }

    @Override
    public String toString() {
        return "Event{" +
                "listeners=" + listeners +
                ", invoker=" + invoker +
                '}';
    }
}
