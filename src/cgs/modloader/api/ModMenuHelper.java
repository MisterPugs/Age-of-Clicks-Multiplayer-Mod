package cgs.modloader.api;

import com.badlogic.gdx.graphics.Color;
import java.util.ArrayList;
import java.util.List;
import ru.ius.david.cgs.elements.ButtonElement;

public class ModMenuHelper {
    public static class ModMenuItem {
        public final String text;
        public final int iconIndex;
        public final Color bgColor;
        public final Color textColor;
        public final Runnable onClick;
        public final String tooltip;

        public ModMenuItem(String text, int iconIndex, Color bgColor, Color textColor, Runnable onClick, String tooltip) {
            this.text = text;
            this.iconIndex = iconIndex;
            this.bgColor = bgColor;
            this.textColor = textColor;
            this.onClick = onClick;
            this.tooltip = tooltip;
        }
    }

    private final List<ModMenuItem> customButtons = new ArrayList<ModMenuItem>();

    public void addMenuButton(String text, int iconIndex, Color bgColor, Color textColor, Runnable onClick) {
        addMenuButton(text, iconIndex, bgColor, textColor, onClick, null);
    }

    public void addMenuButton(String text, int iconIndex, Color bgColor, Color textColor, Runnable onClick, String tooltip) {
        customButtons.add(new ModMenuItem(text, iconIndex, bgColor, textColor, onClick, tooltip));
    }

    public List<ModMenuItem> getCustomButtons() {
        return customButtons;
    }
}
