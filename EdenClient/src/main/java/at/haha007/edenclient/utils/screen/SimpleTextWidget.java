package at.haha007.edenclient.utils.screen;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class SimpleTextWidget extends AbstractWidget {

    private final Font font;

    private int scrollOffset = 0;
    private List<FormattedCharSequence> lines;

    private SimpleTextWidget(int x, int y, int width, int height, Component component, Font font) {
        super(x, y, width, height, component);
        this.font = font;
        rebuildLines();
    }

    private void rebuildLines() {
        this.lines = font.split(message, width);
    }

    public static Builder builder(Component text, Font font) {
        return new Builder(text, font);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        int lineHeight = font.lineHeight;

        int visibleLines = height / lineHeight;
        int maxScroll = Math.max(0, lines.size() - visibleLines);

        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        guiGraphics.enableScissor(
                getX(),
                getY(),
                getX() + width,
                getY() + height
        );

        int yPos = getY();

        for (int i = scrollOffset; i < lines.size(); i++) {
            if ((i - scrollOffset) >= visibleLines) break;

            guiGraphics.drawString(
                    font,
                    lines.get(i),
                    getX(),
                    yPos,
                    0xFFFFFFFF
            );

            yPos += lineHeight;
        }

        guiGraphics.disableScissor();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isMouseOver(mouseX, mouseY)) {
            scrollOffset -= (int) scrollY;
            return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(@NonNull NarrationElementOutput output) {
    }

    public void setMessage(Component component) {
        this.message = component;
        rebuildLines();
    }

    public static class Builder {
        private int x = 0;
        private int y = 0;
        private int width = 100;
        private int height = 20;
        private Component component;
        private Font font;

        private Builder(Component text, Font font) {
            this.font = font;
            this.component = text;
        }

        public Builder x(int x) {
            this.x = x;
            return this;
        }

        public Builder y(int y) {
            this.y = y;
            return this;
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder height(int height) {
            this.height = height;
            return this;
        }

        public Builder component(Component component) {
            this.component = component;
            return this;
        }

        public Builder font(Font font) {
            this.font = font;
            return this;
        }

        public Builder bounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public SimpleTextWidget build() {
            if (font == null) {
                throw new IllegalStateException("Font is required");
            }
            return new SimpleTextWidget(x, y, width, height, component, font);
        }
    }
}
