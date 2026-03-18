package com.omega.protocol.model;

public class SubPass {
    public String id;
    public String label;
    public String doneIso;
    public float  hrs;
    public boolean done;

    public SubPass() {}

    public SubPass(String id, String label, float hrs) {
        this.id    = id;
        this.label = label;
        this.hrs   = hrs;
        this.done  = false;
    }
}
