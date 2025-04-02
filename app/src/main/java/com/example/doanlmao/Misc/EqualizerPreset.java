package com.example.doanlmao.Misc;

public class EqualizerPreset {
    private String name;
    private short bass;
    private short mid;
    private short treble;

    public EqualizerPreset(String name, short bass, short mid, short treble) {
        this.name = name;
        this.bass = bass;
        this.mid = mid;
        this.treble = treble;
    }

    public String getName() {
        return name;
    }

    public short getBass() {
        return bass;
    }

    public short getMid() {
        return mid;
    }

    public short getTreble() {
        return treble;
    }
}