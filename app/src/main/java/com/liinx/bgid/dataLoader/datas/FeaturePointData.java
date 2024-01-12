package com.liinx.bgid.dataLoader.datas;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 特征点的内存表示
 */
public class FeaturePointData {
    // 视频中开始具有特征点的帧，begin帧包含在内
    private int begin;
    // 结束帧，end帧不包含在内
    private int end;
    private ArrayList<Trajectory> trajectories;
    private HashMap<Integer, ArrayList<Trajectory>> frameMapping;

    public FeaturePointData(int begin, int end){
        this.begin = begin;
        this.end = end;
        trajectories = new ArrayList<>();
    }

    public void addTrajectory(Trajectory t){
        trajectories.add(t);
    }

    public void sortByFrame(){
        frameMapping = new HashMap<>();
        for (Trajectory t : trajectories) {
            for (Integer frame : t) {
                if (frameMapping.containsKey(frame))
                    frameMapping.get(frame).add(t);
                else {
                    ArrayList<Trajectory> tras = new ArrayList<>();
                    tras.add(t);
                    frameMapping.put(frame, tras);
                }
            }
        }
    }

    public ArrayList<Trajectory> getFeatureByFrame(Integer frame){
        if (frameMapping == null)
            sortByFrame();
        return frameMapping.get(frame);
    }

    public int size(){return trajectories.size();}
    public int getBegin() {return begin;}
    public int getEnd() {return end;}
}
