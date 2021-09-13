package at.haha007.edenclient.utils.config.wrappers;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BiStringStringMap implements Map<String, String> {
    //key - value
    private final HashMap<String, String> a = new HashMap<>();
    //value - key
    private final HashMap<String, String> b = new HashMap<>();

    public String getKey(String value) {
        return b.get(value);
    }

    public String getValue(String key) {
        return a.get(key);
    }

    public int size() {
        return a.size();
    }

    public boolean isEmpty() {
        return a.isEmpty();
    }

    public boolean containsKey(Object key) {
        return a.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return b.containsKey(value);
    }

    public String get(Object key) {
        if (!(key instanceof String)) return null;
        return getValue((String) key);
    }

    @Nullable
    public String put(String key, String value) {
        b.put(value, key);
        return a.put(key, value);
    }

    public String remove(Object key) {
        if (!(key instanceof String)) return null;
        if (a.containsKey(key)) {
            String s = a.get(key);
            a.remove(key);
            b.remove(s);
            return s;
        } else if (b.containsKey(key)) {
            String s = b.get(key);
            b.remove(key);
            a.remove(s);
            return s;
        } else {
            return null;
        }
    }

    public void putAll(@NotNull Map<? extends String, ? extends String> m) {
        m.forEach(this::put);
    }

    public void clear() {
        a.clear();
        b.clear();
    }

    @NotNull
    public Set<String> keySet() {
        return ImmutableSet.copyOf(a.keySet());
    }

    @NotNull
    public Collection<String> values() {
        return ImmutableSet.copyOf(a.values());
    }

    @NotNull
    public Set<Entry<String, String>> entrySet() {
        return ImmutableSet.copyOf(a.entrySet());
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BiStringStringMap that = (BiStringStringMap) o;
        return Objects.equals(a, that.a);
    }

    public int hashCode() {
        return a.hashCode();
    }
}
