package com.omega.protocol.model;

import java.util.ArrayList;
import java.util.List;

public class Subject {
    public String id;
    public String name;
    public List<Chapter> chapters = new ArrayList<>();

    public Subject() {}
    public Subject(String id, String name) { this.id = id; this.name = name; }

    public Topic findTopic(String topicId) {
        for (Chapter c : chapters)
            for (Topic t : c.topics)
                if (t.id.equals(topicId)) return t;
        return null;
    }

    public Chapter findChapter(String chapterId) {
        for (Chapter c : chapters)
            if (c.id.equals(chapterId)) return c;
        return null;
    }
}
