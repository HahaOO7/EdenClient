package at.haha007.edenclient.callbacks;

import at.haha007.edenclient.utils.EdenUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Event<T> {
    private static final List<Event<?>> EVENTS = new ArrayList<>();
    private final List<Entry<T>> listeners = new ArrayList<>();
    private final Function<List<T>, T> invoker;

    private record Entry<T>(T listener, Class<?> registeredBy) {
    }

    public Event(Function<List<T>, T> invoker) {
        this.invoker = invoker;
        EVENTS.add(this);
    }

    public void register(T listener, Class<?> registeredBy) {
        listeners.add(new Entry<>(listener, registeredBy));
    }

    public T invoker() {
        return invoker.apply(listeners.stream().map(Entry::listener).toList());
    }

    public static void unregisterAll(Predicate<Class<?>> filter) {
        System.out.println("------------------------");
        EVENTS.forEach(e -> {
            System.out.println(e.invoker.getClass().getSimpleName());
            e.listeners.stream().filter(c -> filter.test(c.registeredBy())).map(Entry::registeredBy).map(Class::getSimpleName).map(s -> "unregister " + s).forEach(System.out::println);
        });
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
