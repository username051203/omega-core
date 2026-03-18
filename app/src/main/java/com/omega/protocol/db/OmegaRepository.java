package com.omega.protocol.db;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import com.google.gson.Gson;
import com.omega.protocol.db.entity.*;
import com.omega.protocol.engine.RivalEngine;
import com.omega.protocol.model.*;
import com.omega.protocol.util.MicroSyllabus;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

public class OmegaRepository {

    private static volatile OmegaRepository INSTANCE;
    private final OmegaDatabase db;
    private final Gson gson = new Gson();
    private final ExecutorService io = Executors.newFixedThreadPool(3);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /* ---------- singleton ---------- */
    public static OmegaRepository get(Context ctx) {
        if (INSTANCE == null) {
            synchronized (OmegaRepository.class) {
                if (INSTANCE == null) INSTANCE = new OmegaRepository(ctx);
            }
        }
        return INSTANCE;
    }

    private OmegaRepository(Context ctx) {
        db = OmegaDatabase.get(ctx);
        ensureDefaults();
    }

    /* ====================================================
       INITIALIZATION
    ==================================================== */
    private void ensureDefaults() {
        io.execute(() -> {
            // Inject built-in microbiology syllabus if not present
            SubjectEntity existing = db.subjectDao().getById(MicroSyllabus.SUBJECT_ID);
            if (existing == null) {
                Subject s = MicroSyllabus.build();
                SubjectEntity se = new SubjectEntity();
                se.id   = s.id;
                se.name = s.name;
                se.setChapters(s.chapters);
                db.subjectDao().insert(se);
            }
            // Ensure config row exists
            if (db.appConfigDao().get() == null) {
                db.appConfigDao().upsert(defaultConfig());
            }
        });
    }

    /* ====================================================
       CONFIG / ENGINE / ROUTINE / PROFILES
    ==================================================== */
    public interface ConfigCallback { void onResult(AppConfigEntity cfg); }

    public void getConfig(ConfigCallback cb) {
        io.execute(() -> {
            AppConfigEntity cfg = db.appConfigDao().get();
            if (cfg == null) cfg = defaultConfig();
            AppConfigEntity finalCfg = cfg;
            new Handler(Looper.getMainLooper()).post(() -> cb.onResult(finalCfg));
        });
    }

    public void saveConfig(AppConfigEntity cfg) {
        io.execute(() -> db.appConfigDao().upsert(cfg));
    }

    public EngineConfig getEngineSync() {
        AppConfigEntity cfg = db.appConfigDao().get();
        if (cfg == null || cfg.engineJson == null) return new EngineConfig();
        return gson.fromJson(cfg.engineJson, EngineConfig.class);
    }

    public Routine getRoutineSync() {
        AppConfigEntity cfg = db.appConfigDao().get();
        if (cfg == null || cfg.routineJson == null) return new Routine();
        return gson.fromJson(cfg.routineJson, Routine.class);
    }

    public RivalState getRivalSync() {
        AppConfigEntity cfg = db.appConfigDao().get();
        if (cfg == null || cfg.rivalJson == null) return new RivalState();
        return gson.fromJson(cfg.rivalJson, RivalState.class);
    }

    public Profiles getProfilesSync() {
        AppConfigEntity cfg = db.appConfigDao().get();
        if (cfg == null || cfg.profilesJson == null) return new Profiles();
        Profiles p = gson.fromJson(cfg.profilesJson, Profiles.class);
        return p != null ? p : new Profiles();
    }

    public void saveEngineConfig(EngineConfig ec) {
        io.execute(() -> {
            AppConfigEntity cfg = db.appConfigDao().get();
            if (cfg == null) cfg = defaultConfig();
            cfg.engineJson = gson.toJson(ec);
            db.appConfigDao().upsert(cfg);
        });
    }

    public void saveRoutine(Routine rt) {
        io.execute(() -> {
            AppConfigEntity cfg = db.appConfigDao().get();
            if (cfg == null) cfg = defaultConfig();
            cfg.routineJson = gson.toJson(rt);
            db.appConfigDao().upsert(cfg);
        });
    }

    public void saveRivalState(RivalState rs) {
        io.execute(() -> {
            AppConfigEntity cfg = db.appConfigDao().get();
            if (cfg == null) cfg = defaultConfig();
            cfg.rivalJson = gson.toJson(rs);
            db.appConfigDao().upsert(cfg);
        });
    }

    public void saveProfiles(Profiles p) {
        io.execute(() -> {
            AppConfigEntity cfg = db.appConfigDao().get();
            if (cfg == null) cfg = defaultConfig();
            cfg.profilesJson = gson.toJson(p);
            db.appConfigDao().upsert(cfg);
        });
    }

    /* ====================================================
       SCHEDULE ITEMS
    ==================================================== */
    public List<ScheduleItem> getScheduleItemsSync() {
        List<ScheduleItemEntity> rows = db.scheduleItemDao().getAll();
        List<ScheduleItem> result = new ArrayList<>();
        for (ScheduleItemEntity row : rows) result.add(toModel(row));
        return result;
    }

