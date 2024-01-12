package com.liinx.bgid.dataLoader.datas;

import androidx.annotation.NonNull;

import com.liinx.bgid.utils.Point;

import java.util.HashMap;
import java.util.Iterator;

public class Trajectory implements Iterable<Integer> {
    private static int COUNTER = 0;
    private int ID;
    private int duration;
    private int beginFrame;
    private HashMap<Integer, Point> locations;

    public Trajectory(int duration) {
        this.duration = duration;
        ID = COUNTER++;
        locations = new HashMap<>();
    }

    public void addData(Float x, Float y, Integer frame){
        Point location = new Point(x, y);
        locations.put(frame, location);
    }

    public Point getLocationByFrame(Integer frame){
        return locations.getOrDefault(frame, null);
    }

    public int getBeginFrame() {return beginFrame;}
    public void setBeginFrame(int beginFrame) {this.beginFrame = beginFrame;}
    @NonNull
    @Override
    public Iterator<Integer> iterator() {
        return locations.keySet().iterator();
    }
}
