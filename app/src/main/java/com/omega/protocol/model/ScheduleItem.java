package com.omega.protocol.model;

import java.util.ArrayList;
import java.util.List;

public class ScheduleItem {
    public String id;
    public String subjectId;
    public String chapterId;
    public String topicId;
    public String pinDate;
    public int    priority = 50;
    public int    part     = 1;
    public List<PassGroup> passes = new ArrayList<>();

    public float totalHrs() {
        float h = 0;
        for (PassGroup pg : passes)
            for (SubPass sp : pg.subPasses) h += sp.hrs;
        return h;
    }

    public int totalCount() {
        int c = 0;
        for (PassGroup pg : passes) c += pg.subPasses.size();
        return c;
    }

    public int doneCount() {
        int c = 0;
        for (PassGroup pg : passes)
            for (SubPass sp : pg.subPasses) if (sp.done) c++;
        return c;
    }

    public boolean isComplete() {
        int t = totalCount();
        return t > 0 && doneCount() == t;
    }
}
