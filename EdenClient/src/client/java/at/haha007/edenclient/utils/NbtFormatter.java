package at.haha007.edenclient.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Iterator;
import java.util.Map;

public class NbtFormatter {

    public static Component format(Tag tag,
                                   boolean prettyPrint,
                                   int spacesPerIndent,
                                   int maxIndentationDepth,
                                   boolean useColors) {

        MutableComponent out = Component.empty();

        formatTag(
                out,
                tag,
                0,
                prettyPrint,
                spacesPerIndent,
                maxIndentationDepth,
                useColors
        );

        return out;
    }

    private static void formatTag(MutableComponent out,
                                  Tag tag,
                                  int indent,
                                  boolean pretty,
                                  int spaces,
                                  int maxDepth,
                                  boolean colors) {

        if (indent > maxDepth) {
            out.append(text("...", ChatFormatting.DARK_GRAY, colors));
            return;
        }

        switch (tag) {
            case CompoundTag compound -> formatCompound(out, compound, indent, pretty, spaces, maxDepth, colors);
            case ListTag list -> formatList(out, list, indent, pretty, spaces, maxDepth, colors);
            case ByteArrayTag arr -> formatArray(out, arr.getAsByteArray(), "B", colors);
            case IntArrayTag arr -> formatArray(out, arr.getAsIntArray(), "I", colors);
            case LongArrayTag arr -> formatArray(out, arr.getAsLongArray(), "L", colors);
            default -> formatPrimitive(out, tag, colors);
        }
    }

    private static void formatCompound(MutableComponent out,
                                       CompoundTag compound,
                                       int indent,
                                       boolean pretty,
                                       int spaces,
                                       int maxDepth,
                                       boolean colors) {

        out.append(text("{", ChatFormatting.GRAY, colors));

        if (compound.isEmpty()) {
            out.append(text("}", ChatFormatting.GRAY, colors));
            return;
        }

        if (pretty) out.append("\n");

        Iterator<Map.Entry<String, Tag>> it = compound.entrySet().iterator();

        while (it.hasNext()) {

            Map.Entry<String, Tag> entry = it.next();

            if (pretty) indent(out, indent + 1, spaces);

            out.append(text(entry.getKey(), ChatFormatting.AQUA, colors));
            out.append(text(": ", ChatFormatting.GRAY, colors));

            formatTag(
                    out,
                    entry.getValue(),
                    indent + 1,
                    pretty,
                    spaces,
                    maxDepth,
                    colors
            );

            if (it.hasNext()) out.append(text(",", ChatFormatting.GRAY, colors));

            if (pretty) out.append("\n");
        }

        if (pretty) indent(out, indent, spaces);

        out.append(text("}", ChatFormatting.GRAY, colors));
    }

    private static void formatList(MutableComponent out,
                                   ListTag list,
                                   int indent,
                                   boolean pretty,
                                   int spaces,
                                   int maxDepth,
                                   boolean colors) {

        out.append(text("[", ChatFormatting.GRAY, colors));

        if (list.isEmpty()) {
            out.append(text("]", ChatFormatting.GRAY, colors));
            return;
        }

        if (pretty) out.append("\n");

        for (int i = 0; i < list.size(); i++) {

            if (pretty) indent(out, indent + 1, spaces);

            formatTag(
                    out,
                    list.get(i),
                    indent + 1,
                    pretty,
                    spaces,
                    maxDepth,
                    colors
            );

            if (i < list.size() - 1) out.append(text(",", ChatFormatting.GRAY, colors));

            if (pretty) out.append("\n");
        }

        if (pretty) indent(out, indent, spaces);

        out.append(text("]", ChatFormatting.GRAY, colors));
    }

    private static void formatPrimitive(MutableComponent out,
                                        Tag tag,
                                        boolean colors) {
        switch (tag) {
            case ByteTag byteTag -> out.append(text(byteTag.byteValue() + "b", ChatFormatting.YELLOW, colors));
            case ShortTag shortTag ->
                    out.append(text(shortTag.shortValue() + "s", ChatFormatting.GOLD, colors));
            case IntTag intTag -> out.append(text(intTag.intValue() + "", ChatFormatting.DARK_PURPLE, colors));
            case LongTag longTag -> out.append(text(longTag.longValue() + "L", ChatFormatting.LIGHT_PURPLE, colors));
            case FloatTag floatTag ->
                    out.append(text(floatTag.floatValue() + "f", ChatFormatting.BLUE, colors));
            case DoubleTag doubleTag ->
                    out.append(text(doubleTag.doubleValue() + "d", ChatFormatting.DARK_AQUA, colors));
            case StringTag stringTag ->
                    out.append(text("\"" + stringTag.asString().orElse("null") + "\"", ChatFormatting.GREEN, colors));
            default -> out.append(text(tag.asString().orElse("null"), ChatFormatting.RED, colors));
        }
    }

    private static void formatArray(MutableComponent out,
                                    Object array,
                                    String prefix,
                                    boolean colors) {
        out.append(text("[" + prefix + "; ", ChatFormatting.GRAY, colors));
        if (array instanceof byte[] b) {
            for (int i = 0; i < b.length; i++) {
                out.append(text(Byte.toString(b[i]) + "b", ChatFormatting.YELLOW, colors));
                if (i < b.length - 1) out.append(text(", ", ChatFormatting.GRAY, colors));
            }
        } else if (array instanceof int[] b) {
            for (int i = 0; i < b.length; i++) {
                out.append(text(Integer.toString(b[i]), ChatFormatting.AQUA, colors));
                if (i < b.length - 1) out.append(text(", ", ChatFormatting.GRAY, colors));
            }
        } else if (array instanceof long[] b) {
            for (int i = 0; i < b.length; i++) {
                out.append(text(Long.toString(b[i]) + "L", ChatFormatting.LIGHT_PURPLE, colors));
                if (i < b.length - 1) out.append(text(", ", ChatFormatting.GRAY, colors));
            }
        }

        out.append(text("]", ChatFormatting.GRAY, colors));
    }

    private static void indent(MutableComponent out, int indent, int spaces) {

        for (int i = 0; i < indent * spaces; i++) {
            out.append(" ");
        }
    }

    private static Component text(String s, ChatFormatting color, boolean enabled) {

        if (!enabled) return Component.literal(s);

        return Component.literal(s).withStyle(color);
    }
}
