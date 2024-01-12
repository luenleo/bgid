package com.liinx.bgid.utils;

public class Interpolate {

    public static float[] run(long leftTS, float[] v1, long rightTS, float[] v2, long targetTS){
        double ratio = (targetTS - leftTS) * 1.0 / (rightTS - leftTS);
        double ratio_ = 1 - ratio;
        float[] result = new float[3];
        for (int i = 0; i < 3; i++)
            result[i] = (float) (v1[i] * ratio_ + v2[i] * ratio);
        return result;
    }
}
