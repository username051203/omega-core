package com.omega.protocol.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.omega.protocol.db.entity.ScheduleItemEntity;
import java.util.List;

@Dao
public interface ScheduleItemDao {
    @Query("SELECT * FROM schedule_items ORDER BY priority ASC")
    List<ScheduleItemEntity> getAll();

    @Query("SELECT * FROM schedule_items ORDER BY priority ASC")
    LiveData<List<ScheduleItemEntity>> getAllLive();

    @Query("SELECT * FROM schedule_items WHERE id = :id LIMIT 1")
    ScheduleItemEntity getById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ScheduleItemEntity item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ScheduleItemEntity> items);

    @Update
    void update(ScheduleItemEntity item);

    @Query("DELETE FROM schedule_items WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM schedule_items")
    void deleteAll();
}
