package com.omega.protocol.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.omega.protocol.db.entity.SubjectEntity;
import java.util.List;

@Dao
public interface SubjectDao {
    @Query("SELECT * FROM subjects")
    List<SubjectEntity> getAll();

    @Query("SELECT * FROM subjects")
    LiveData<List<SubjectEntity>> getAllLive();

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    SubjectEntity getById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SubjectEntity s);

    @Query("DELETE FROM subjects WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM subjects")
    void deleteAll();
}
