package at.haha007.edenclient.render;

import at.haha007.edenclient.utils.EdenRenderUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Accessors(fluent = true)
public class LineRenderer implements Renderer {
    boolean enabled = true;
    private EdenColor color = new EdenColor(1f, 0f, 1f, 1f);
    private float width = 1f;
    private EdenRenderUtils.DepthTest depthTest = EdenRenderUtils.DepthTest.DEPTH_TESTED;
    private List<Line> lines = new ArrayList<>();


    public void render() {
        if(!enabled) {
            return;
        }
        EdenRenderUtils.drawLines(lines, color, width, depthTest);
    }

    public void render(List<Line> lines) {
        if(!enabled) {
            return;
        }
        EdenRenderUtils.drawLines(lines, color, width, depthTest);
    }
}
