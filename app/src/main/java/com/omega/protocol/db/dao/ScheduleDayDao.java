package com.omega.protocol.db.dao;

import androidx.room.*;
import com.omega.protocol.db.entity.ScheduleDayEntity;
import java.util.List;

@Dao
public interface ScheduleDayDao {
    @Query("SELECT * FROM schedule_days ORDER BY day_str ASC")
    List<ScheduleDayEntity> getAll();

    @Query("SELECT * FROM schedule_days WHERE day_str >= :from ORDER BY day_str ASC")
    List<ScheduleDayEntity> getFrom(String from);

    @Query("SELECT * FROM schedule_days WHERE day_str = :day LIMIT 1")
    ScheduleDayEntity getByDay(String day);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ScheduleDayEntity day);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ScheduleDayEntity> days);

    @Query("DELETE FROM schedule_days WHERE day_str > :from")
    void deleteFutureFrom(String from);

    @Query("DELETE FROM schedule_days")
    void deleteAll();
}
