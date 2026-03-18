package com.omega.protocol.db;

import android.content.Context;
import androidx.room.*;
import com.omega.protocol.db.dao.*;
import com.omega.protocol.db.entity.*;

@Database(
    entities = {
        ScheduleItemEntity.class,
        TickLogEntity.class,
        DaySnapshotEntity.class,
        SubjectEntity.class,
        ScheduleDayEntity.class,
        AppConfigEntity.class
    },
    version = 1,
    exportSchema = false
)
public abstract class OmegaDatabase extends RoomDatabase {

    private static volatile OmegaDatabase INSTANCE;

    public abstract ScheduleItemDao scheduleItemDao();
    public abstract TickLogDao      tickLogDao();
    public abstract DaySnapshotDao  daySnapshotDao();
    public abstract SubjectDao      subjectDao();
    public abstract ScheduleDayDao  scheduleDayDao();
    public abstract AppConfigDao    appConfigDao();

    public static OmegaDatabase get(Context ctx) {
        if (INSTANCE == null) {
            synchronized (OmegaDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        ctx.getApplicationContext(),
                        OmegaDatabase.class,
                        "omega.db"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
