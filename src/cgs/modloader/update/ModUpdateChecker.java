package cgs.modloader.update;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ModUpdateChecker {
    public static final String CURRENT_VERSION = "1.1.0";
    public static final int CURRENT_VERSION_CODE = 110;
    public static final String GITHUB_REPO = "MisterPugs/Age-of-Clicks-Multiplayer-Mod";
    public static final String VERSION_URL = "https://raw.githubusercontent.com/MisterPugs/Age-of-Clicks-Multiplayer-Mod/main/version.json";
    public static final String RELEASES_URL = "https://github.com/MisterPugs/Age-of-Clicks-Multiplayer-Mod/releases/latest";

    private static final ModUpdateChecker INSTANCE = new ModUpdateChecker();
    public static ModUpdateChecker getInstance() { return INSTANCE; }

    private boolean checked = false;
    private boolean checking = false;
    private boolean updateAvailable = false;
    private String remoteVersion = "";
    private int remoteVersionCode = 0;
    private String changelog = "";
    private String downloadUrl = RELEASES_URL;
    private boolean dismissed = false;

    private final GlyphLayout glyphLayout = new GlyphLayout();
    private float pulseTimer = 0.0f;

    private ModUpdateChecker() {}

    public void checkForUpdatesAsync() {
        if (checked || checking) return;
        checking = true;

        Thread t = new Thread(() -> {
            try {
                URL url = new URL(VERSION_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3500);
                conn.setReadTimeout(3500);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "AgeOfClicksModLoader/" + CURRENT_VERSION);

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    parseVersionJson(sb.toString());
                }
            } catch (Exception e) {
                Gdx.app.log("ModUpdateChecker", "Update check failed (offline or repo unreleased): " + e.getMessage());
            } finally {
                checking = false;
                checked = true;
            }
        }, "ModUpdateChecker-Thread");
        t.setDaemon(true);
        t.start();
    }

    private void parseVersionJson(String json) {
        try {
            this.remoteVersion = extractJsonString(json, "version");
            this.remoteVersionCode = extractJsonInt(json, "versionCode");
            this.changelog = extractJsonString(json, "changelog");
            String dl = extractJsonString(json, "releaseUrl");
            if (dl != null && !dl.isEmpty()) {
                this.downloadUrl = dl;
            }

            if (this.remoteVersionCode > CURRENT_VERSION_CODE) {
                this.updateAvailable = true;
                Gdx.app.log("ModUpdateChecker", "⚡ New update available: v" + this.remoteVersion + " (Current: v" + CURRENT_VERSION + ")");
            }
        } catch (Exception e) {
            Gdx.app.log("ModUpdateChecker", "Parse error: " + e.getMessage());
        }
    }

    private static String extractJsonString(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx == -1) return "";
        int colon = json.indexOf(':', idx);
        if (colon == -1) return "";
        int firstQuote = json.indexOf('"', colon);
        if (firstQuote == -1) return "";
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (secondQuote == -1) return "";
        return json.substring(firstQuote + 1, secondQuote);
    }

    private static int extractJsonInt(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx == -1) return 0;
        int colon = json.indexOf(':', idx);
        if (colon == -1) return 0;
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        if (start < end) {
            try {
                return Integer.parseInt(json.substring(start, end));
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    public boolean isUpdateAvailable() {
        return updateAvailable && !dismissed;
    }

    public String getRemoteVersion() {
        return remoteVersion;
    }

    public String getChangelog() {
        return changelog;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void dismiss() {
        this.dismissed = true;
    }

    public void openDownloadPage() {
        try {
            Gdx.net.openURI(downloadUrl);
        } catch (Exception e) {
            Gdx.app.log("ModUpdateChecker", "Failed to open browser: " + e.getMessage());
        }
    }

    public boolean handleClick(float mouseX, float mouseY, float screenWidth, float screenHeight) {
        if (!isUpdateAvailable()) return false;
        float bannerW = 460;
        float bannerH = 46;
        float x = (screenWidth - bannerW) / 2.0f;
        float y = screenHeight - bannerH - 12;

        if (mouseX >= x && mouseX <= x + bannerW && mouseY >= y && mouseY <= y + bannerH) {
            openDownloadPage();
            return true;
        }
        return false;
    }

    public void renderBanner(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font, float delta, float screenWidth, float screenHeight) {
        if (!isUpdateAvailable()) return;
        pulseTimer += delta;

        float bannerW = 460;
        float bannerH = 46;
        float x = (screenWidth - bannerW) / 2.0f;
        float y = screenHeight - bannerH - 12;

        float glow = 0.7f + 0.3f * (float) Math.sin(pulseTimer * 4.0);

        if (shapes != null) {
            batch.end();
            shapes.setProjectionMatrix(batch.getProjectionMatrix());
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(0.08f, 0.12f, 0.22f, 0.95f);
            shapes.rect(x, y, bannerW, bannerH);
            shapes.end();

            shapes.begin(ShapeRenderer.ShapeType.Line);
            shapes.setColor(0.1f, 0.8f * glow, 1.0f * glow, 1.0f);
            shapes.rect(x, y, bannerW, bannerH);
            shapes.rect(x + 1, y + 1, bannerW - 2, bannerH - 2);
            shapes.end();
            batch.begin();
        }

        if (font != null) {
            font.setColor(0.3f, 0.95f, 1.0f, 1.0f);
            String title = "⚡ ДОСТУПНО ОБНОВЛЕНИЕ МОДА v" + remoteVersion + "! [НАЖМИТЕ ЗДЕСЬ]";
            glyphLayout.setText(font, title);
            font.draw(batch, title, x + (bannerW - glyphLayout.width) / 2.0f, y + bannerH - 10);

            font.setColor(0.8f, 0.85f, 0.9f, 0.85f);
            String sub = changelog.isEmpty() ? "Нажмите для перехода на страницу загрузки GitHub" : changelog;
            if (sub.length() > 55) sub = sub.substring(0, 52) + "...";
            glyphLayout.setText(font, sub);
            font.draw(batch, sub, x + (bannerW - glyphLayout.width) / 2.0f, y + 16);
            font.setColor(Color.WHITE);
        }
    }
}
