package com.omega.protocol.db.dao;

import androidx.room.*;
import com.omega.protocol.db.entity.TickLogEntity;
import java.util.List;

@Dao
public interface TickLogDao {
    @Query("SELECT * FROM tick_logs ORDER BY id DESC LIMIT 1000")
    List<TickLogEntity> getRecent();

    @Query("SELECT COUNT(*) FROM tick_logs WHERE done=1 AND iso_date LIKE :dayPrefix || '%'")
    int countDoneOnDay(String dayPrefix);

    @Query("SELECT DISTINCT substr(iso_date,1,10) FROM tick_logs WHERE done=1 ORDER BY iso_date DESC")
    List<String> getDaysWithPasses();

    @Insert
    void insert(TickLogEntity log);

    @Query("DELETE FROM tick_logs")
    void deleteAll();
}
