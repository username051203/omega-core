package com.omega.protocol.worker;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.*;
import com.omega.protocol.db.OmegaRepository;
import com.omega.protocol.db.entity.*;
import com.omega.protocol.engine.RivalEngine;
import com.omega.protocol.model.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class RivalTickWorker extends Worker {

    public static final String TAG = "rival_tick";

    public RivalTickWorker(@NonNull Context ctx, @NonNull WorkerParameters p) { super(ctx, p); }

    @NonNull @Override
    public Result doWork() {
        OmegaRepository repo = OmegaRepository.get(getApplicationContext());

        EngineConfig cfg   = repo.getEngineSync();
        Routine rt         = repo.getRoutineSync();
        RivalState rival   = repo.getRivalSync();
        List<ScheduleItem> all  = repo.getScheduleItemsSync();
        Map<String,List<String>> sched = repo.getScheduleMapSync();

        String today = RivalEngine.today();

        // Day rollover
        if (!today.equals(cfg.lastResetDay)) {
            if (cfg.lastResetDay != null && cfg.lastResetDay.compareTo(today) < 0) {
                int uCh = repo.countDoneOnDay(cfg.lastResetDay);
                List<ScheduleItem> prev = RivalEngine.todayItemsFrom(sched, all);
                int totalPasses = 0; for (ScheduleItem it : prev) totalPasses += it.totalCount();
                DaySnapshotEntity snap = RivalEngine.buildSnapshot(
                    cfg.lastResetDay, uCh, rival.cur, totalPasses,
                    rival.boost, rival.hrsWorked, RivalEngine.availableHrs(rt));
                repo.saveSnapshot(snap);
            }
            cfg.lastResetDay = today;
            rival.cur = 0f; rival.hrsWorked = 0f; rival.boost = 1.0f;
            repo.saveEngineConfig(cfg);
        }

        List<ScheduleItem> todayItems = RivalEngine.todayItemsFrom(sched, all);
        RivalEngine.tick(rival, rt, todayItems, cfg);
        repo.saveRivalState(rival);

        return Result.success();
    }

    /** Schedule a periodic tick every 15 minutes. */
    public static void schedule(Context ctx) {
        PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(
            RivalTickWorker.class, 15, TimeUnit.MINUTES)
            .addTag(TAG)
            .setConstraints(new Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .build())
            .build();
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            req);
    }
}
