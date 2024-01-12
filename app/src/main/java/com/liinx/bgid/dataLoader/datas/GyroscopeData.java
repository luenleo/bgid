package com.liinx.bgid.dataLoader.datas;

import com.liinx.bgid.utils.Vector;

import java.util.ArrayList;

public class GyroscopeData {
    private ArrayList<Long> timestamps;
    private ArrayList<Float> g0;
    private ArrayList<Float> g1;
    private ArrayList<Float> g2;

    public int getIndex(Long value){
        int left = 0;
        int right = timestamps.size()-1;
        int index;
        while(left <= right && right - left != 1){
            index = (left+right) / 2;
            if (timestamps.get(index) < value){
                left = index + 1;
            }
            else if (timestamps.get(index) > value) {
                right = index - 1;
            }
            else
                return index;
        }
        return left;
    }

//    public float getRatio(int index, Long value){
//        Long left = timestamps.get(index);
//        Long right = timestamps.get(index+1);
//        return (value - left) / (right - left);
//    }

    public GyroscopeData() {
        timestamps = new ArrayList<>();
        g0 = new ArrayList<>();
        g1 = new ArrayList<>();
        g2 = new ArrayList<>();
    }

    public void addData(Long timestamp, float g0, float g1, float g2) {
        timestamps.add(timestamp);
        this.g0.add(g0);
        this.g1.add(g1);
        this.g2.add(g2);
    }

    public int size(){return timestamps.size();}

    public Vector getGyro(int index) {
        return new Vector(g0.get(index), g1.get(index), g2.get(index));
    }
}