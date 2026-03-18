package com.omega.protocol.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.omega.protocol.db.OmegaRepository;
import com.omega.protocol.engine.RivalEngine;
import com.omega.protocol.model.*;
import java.util.*;
import java.util.concurrent.Executors;

public class ScheduleViewModel extends AndroidViewModel {

    private final OmegaRepository repo;
    private final MutableLiveData<ScheduleState> state = new MutableLiveData<>();
    public LiveData<ScheduleState> getState() { return state; }

    public ScheduleViewModel(@NonNull Application app) {
        super(app);
        repo = OmegaRepository.get(app);
    }

    public void load() {
        Executors.newSingleThreadExecutor().execute(() -> {
            ScheduleState s         = new ScheduleState();
            s.allItems              = repo.getScheduleItemsSync();
            s.scheduleMap           = repo.getScheduleMapSync();
            s.subjects              = repo.getSubjectsSync();
            EngineConfig cfg        = repo.getEngineSync();
            s.dailyHrs              = cfg.dailyHrs;
            s.today                 = RivalEngine.today();
            state.postValue(s);
        });
    }

    public void addItemToPool(ScheduleItem item, Runnable onDone) {
        Executors.newSingleThreadExecutor().execute(() -> {
            repo.saveScheduleItem(item);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (onDone != null) onDone.run();
                load();
            });
        });
    }

    public void removeItem(String id, Runnable onDone) {
        Executors.newSingleThreadExecutor().execute(() -> {
            repo.deleteScheduleItem(id);
            // Also remove from all schedule days
            Map<String,List<String>> map = repo.getScheduleMapSync();
            for (Map.Entry<String,List<String>> e : map.entrySet())
                e.getValue().remove(id);
            repo.saveScheduleMap(map);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (onDone != null) onDone.run();
                load();
            });
        });
    }

    public void assignToDay(String itemId, String dayStr, Runnable onDone) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Map<String,List<String>> map = repo.getScheduleMapSync();
            List<String> ids = map.computeIfAbsent(dayStr, k -> new ArrayList<>());
            if (!ids.contains(itemId)) ids.add(itemId);
            repo.saveScheduleMap(map);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (onDone != null) onDone.run();
                load();
            });
        });
    }

    public void removeFromDay(String itemId, String dayStr, Runnable onDone) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Map<String,List<String>> map = repo.getScheduleMapSync();
            List<String> ids = map.get(dayStr);
            if (ids != null) { ids.remove(itemId); if (ids.isEmpty()) map.remove(dayStr); }
            repo.saveScheduleMap(map);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (onDone != null) onDone.run();
                load();
            });
        });
    }

    public void autoGenerate(Runnable onDone) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ScheduleItem> all = repo.getScheduleItemsSync();
            Map<String,List<String>> existing = repo.getScheduleMapSync();
            EngineConfig cfg = repo.getEngineSync();
            String fromDay = RivalEngine.addDays(RivalEngine.today(), 1);
            Map<String,List<String>> newMap = RivalEngine.generateSchedule(existing, all, cfg.dailyHrs, fromDay);
            repo.saveScheduleMap(newMap);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (onDone != null) onDone.run();
                load();
            });
        });
    }

    public static class ScheduleState {
        public List<ScheduleItem>        allItems    = new ArrayList<>();
        public Map<String, List<String>> scheduleMap = new HashMap<>();
        public List<Subject>             subjects    = new ArrayList<>();
        public float  dailyHrs = 8f;
        public String today    = RivalEngine.today();
    }
}
