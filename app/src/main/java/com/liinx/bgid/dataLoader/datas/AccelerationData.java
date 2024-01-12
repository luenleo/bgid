package com.liinx.bgid.dataLoader.datas;

import com.liinx.bgid.utils.Vector;

import java.util.ArrayList;

public class AccelerationData {
    private ArrayList<Long> timestamps;
    private ArrayList<Float> a0;
    private ArrayList<Float> a1;
    private ArrayList<Float> a2;

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

    public float getRatio(int index, Long value){
        double left = timestamps.get(index);
        double right = timestamps.get(index+1);
        return (float) ((value - left) / (right - left));
    }

    public AccelerationData() {
        timestamps = new ArrayList<>();
        a0 = new ArrayList<>();
        a1 = new ArrayList<>();
        a2 = new ArrayList<>();
    }

    public void addData(Long timestamp, float a0, float a1, float a2) {
        timestamps.add(timestamp);
        this.a0.add(a0);
        this.a1.add(a1);
        this.a2.add(a2);
    }

    public int size(){return timestamps.size();}

    public Vector getAcce(int index) {
        return new Vector(a0.get(index), a1.get(index), a2.get(index));
    }
}
