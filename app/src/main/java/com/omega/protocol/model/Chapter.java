package com.omega.protocol.model;

import java.util.ArrayList;
import java.util.List;

public class Chapter {
    public String id;
    public String name;
    public List<Topic> topics = new ArrayList<>();

    public Chapter() {}
    public Chapter(String id, String name) { this.id = id; this.name = name; }
}
