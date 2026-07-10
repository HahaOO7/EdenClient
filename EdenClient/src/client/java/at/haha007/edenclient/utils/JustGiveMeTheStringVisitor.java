package at.haha007.edenclient.utils;

import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;

public class JustGiveMeTheStringVisitor implements FormattedCharSink {
    final StringBuilder sb = new StringBuilder();

    @Override
    public boolean accept(int index, Style style, int codePoint) {
        sb.appendCodePoint(codePoint);
        return true;
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
