package com.omega.protocol.model;

import java.util.ArrayList;
import java.util.List;

public class PassGroup {
    public String type;
    public List<SubPass> subPasses = new ArrayList<>();

    public PassGroup() {}
    public PassGroup(String type) { this.type = type; }
}
