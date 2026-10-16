package cgs.modloader.timeline;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.ius.david.cgs.game.CountryState;
import ru.ius.david.cgs.game.GameManager;

public class TimelineRecorder {
    private static final TimelineRecorder INSTANCE = new TimelineRecorder();
    public static TimelineRecorder getInstance() { return INSTANCE; }

    public static class Snapshot {
        public final int year;
        public final int month;
        public final Map<String, Integer> territories;
        public final int warsCount;

        public Snapshot(int year, int month, Map<String, Integer> territories, int warsCount) {
            this.year = year;
            this.month = month;
            this.territories = territories;
            this.warsCount = warsCount;
        }
    }

    private final List<Snapshot> snapshots = new ArrayList<Snapshot>();
    private float tickTimer = 0.0f;
    private static final float RECORD_INTERVAL_SECONDS = 10.0f;

    private TimelineRecorder() {}

    public void reset() {
        snapshots.clear();
        tickTimer = 0.0f;
    }

    public void update(float delta, GameManager gm) {
        if (gm == null || gm.getWorldCountries() == null) return;
        tickTimer += delta;
        if (tickTimer >= RECORD_INTERVAL_SECONDS) {
            tickTimer = 0.0f;
            recordSnapshot(gm);
        }
    }

    private void recordSnapshot(GameManager gm) {
        Map<String, Integer> lands = new HashMap<String, Integer>();
        for (Map.Entry<String, CountryState> entry : gm.getWorldCountries().entrySet()) {
            if (entry.getValue() != null) {
                lands.put(entry.getKey(), (int)entry.getValue().getMilitaryCount());
            }
        }
        int wars = (gm.getWarManager() != null && gm.getWarManager().getActiveWars() != null) ?
                   gm.getWarManager().getActiveWars().size() : 0;

        snapshots.add(new Snapshot(gm.getCurrentYear(), gm.getGameDays(), lands, wars));
    }

    public List<Snapshot> getSnapshots() {
        return snapshots;
    }
}
