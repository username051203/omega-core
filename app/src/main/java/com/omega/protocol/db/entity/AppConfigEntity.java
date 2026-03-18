package com.omega.protocol.db.entity;

import androidx.annotation.NonNull;
import androidx.room.*;

/** Single-row config table. Always upserted with id=1. */
@Entity(tableName = "app_config")
public class AppConfigEntity {
    @PrimaryKey public int id = 1;
    /** JSON of EngineConfig */
    @ColumnInfo(name = "engine_json")   public String engineJson;
    /** JSON of Routine */
    @ColumnInfo(name = "routine_json")  public String routineJson;
    /** JSON of RivalState */
    @ColumnInfo(name = "rival_json")    public String rivalJson;
    /** JSON of Profiles */
    @ColumnInfo(name = "profiles_json") public String profilesJson;
}
