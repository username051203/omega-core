package com.omega.protocol.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.omega.protocol.db.OmegaRepository;
import com.omega.protocol.engine.RivalEngine;
import com.omega.protocol.model.*;
import java.util.*;
import java.util.concurrent.Executors;

public class SimulationViewModel extends AndroidViewModel {

    private final OmegaRepository repo;
    private final MutableLiveData<SimState> state = new MutableLiveData<>();
    public LiveData<SimState> getState() { return state; }

    private boolean running = false;
    private long simOffsetMs = 0;   // ms added to real time
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable ticker;

    // speed = simulated seconds per real second
    private int speedMultiplier = 900;

    public SimulationViewModel(@NonNull Application app) {
        super(app);
        repo = OmegaRepository.get(app);
    }

    public void setSpeed(int mult) { speedMultiplier = mult; }

    public void start(int startHour, int startMinute) {
        if (running) return;
        // Calculate offset so sim time = today at startHour:startMinute
        long nowMs   = System.currentTimeMillis();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, startHour);
        cal.set(java.util.Calendar.MINUTE, startMinute);
        cal.set(java.util.Calendar.SECOND, 0);
        long simStartMs = cal.getTimeInMillis();
        simOffsetMs = simStartMs - nowMs;
        running = true;
        final long[] lastWall = {nowMs};
        ticker = new Runnable() {
            @Override public void run() {
                if (!running) return;
                long wallNow = System.currentTimeMillis();
                long wallElapsed = wallNow - lastWall[0];
                lastWall[0] = wallNow;
                simOffsetMs += wallElapsed * (speedMultiplier - 1);
                tickSim();
                handler.postDelayed(this, 200);
            }
        };
        handler.post(ticker);
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(ticker);
    }

    public void reset() {
        stop();
        simOffsetMs = 0;
        SimState s = new SimState();
        s.running = false;
        s.timeLabel = "NOT RUNNING";
        state.postValue(s);
    }

    private void tickSim() {
        Executors.newSingleThreadExecutor().execute(() -> {
            long simMs = System.currentTimeMillis() + simOffsetMs;
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.setTimeInMillis(simMs);
            int h = c.get(java.util.Calendar.HOUR_OF_DAY);
            int m = c.get(java.util.Calendar.MINUTE);

            Routine rt              = repo.getRoutineSync();
            RivalState rival        = repo.getRivalSync();
            EngineConfig cfg        = repo.getEngineSync();
            List<ScheduleItem> all  = repo.getScheduleItemsSync();
            Map<String,List<String>> sched = repo.getScheduleMapSync();
            List<ScheduleItem> todayItems  = RivalEngine.todayItemsFrom(sched, all);

            // Tick rival using simulated hour
            float simHour = h + m / 60f;
            // Temporarily compute what rival would be at simHour
            float blocked = 0f;
            if (rt != null) {
                for (Routine.TimeRange r : rt.slp) blocked += simBlockedSoFar(r.s, r.e, simHour);
                for (Routine.TimeRange r : rt.bf)  blocked += simBlockedSoFar(r.s, r.e, simHour);
                for (float ml : rt.ml)             blocked += simBlockedSoFar(ml, ml+0.75f, simHour);
            }
            float aliciaHrs = Math.max(0f, simHour - blocked);
            int userPasses  = RivalEngine.userPassesToday(todayItems);
            float lead = userPasses - rival.cur;
            float boost = lead >= 2.5f ? 1.30f : lead >= 1.0f ? 1.15f : 1.0f;
            float rivalPasses = RivalEngine.aliciaPassCount(todayItems, aliciaHrs * boost);

            SimState ss = new SimState();
            ss.running      = running;
            ss.timeLabel    = String.format(Locale.US, "%02d:%02d", h, m);
            ss.simHour      = simHour;
            ss.userPasses   = userPasses;
            ss.rivalPasses  = rivalPasses;
            ss.anchorHour   = cfg.anchorHour;
            // Check anchor hit
            ss.anchorHit = Math.abs(simHour - cfg.anchorHour) < ((float)speedMultiplier / 3600f * 0.6f);
            state.postValue(ss);
        });
    }

    private float simBlockedSoFar(float s, float e, float elapsed) {
        if (e < s) { return Math.max(0,Math.min(24,elapsed)-s)+Math.max(0,Math.min(e,elapsed)); }
        return Math.max(0, Math.min(e, elapsed) - s);
    }

    @Override protected void onCleared() { stop(); super.onCleared(); }

    public static class SimState {
        public boolean running   = false;
        public boolean anchorHit = false;
        public String  timeLabel = "NOT RUNNING";
        public float   simHour   = 0f, anchorHour = 3.5f;
        public int     userPasses = 0;
        public float   rivalPasses = 0f;
    }
}
