package com.omega.protocol;

import android.app.Application;
import com.omega.protocol.db.OmegaRepository;

public class OmegaApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Warm up repository on background thread
        new Thread(() -> OmegaRepository.get(this)).start();
    }
}
