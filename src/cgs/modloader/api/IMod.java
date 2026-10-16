package cgs.modloader.api;

import ru.ius.david.cgs.MainMenuScreen;
import ru.ius.david.cgs.cgsGame;
import ru.ius.david.cgs.game.GameManager;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public interface IMod {
    void onInitialize(ModContext context);
    default void onGameCreated(cgsGame game) {}
    default void onMainMenuBuild(MainMenuScreen screen, ModMenuHelper helper) {}
    default void onGameTick(GameManager gameManager) {}
    default void onRenderOverlay(SpriteBatch batch, float delta) {}
}
