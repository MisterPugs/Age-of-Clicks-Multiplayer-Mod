package ru.ius.david.cgs;

import android.os.Bundle;
import android.view.WindowManager;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import ru.ius.david.cgs.platform.NoOpSecurityService;

public class AndroidLauncher extends AndroidApplication {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } catch (Throwable ignored) {}

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useImmersiveMode = true;
        config.useGL30 = false;
        config.useAccelerometer = false;
        config.useCompass = false;

        try {
            cgs.modloader.ModLoader.getInstance().init();
        } catch (Throwable t) {
            System.err.println("[AndroidLauncher] ModLoader init error: " + t.getMessage());
        }

        cgsGame game = new cgsGame();
        game.setSecurityService(new NoOpSecurityService());
        initialize(game, config);
    }
}
