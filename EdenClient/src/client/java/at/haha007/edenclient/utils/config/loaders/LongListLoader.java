package at.haha007.edenclient.utils.config.loaders;

import at.haha007.edenclient.utils.config.wrappers.LongList;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.List;

import static java.lang.Integer.parseInt;

public class LongListLoader implements ConfigLoader<LongArrayTag, LongList> {
    @Override
    public @NonNull LongArrayTag save(@NonNull LongList value) {
        long[] arr = new long[value.size()];
        for (int i = 0; i < value.size(); i++) {
            arr[i] = value.get(i);
        }
        return new LongArrayTag(arr);
    }

    @Override
    public @NonNull LongList load(@NonNull LongArrayTag nbtElement) {
        LongList list = new LongList();
        for (long l : nbtElement.getAsLongArray()) {
            list.add(l);
        }
        return list;
    }

    @Override
    public @NonNull LongArrayTag parse(@NotNull String s) {
        String[] parts = s.split(",");
        long[] arr = new long[parts.length];
        for (int i = 0; i < parts.length; i++) {
            arr[i] = Long.parseLong(parts[i].trim());
        }
        return new LongArrayTag(arr);
    }
}
