package com.omega.protocol.db.dao;

import androidx.room.*;
import com.omega.protocol.db.entity.AppConfigEntity;

@Dao
public interface AppConfigDao {
    @Query("SELECT * FROM app_config WHERE id=1 LIMIT 1")
    AppConfigEntity get();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(AppConfigEntity cfg);
}
