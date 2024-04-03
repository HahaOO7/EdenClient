package at.haha007.edenclient.utils;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.annotations.Mod;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class ModInitializer {
    private final Map<Class<?>, Object> registeredMods = new HashMap<>();

    public void initializeMods() {
        List<Class<?>> modClasses = getModClassesSorted();
        modClasses.forEach(this::registerMod);
    }
    public void initializeMods(Collection<Class<?>> filter) {
        List<Class<?>> modClasses = getModClassesSorted();
        modClasses.stream().filter(filter::contains).forEach(this::registerMod);
    }


    public <T> T getMod(Class<T> clazz) {
        //noinspection unchecked
        return (T) registeredMods.get(clazz);
    }

    private void registerMod(Class<?> clazz) {
        try {
            Utils.getLogger().info("Registering mod: %s".formatted(clazz.getCanonicalName()));
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object object = constructor.newInstance();
            registeredMods.put(object.getClass(), object);
        } catch (InstantiationException |
                 IllegalAccessException |
                 InvocationTargetException |
                 NoSuchMethodException e) {
            Utils.getLogger().error("Error while enabling mod: " + clazz.getCanonicalName(), e);
        }
    }

    private List<Class<?>> getModClassesSorted() {
        List<AnnotatedClass> annotatedClasses = getAnnotatedClassesFromFile();
        //CONFIRM DAG
        annotatedClasses.forEach(c -> c.dependencies.add(EdenClient.class));
        annotatedClasses.add(new AnnotatedClass(EdenClient.class, new ArrayList<>()));
        ClassDAG dag = new ClassDAG(annotatedClasses);
        List<Class<?>> sorted = dag.topologicallySorted();
        sorted.remove(EdenClient.class);
        return sorted;
    }

    private List<AnnotatedClass> getAnnotatedClassesFromFile() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("mods.txt")) {
            if (is == null) throw new IOException("File not found: mods.txt");
            String[] strings = new String(is.readAllBytes()).split("\n");
            List<AnnotatedClass> annotatedClasses = new ArrayList<>();
            for (String string : strings) {
                Class<?> clazz = Class.forName(string);
                Mod mod = clazz.getAnnotation(Mod.class);
                List<Class<?>> dependencies = Arrays.stream(mod.dependencies()).distinct().sorted().toList();
                AnnotatedClass annotatedClass = new AnnotatedClass(clazz, new ArrayList<>(dependencies));
                annotatedClasses.add(annotatedClass);
            }
            return annotatedClasses;
        } catch (IOException | ClassNotFoundException e) {
            Utils.getLogger().trace("Error while loading mod.txt", e);
        }
        return new ArrayList<>();
    }

    private static class ClassDAG {
        private final List<Class<?>> classes = new ArrayList<>();
        private final List<List<Integer>> edges = new ArrayList<>();

        ClassDAG(List<AnnotatedClass> annotatedClasses) {
            for (AnnotatedClass aClass : annotatedClasses) {
                this.classes.add(aClass.clazz);
            }
            for (AnnotatedClass aClass : annotatedClasses) {
                List<Integer> edges = aClass.dependencies.stream().map(classes::indexOf).toList();
                this.edges.add(edges);
            }
        }

        List<Class<?>> topologicallySorted() {
            ArrayList<Class<?>> list = new ArrayList<>();
            List<Class<?>> toVisit = new ArrayList<>(classes);
            while (!toVisit.isEmpty()) {
                Class<?> clazz = null;
                for (Class<?> visit : toVisit) {
                    if (!tryToAdd(list, visit)) continue;
                    clazz = visit;
                    break;
                }
                if (clazz == null) throw new IllegalStateException("Error while sorting mod dependencies");
                toVisit.remove(clazz);
            }
            return list;
        }

        private boolean tryToAdd(ArrayList<Class<?>> list, Class<?> clazz) {
            int index = classes.indexOf(clazz);
            List<Integer> edges = this.edges.get(index);
            List<Class<?>> dependencies = edges.stream().map(classes::get).collect(Collectors.toList());
            if (!list.containsAll(dependencies)) return false;
            list.add(clazz);
            return true;
        }
    }

    private record AnnotatedClass(Class<?> clazz, List<Class<?>> dependencies) {
        public String toString() {
            return "AnnotatedClass{clazz=%s, dependencies=%s}".formatted(clazz.getSimpleName(), dependencies);
        }
    }
}
