package cgs.modloader.scripting;

import cgs.modloader.ModLoader;
import cgs.mods.multiplayer.chat.MultiplayerChat;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;
import ru.ius.david.cgs.game.GameManager;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class LuaModEngine {
    private static final LuaModEngine INSTANCE = new LuaModEngine();
    public static LuaModEngine getInstance() { return INSTANCE; }

    public static class ActiveLuaScript {
        public final String modName;
        public final File scriptFile;
        public Globals globals;
        public LuaValue onTickFunc;
        public LuaValue onInitFunc;

        public ActiveLuaScript(String modName, File file) {
            this.modName = modName;
            this.scriptFile = file;
        }
    }

    private final List<ActiveLuaScript> scripts = new ArrayList<ActiveLuaScript>();

    private LuaModEngine() {}

    public void loadScriptFromModDir(String modName, File dir) {
        File luaFile = new File(dir, "main.lua");
        if (!luaFile.exists()) {
            luaFile = new File(dir, "script.lua");
        }
        if (!luaFile.exists()) return;

        try {
            Globals globals = JsePlatform.standardGlobals();
            ActiveLuaScript script = new ActiveLuaScript(modName, luaFile);
            script.globals = globals;

            bindApi(globals, script);

            String code = new String(Files.readAllBytes(luaFile.toPath()), StandardCharsets.UTF_8);
            LuaValue chunk = globals.load(code);
            chunk.call();

            script.onInitFunc = globals.get("onInit");
            if (script.onInitFunc != null && script.onInitFunc.isfunction()) {
                script.onInitFunc.call();
            }

            script.onTickFunc = globals.get("onTick");

            scripts.add(script);
            System.out.println("[LuaEngine] Successfully loaded Lua script from mod: " + modName + " (" + luaFile.getName() + ")");
        } catch (Throwable t) {
            System.err.println("[LuaEngine] Error loading Lua script in " + modName + ": " + t.getMessage());
            t.printStackTrace();
        }
    }

    private void bindApi(Globals globals, final ActiveLuaScript script) {
        LuaTable cgsTable = new LuaTable();

        // cgs.log(text)
        cgsTable.set("log", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                System.out.println("[Lua: " + script.modName + "] " + arg.tojstring());
                return LuaValue.NIL;
            }
        });

        // cgs.chat(text)
        cgsTable.set("chat", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                MultiplayerChat.getInstance().addSystemMessage("[" + script.modName + "] " + arg.tojstring());
                return LuaValue.NIL;
            }
        });

        // cgs.getYear()
        cgsTable.set("getYear", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                GameManager gm = GameManager.getInstance();
                return LuaValue.valueOf(gm != null ? gm.getCurrentYear() : 0);
            }
        });

        globals.set("cgs", cgsTable);
    }

    public void onGameTick(GameManager gm) {
        for (ActiveLuaScript s : scripts) {
            if (s.onTickFunc != null && s.onTickFunc.isfunction()) {
                try {
                    s.onTickFunc.call();
                } catch (Throwable t) {
                    // silently handle
                }
            }
        }
    }

    public void reload() {
        scripts.clear();
        File modsDir = new File(ModLoader.MODS_DIR_NAME);
        File[] files = modsDir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                loadScriptFromModDir(f.getName(), f);
            }
        }
        System.out.println("[LuaEngine] Reloaded all Lua scripts! Active: " + scripts.size());
    }
}
