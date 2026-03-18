package com.omega.protocol.db.entity;

import androidx.annotation.NonNull;
import androidx.room.*;

@Entity(tableName = "tick_logs",
        indices = {@Index("item_id"), @Index("iso_date")})
public class TickLogEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    @ColumnInfo(name = "item_id")   public String itemId;
    @ColumnInfo(name = "pass_idx")  public int passIdx;
    @ColumnInfo(name = "sp_idx")    public int spIdx;
    public boolean done;
    /** ISO-8601 timestamp, e.g. 2026-03-15T14:32:00 */
    @ColumnInfo(name = "iso_date")  public String iso;
}