    public LiveData<List<ScheduleItemEntity>> getScheduleItemsLive() {
        return db.scheduleItemDao().getAllLive();
    }

    public ScheduleItem getItemSync(String id) {
        ScheduleItemEntity e = db.scheduleItemDao().getById(id);
        return e != null ? toModel(e) : null;
    }

    public void saveScheduleItem(ScheduleItem item) {
        io.execute(() -> db.scheduleItemDao().insert(toEntity(item)));
    }

    public void deleteScheduleItem(String id) {
        io.execute(() -> db.scheduleItemDao().deleteById(id));
    }

    public void saveAllScheduleItems(List<ScheduleItem> items) {
        io.execute(() -> {
            db.scheduleItemDao().deleteAll();
            List<ScheduleItemEntity> rows = new ArrayList<>();
            for (ScheduleItem it : items) rows.add(toEntity(it));
            db.scheduleItemDao().insertAll(rows);
        });
    }

    /* ====================================================
       SCHEDULE DAYS
    ==================================================== */
    public Map<String, List<String>> getScheduleMapSync() {
        List<ScheduleDayEntity> rows = db.scheduleDayDao().getAll();
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (ScheduleDayEntity row : rows) map.put(row.dayStr, row.getItemIds());
        return map;
    }

    public void saveScheduleMap(Map<String, List<String>> map) {
        io.execute(() -> {
            List<ScheduleDayEntity> rows = new ArrayList<>();
            for (Map.Entry<String, List<String>> e : map.entrySet()) {
                ScheduleDayEntity row = new ScheduleDayEntity();
                row.dayStr = e.getKey();
                row.setItemIds(e.getValue());
                rows.add(row);
            }
            db.scheduleDayDao().deleteAll();
            db.scheduleDayDao().insertAll(rows);
        });
    }

    public void saveScheduleDay(String dayStr, List<String> itemIds) {
        io.execute(() -> {
            ScheduleDayEntity row = new ScheduleDayEntity();
            row.dayStr = dayStr;
            row.setItemIds(itemIds);
            db.scheduleDayDao().insert(row);
        });
    }

    public void clearFutureDays(String fromDay) {
        io.execute(() -> db.scheduleDayDao().deleteFutureFrom(fromDay));
    }

    /* ====================================================
       TICK LOGS
    ==================================================== */
    public void insertTickLog(TickLogEntity log) {
        io.execute(() -> db.tickLogDao().insert(log));
    }

    public List<TickLogEntity> getRecentLogsSync() {
        return db.tickLogDao().getRecent();
    }

    public int countDoneOnDay(String dayStr) {
        return db.tickLogDao().countDoneOnDay(dayStr);
    }

    public List<String> getDaysWithPassesSync() {
        return db.tickLogDao().getDaysWithPasses();
    }

    /* ====================================================
       DAY SNAPSHOTS (archive)
    ==================================================== */
    public LiveData<List<DaySnapshotEntity>> getAllSnapshotsLive() {
        return db.daySnapshotDao().getAllLive();
    }

    public List<DaySnapshotEntity> getAllSnapshotsSync() {
        return db.daySnapshotDao().getAll();
    }

    public void saveSnapshot(DaySnapshotEntity snap) {
        io.execute(() -> db.daySnapshotDao().insert(snap));
    }

    public void deleteSnapshot(String dayStr) {
        io.execute(() -> db.daySnapshotDao().deleteByDay(dayStr));
    }

    /* ====================================================
       SUBJECTS
    ==================================================== */
    public LiveData<List<SubjectEntity>> getSubjectsLive() {
        return db.subjectDao().getAllLive();
    }

    public List<Subject> getSubjectsSync() {
        List<SubjectEntity> rows = db.subjectDao().getAll();
        List<Subject> result = new ArrayList<>();
        for (SubjectEntity row : rows) {
            Subject s = new Subject(row.id, row.name);
            s.chapters = row.getChapters();
            result.add(s);
        }
        return result;
    }

    public void saveSubject(Subject s) {
        io.execute(() -> {
            SubjectEntity se = new SubjectEntity();
            se.id   = s.id;
            se.name = s.name;
            se.setChapters(s.chapters);
            db.subjectDao().insert(se);
        });
    }

    public void deleteSubject(String id) {
        io.execute(() -> db.subjectDao().deleteById(id));
    }

    /* ====================================================
       FULL EXPORT / IMPORT (Kernel Sync)
    ==================================================== */
    public interface ExportCallback { void onDone(String json); }

