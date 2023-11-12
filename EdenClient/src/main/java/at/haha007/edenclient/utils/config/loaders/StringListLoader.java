package at.haha007.edenclient.utils.config.loaders;

import at.haha007.edenclient.utils.config.wrappers.StringList;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

public class StringListLoader implements ConfigLoader<ListTag, StringList> {

    @NotNull
    public ListTag save(@NotNull StringList value) {
        ListTag list = new ListTag();
        for (String s : value) {
            list.add(StringTag.valueOf(s));
        }
        return list;
    }

    @NotNull
    public StringList load(@NotNull ListTag nbtElement) {
        StringList list = new StringList();
        for (Tag element : nbtElement) {
            list.add(element.getAsString());
        }
        return list;
    }

    @NotNull
    public  ListTag parse(@NotNull String s) {
        ListTag list = new ListTag();
        if (s.isEmpty()) return list;
        for (String v : s.split(";")) {
            list.add(StringTag.valueOf(v));
        }
        return list;
    }
}
