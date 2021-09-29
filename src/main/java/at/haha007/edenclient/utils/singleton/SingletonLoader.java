package at.haha007.edenclient.utils.singleton;

import at.haha007.edenclient.EdenClient;
import org.spongepowered.asm.mixin.transformer.throwables.IllegalClassLoadError;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class SingletonLoader {
    private static boolean loaded = false;
    private final EdenClient edenClient;
    private static final SingletonLoader instance = new SingletonLoader(EdenClient.INSTANCE);
    private final Map<Class<?>, Object> singletons = new HashMap<>();

    static {
        instance.init();
    }

    public static <T> T get(Class<T> clazz) {
        //noinspection unchecked
        return (T) instance.singletons.get(clazz);
    }

    private SingletonLoader(EdenClient edenClient) {
        if (loaded) throw new IllegalStateException("Singletons already initialized");
        loaded = true;
        this.edenClient = edenClient;
    }

    private void init() {
        final Thread mainThread = Thread.currentThread();
        new Thread(() -> {
            try {
                Thread.sleep(100);
                mainThread.join();
                initializeSingletons();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        initializeSingletons();
    }

    private void initializeSingletons() {
        String path = edenClient.getClass().getPackageName();
        Set<Class<?>> classes = new HashSet<>();
        populateSet(classes, path);
        classes.stream().sorted(this::compare).forEachOrdered(this::initialize);
    }

    private void populateSet(Set<Class<?>> classes, String path) {
        try (InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(path.replace('.', '/'));
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Set<String> lines = br.lines().collect(Collectors.toSet());
            lines.stream()
                    .filter(l -> l.endsWith(".class"))
                    .map(s -> asClass(path, s))
                    .filter(Objects::nonNull)
                    .filter(this::hasAnnotation)
                    .forEach(classes::add);
            lines.stream()
                    .filter(s -> !s.contains("."))
                    .forEach(s -> populateSet(classes, path + "." + s));
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    private Class<?> asClass(String path, String fileName) {
        try {
            return Class.forName(path + "." + fileName.substring(0, fileName.length() - 6));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalClassLoadError ignored) {
            //ignore mixins (stupid solution)
        }
        return null;
    }

    private boolean hasAnnotation(Class<?> clazz) {
        return clazz.isAnnotationPresent(Singleton.class);
    }

    private int compare(Class<?> clazzA, Class<?> clazzB) {
        return Integer.compare(getPriority(clazzA), getPriority(clazzB));
    }

    private int getPriority(Class<?> clazz) {
        return clazz.getAnnotation(Singleton.class).priority();
    }

    private void initialize(Class<?> clazz) {
        try {
            System.out.println("[EC] SingletonLoaded: " + clazz.getSimpleName());
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object instance = constructor.newInstance();
            singletons.put(clazz, instance);
            constructor.setAccessible(false);
        } catch (NoSuchMethodException | NullPointerException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.getCause().printStackTrace();
        }
    }
}
