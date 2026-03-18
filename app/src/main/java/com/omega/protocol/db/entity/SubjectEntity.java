package com.omega.protocol.db.entity;

import androidx.annotation.NonNull;
import androidx.room.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.omega.protocol.model.Chapter;
import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "subjects")
public class SubjectEntity {
    @PrimaryKey @NonNull public String id = "";
    public String name;
    /** JSON-serialized List<Chapter> */
    @ColumnInfo(name = "chapters_json") public String chaptersJson = "[]";

    public List<Chapter> getChapters() {
        return new Gson().fromJson(chaptersJson,
            new TypeToken<List<Chapter>>(){}.getType());
    }
    public void setChapters(List<Chapter> c) {
        chaptersJson = new Gson().toJson(c);
    }
}
