package com.omega.protocol.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.omega.protocol.db.OmegaRepository;
import com.omega.protocol.model.*;
import java.util.concurrent.Executors;

public class ProfileViewModel extends AndroidViewModel {

    private final OmegaRepository repo;
    private final MutableLiveData<ProfileState> state = new MutableLiveData<>();
    public LiveData<ProfileState> getState() { return state; }

    public ProfileViewModel(@NonNull Application app) {
        super(app);
        repo = OmegaRepository.get(app);
    }

    public void load() {
        Executors.newSingleThreadExecutor().execute(() -> {
            ProfileState s    = new ProfileState();
            s.profiles        = repo.getProfilesSync();
            s.engine          = repo.getEngineSync();
            s.routine         = repo.getRoutineSync();
            state.postValue(s);
        });
    }

    public void saveProfiles(Profiles p, Runnable onDone) {
        Executors.newSingleThreadExecutor().execute(() -> {
            repo.saveProfiles(p);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (onDone != null) onDone.run(); load();
            });
        });
    }

    public void saveEngine(EngineConfig ec, Runnable onDone) {
        Executors.newSingleThreadExecutor().execute(() -> {
            repo.saveEngineConfig(ec);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (onDone != null) onDone.run(); load();
            });
        });
    }

    public void saveRoutine(Routine rt, Runnable onDone) {
        Executors.newSingleThreadExecutor().execute(() -> {
            repo.saveRoutine(rt);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (onDone != null) onDone.run(); load();
            });
        });
    }

    public static class ProfileState {
        public Profiles    profiles;
        public EngineConfig engine;
        public Routine      routine;
    }
}
