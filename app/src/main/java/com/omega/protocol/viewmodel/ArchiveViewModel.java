package com.omega.protocol.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.omega.protocol.db.OmegaRepository;
import com.omega.protocol.db.entity.DaySnapshotEntity;
import java.util.List;
import java.util.concurrent.Executors;

public class ArchiveViewModel extends AndroidViewModel {

    private final OmegaRepository repo;
    private final MutableLiveData<List<DaySnapshotEntity>> snapshots = new MutableLiveData<>();
    public LiveData<List<DaySnapshotEntity>> getSnapshots() { return snapshots; }

    public ArchiveViewModel(@NonNull Application app) {
        super(app);
        repo = OmegaRepository.get(app);
    }

    public void load() {
        Executors.newSingleThreadExecutor().execute(() ->
            snapshots.postValue(repo.getAllSnapshotsSync()));
    }

    public void deleteSnapshot(String dayStr, Runnable onDone) {
        Executors.newSingleThreadExecutor().execute(() -> {
            repo.deleteSnapshot(dayStr);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (onDone != null) onDone.run();
                load();
            });
        });
    }
}
