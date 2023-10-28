package at.haha007.edenclient.utils.config.loaders;

import at.haha007.edenclient.utils.config.wrappers.StringList;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class StringListLoader implements ConfigLoader<ListTag, StringList> {

    public ListTag save(StringList value) {
        ListTag list = new ListTag();
        for (String s : value) {
            list.add(StringTag.valueOf(s));
        }
        return list;
    }

    public StringList load(ListTag nbtElement) {
        StringList list = new StringList();
        for (Tag element : nbtElement) {
            list.add(element.getAsString());
        }
        return list;
    }

    public ListTag parse(String s) {
        ListTag list = new ListTag();
        if (s.isEmpty()) return list;
        for (String v : s.split(";")) {
            list.add(StringTag.valueOf(v));
        }
        return list;
    }
}
