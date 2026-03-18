package com.omega.protocol.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.omega.protocol.db.OmegaRepository;
import java.util.concurrent.Executors;

public class SyncViewModel extends AndroidViewModel {

    private final OmegaRepository repo;
    private final MutableLiveData<String> status = new MutableLiveData<>();
    public LiveData<String> getStatus() { return status; }

    public SyncViewModel(@NonNull Application app) {
        super(app);
        repo = OmegaRepository.get(app);
    }

    public void export(java.util.function.Consumer<String> onJson) {
        repo.exportAll(json -> {
            status.postValue("Export ready — " + json.length() + " chars");
            onJson.accept(json);
        });
    }

    public void importJson(String json) {
        status.postValue("Importing…");
        repo.importAll(json, success ->
            status.postValue(success ? "✓ Import successful" : "✗ Import failed — invalid data"));
    }

    public void hardReset() {
        status.postValue("Resetting…");
        repo.hardReset(() -> status.postValue("✓ All data cleared"));
    }
}
