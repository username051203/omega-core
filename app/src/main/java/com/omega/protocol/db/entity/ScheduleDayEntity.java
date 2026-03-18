package com.omega.protocol.db.entity;

import androidx.annotation.NonNull;
import androidx.room.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "schedule_days")
public class ScheduleDayEntity {
    @PrimaryKey @NonNull
    @ColumnInfo(name = "day_str") public String dayStr = "";
    /** JSON array of item IDs */
    @ColumnInfo(name = "item_ids_json") public String itemIdsJson = "[]";

    public List<String> getItemIds() {
        return new Gson().fromJson(itemIdsJson,
            new TypeToken<List<String>>(){}.getType());
    }
    public void setItemIds(List<String> ids) {
        itemIdsJson = new Gson().toJson(ids);
    }
}
