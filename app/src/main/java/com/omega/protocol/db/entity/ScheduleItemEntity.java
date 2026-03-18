package com.omega.protocol.db.entity;

import androidx.annotation.NonNull;
import androidx.room.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.omega.protocol.model.PassGroup;
import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "schedule_items")
public class ScheduleItemEntity {
    @PrimaryKey @NonNull public String id = "";
    public String subjectId;
    public String chapterId;
    public String topicId;
    public int    priority  = 50;
    public int    part      = 1;
    public String pinDate;
    /** JSON-serialized List<PassGroup> */
    @ColumnInfo(name = "passes_json")
    public String passesJson = "[]";

    public List<PassGroup> getPasses() {
        return new Gson().fromJson(passesJson,
            new TypeToken<List<PassGroup>>(){}.getType());
    }
    public void setPasses(List<PassGroup> p) {
        passesJson = new Gson().toJson(p);
    }
}
