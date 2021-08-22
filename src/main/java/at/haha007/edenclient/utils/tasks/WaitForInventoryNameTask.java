package at.haha007.edenclient.utils.tasks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.text.Text;

import java.util.regex.Pattern;

public class WaitForInventoryNameTask extends WaitingTask {
    public WaitForInventoryNameTask(Pattern pattern) {
        super(() -> {
            Screen sc = MinecraftClient.getInstance().currentScreen;
            if (sc == null) return false;
            if(!(sc instanceof GenericContainerScreen))return false;
            Text title = sc.getTitle();
            if (title == null) return false;
            return pattern.matcher(title.getString()).matches();
        });
    }
}
