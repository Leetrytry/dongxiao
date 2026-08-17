package com.dongxiao.practice.music;

public enum FingeringMode {
    TUBE_AS_5("筒音作5", 5),
    TUBE_AS_2("筒音作2", 2),
    TUBE_AS_1("筒音作1", 1),
    TUBE_AS_6("筒音作6", 6);

    public final String label;
    public final int tubeDegree;

    FingeringMode(String label, int tubeDegree) {
        this.label = label;
        this.tubeDegree = tubeDegree;
    }

    @Override
    public String toString() {
        return label;
    }
}
