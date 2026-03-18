package com.omega.protocol.model;

import java.util.ArrayList;
import java.util.List;

public class Routine {

    public static class TimeRange {
        public float s;
        public float e;
        public TimeRange() {}
        public TimeRange(float s, float e) { this.s = s; this.e = e; }
    }

    public List<TimeRange> slp = new ArrayList<>();
    public List<Float>     ml  = new ArrayList<>();
    public List<TimeRange> bf  = new ArrayList<>();

    public Routine() {
        slp.add(new TimeRange(23f, 6.5f));
        ml.add(8.5f); ml.add(13f); ml.add(20f);
        bf.add(new TimeRange(17f, 18.5f));
    }
}
