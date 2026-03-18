package com.omega.protocol.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.omega.protocol.db.OmegaRepository;
import com.omega.protocol.db.entity.DaySnapshotEntity;
import com.omega.protocol.engine.RivalEngine;
import com.omega.protocol.model.*;
import java.util.*;
import java.util.concurrent.Executors;

public class DashboardViewModel extends AndroidViewModel {

    private final OmegaRepository repo;

    // Observed by DashboardFragment
    private final MutableLiveData<DashState> dashState = new MutableLiveData<>();
    public LiveData<DashState> getDashState() { return dashState; }

    public DashboardViewModel(@NonNull Application app) {
        super(app);
        repo = OmegaRepository.get(app);
    }

    /** Call this every tick (every 60s or on resume) */
    public void refresh() {
        Executors.newSingleThreadExecutor().execute(() -> {
            // Load everything from DB
            EngineConfig   cfg       = repo.getEngineSync();
            Routine        rt        = repo.getRoutineSync();
            RivalState     rival     = repo.getRivalSync();
            Profiles       profiles  = repo.getProfilesSync();
            List<ScheduleItem> all   = repo.getScheduleItemsSync();
            Map<String,List<String>> sched = repo.getScheduleMapSync();

            String today = RivalEngine.today();

            // Day rollover
            if (!today.equals(cfg.lastResetDay)) {
                if (cfg.lastResetDay != null && cfg.lastResetDay.compareTo(today) < 0) {
                    int uCh = repo.countDoneOnDay(cfg.lastResetDay);
                    List<ScheduleItem> prevItems = RivalEngine.todayItemsFrom(sched, all);
                    int totalPasses = 0; for (ScheduleItem it : prevItems) totalPasses += it.totalCount();
                    DaySnapshotEntity snap = RivalEngine.buildSnapshot(
                        cfg.lastResetDay, uCh, rival.cur, totalPasses,
                        rival.boost, rival.hrsWorked, RivalEngine.availableHrs(rt));
                    repo.saveSnapshot(snap);
                }
                cfg.lastResetDay = today;
                rival.cur = 0f; rival.hrsWorked = 0f; rival.boost = 1.0f;
                repo.saveEngineConfig(cfg);
            }

            // Get today's items and tick rival
            List<ScheduleItem> todayItems = RivalEngine.todayItemsFrom(sched, all);
            RivalEngine.tick(rival, rt, todayItems, cfg);
            repo.saveRivalState(rival);

            // Streak
            List<String> daysWithPasses = repo.getDaysWithPassesSync();
            int streak = RivalEngine.calcStreak(daysWithPasses);

            // Build state
            int userPasses  = RivalEngine.userPassesToday(todayItems);
            int totalPasses = 0; for (ScheduleItem it : todayItems) totalPasses += it.totalCount();
            float diff = userPasses - rival.cur;
            RivalEngine.AliciaStatus status = RivalEngine.status(todayItems, rt);

            // Determine which quip context
            String qCtx = todayItems.isEmpty() ? "startup"
                : diff > 0.05f ? "winning"
                : diff < -0.05f ? "losing"
                : "tie";

            DashState ds = new DashState();
            ds.timeLabel      = RivalEngine.nowTimeLabel();
            ds.anchorLabel    = RivalEngine.anchorLabel(cfg);
            ds.userPasses     = userPasses;
            ds.totalPasses    = totalPasses;
            ds.rivalPasses    = rival.cur;
            ds.rivalHrs       = rival.hrsWorked;
            ds.dailyBudget    = RivalEngine.availableHrs(rt);
            ds.rivalBoost     = rival.boost;
            ds.status         = status;
            ds.diff           = diff;
            ds.streak         = streak;
            ds.profiles       = profiles;
            ds.todayItems     = todayItems;
            ds.quip           = RivalEngine.quip(qCtx);
            ds.daysToExam     = RivalEngine.daysToExam(cfg);
            ds.examDate       = cfg.examDate;
            dashState.postValue(ds);
        });
    }

    /** Check if anchor hour passed → show EOD report */
    public void checkEndOfDay(Runnable onAnchorHit) {
        Executors.newSingleThreadExecutor().execute(() -> {
            EngineConfig cfg = repo.getEngineSync();
            float h = java.time.LocalTime.now().toSecondOfDay() / 3600f;
            if (Math.abs(h - cfg.anchorHour) < (1f / 60f)) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(onAnchorHit);
            }
        });
    }

    public void tickSubPass(String itemId, int passIdx, int spIdx, boolean val, Runnable onDone) {
        Executors.newSingleThreadExecutor().execute(() -> {
            ScheduleItem item = repo.getItemSync(itemId);
            if (item == null) return;
            item.passes.get(passIdx).subPasses.get(spIdx).done = val;
            repo.saveScheduleItem(item);
            com.omega.protocol.db.entity.TickLogEntity log = new com.omega.protocol.db.entity.TickLogEntity();
            log.itemId  = itemId; log.passIdx = passIdx; log.spIdx = spIdx;
            log.done    = val;
            log.iso     = java.time.LocalDateTime.now().toString();
            repo.insertTickLog(log);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (onDone != null) onDone.run();
                refresh();
            });
        });
    }

    // ── Flat state object passed to UI ─────────────────
    public static class DashState {
        public String timeLabel, anchorLabel, examDate, quip;
        public int    userPasses, totalPasses, streak;
        public float  rivalPasses, rivalHrs, dailyBudget, rivalBoost, diff;
        public long   daysToExam;
        public RivalEngine.AliciaStatus status;
        public Profiles profiles;
        public List<ScheduleItem> todayItems = new ArrayList<>();
    }
}
