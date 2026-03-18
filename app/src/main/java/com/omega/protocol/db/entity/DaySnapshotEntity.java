package com.omega.protocol.db.entity;

import androidx.annotation.NonNull;
import androidx.room.*;

@Entity(tableName = "day_snapshots")
public class DaySnapshotEntity {
    @PrimaryKey @NonNull
    @ColumnInfo(name = "day_str") public String dayStr = "";
    @ColumnInfo(name = "user_ch")     public int   userCh;
    @ColumnInfo(name = "alicia_ch")   public float aliciaCh;
    public Boolean win;  // null = no data
    @ColumnInfo(name = "total_passes") public int  totalPasses;
    public float boost;
    @ColumnInfo(name = "hrs_used")    public float hrsUsed;
    public float budget;
}