    public void exportAll(ExportCallback cb) {
        io.execute(() -> {
            ExportBundle bundle = new ExportBundle();
            bundle.scheduleItems   = getScheduleItemsSync();
            bundle.scheduleMap     = getScheduleMapSync();
            bundle.subjects        = getSubjectsSync();
            bundle.logs            = getRecentLogsSync();
            bundle.snapshots       = getAllSnapshotsSync();
            bundle.engineJson      = db.appConfigDao().get() != null ? db.appConfigDao().get().engineJson : null;
            bundle.routineJson     = db.appConfigDao().get() != null ? db.appConfigDao().get().routineJson : null;
            bundle.rivalJson       = db.appConfigDao().get() != null ? db.appConfigDao().get().rivalJson : null;
            bundle.profilesJson    = db.appConfigDao().get() != null ? db.appConfigDao().get().profilesJson : null;
            String json = gson.toJson(bundle);
            new Handler(Looper.getMainLooper()).post(() -> cb.onDone(json));
        });
    }

    public interface ImportCallback { void onDone(boolean success); }

    public void importAll(String json, ImportCallback cb) {
        io.execute(() -> {
            try {
                ExportBundle bundle = gson.fromJson(json, ExportBundle.class);
                if (bundle == null) { post(cb, false); return; }
                // Wipe everything
                db.scheduleItemDao().deleteAll();
                db.scheduleDayDao().deleteAll();
                db.tickLogDao().deleteAll();
                db.daySnapshotDao().deleteAll();
                db.subjectDao().deleteAll();
                // Re-insert
                if (bundle.scheduleItems != null)
                    for (ScheduleItem it : bundle.scheduleItems)
                        db.scheduleItemDao().insert(toEntity(it));
                if (bundle.scheduleMap != null) {
                    for (Map.Entry<String, List<String>> e : bundle.scheduleMap.entrySet()) {
                        ScheduleDayEntity row = new ScheduleDayEntity();
                        row.dayStr = e.getKey();
                        row.setItemIds(e.getValue());
                        db.scheduleDayDao().insert(row);
                    }
                }
                if (bundle.subjects != null)
                    for (Subject s : bundle.subjects) {
                        SubjectEntity se = new SubjectEntity();
                        se.id = s.id; se.name = s.name; se.setChapters(s.chapters);
                        db.subjectDao().insert(se);
                    }
                if (bundle.logs != null)
                    for (TickLogEntity l : bundle.logs) db.tickLogDao().insert(l);
                if (bundle.snapshots != null)
                    for (DaySnapshotEntity sn : bundle.snapshots) db.daySnapshotDao().insert(sn);
                AppConfigEntity cfg = defaultConfig();
                if (bundle.engineJson   != null) cfg.engineJson   = bundle.engineJson;
                if (bundle.routineJson  != null) cfg.routineJson  = bundle.routineJson;
                if (bundle.rivalJson    != null) cfg.rivalJson    = bundle.rivalJson;
                if (bundle.profilesJson != null) cfg.profilesJson = bundle.profilesJson;
                db.appConfigDao().upsert(cfg);
                ensureDefaults();
                post(cb, true);
            } catch (Exception e) { post(cb, false); }
        });
    }

    public void hardReset(Runnable onDone) {
        io.execute(() -> {
            db.scheduleItemDao().deleteAll();
            db.scheduleDayDao().deleteAll();
            db.tickLogDao().deleteAll();
            db.daySnapshotDao().deleteAll();
            db.subjectDao().deleteAll();
            db.appConfigDao().upsert(defaultConfig());
            ensureDefaults();
            new Handler(Looper.getMainLooper()).post(onDone);
        });
    }

    /* ====================================================
       HELPERS
    ==================================================== */
    private void post(ImportCallback cb, boolean v) {
        new Handler(Looper.getMainLooper()).post(() -> cb.onDone(v));
    }

    private AppConfigEntity defaultConfig() {
        AppConfigEntity c = new AppConfigEntity();
        c.engineJson   = gson.toJson(new EngineConfig());
        c.routineJson  = gson.toJson(new Routine());
        c.rivalJson    = gson.toJson(new RivalState());
        c.profilesJson = gson.toJson(new Profiles());
        return c;
    }

    private ScheduleItem toModel(ScheduleItemEntity e) {
        ScheduleItem it = new ScheduleItem();
        it.id        = e.id;
        it.subjectId = e.subjectId;
        it.chapterId = e.chapterId;
        it.topicId   = e.topicId;
        it.priority  = e.priority;
        it.part      = e.part;
        it.pinDate   = e.pinDate;
        it.passes    = e.getPasses();
        return it;
    }

    private ScheduleItemEntity toEntity(ScheduleItem it) {
        ScheduleItemEntity e = new ScheduleItemEntity();
        e.id        = it.id;
        e.subjectId = it.subjectId;
        e.chapterId = it.chapterId;
        e.topicId   = it.topicId;
        e.priority  = it.priority;
        e.part      = it.part;
        e.pinDate   = it.pinDate;
        e.setPasses(it.passes);
        return e;
    }

    /* ====================================================
       EXPORT BUNDLE
    ==================================================== */
    public static class ExportBundle {
        public List<ScheduleItem>       scheduleItems;
        public Map<String, List<String>> scheduleMap;
        public List<Subject>            subjects;
        public List<TickLogEntity>      logs;
        public List<DaySnapshotEntity>  snapshots;
        public String engineJson, routineJson, rivalJson, profilesJson;
    }
}
