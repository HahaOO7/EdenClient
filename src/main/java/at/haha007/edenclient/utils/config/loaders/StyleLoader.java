package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;

public class StyleLoader implements ConfigLoader<NbtCompound, Style> {
    @Override
    public NbtCompound save(Object value) {
        Style s = cast(value);
        NbtCompound tag = new NbtCompound();
        tag.putBoolean("bold", s.isBold());
        tag.putBoolean("obfuscated", s.isObfuscated());
        tag.putBoolean("italic", s.isItalic());
        tag.putBoolean("strikethrough", s.isStrikethrough());
        tag.putBoolean("underlined", s.isUnderlined());
        TextColor rgb = s.getColor();
        if (rgb != null)
            tag.putInt("rgb", rgb.getRgb());
        return tag;
    }

    @Override
    public Style load(NbtCompound tag) {
        Style style = Style.EMPTY;
        style.withBold(tag.getBoolean("bold"));
        style.obfuscated(tag.getBoolean("obfuscated"));
        style.withItalic(tag.getBoolean("italic"));
        style.withStrikethrough(tag.getBoolean("strikethrough"));
        style.withUnderline(tag.getBoolean("underlined"));
        if (tag.contains("rgb"))
            style.withColor(tag.getInt("rgb"));
        return style;
    }

    @Override
    public NbtCompound parse(String s) {
        return new NbtCompound();
    }
}
