package com.omega.protocol.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.omega.protocol.db.entity.DaySnapshotEntity;
import java.util.List;

@Dao
public interface DaySnapshotDao {
    @Query("SELECT * FROM day_snapshots ORDER BY day_str DESC")
    List<DaySnapshotEntity> getAll();

    @Query("SELECT * FROM day_snapshots ORDER BY day_str DESC")
    LiveData<List<DaySnapshotEntity>> getAllLive();

    @Query("SELECT * FROM day_snapshots WHERE day_str = :day LIMIT 1")
    DaySnapshotEntity getByDay(String day);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DaySnapshotEntity snap);

    @Query("DELETE FROM day_snapshots WHERE day_str = :day")
    void deleteByDay(String day);

    @Query("DELETE FROM day_snapshots")
    void deleteAll();
}
