package com.liinx.bgid.utils;

import com.liinx.bgid.CONFIG;

/**
 * 记录每一帧中图像坐标的点，包含额外的旋转分量信息
 */
public class Point {
    public float R_dx, R_dy;
    public float T_dx, T_dy;
    public float map_x, map_y;

    public float x, y;

    public void setRotate(float dx, float dy){
        R_dx = dx;
        R_dy = dy;
    }

    public void setTrans(float dx, float dy){
        T_dx = dx;
        T_dy = dy;
    }

    public float calTranLen(){
        return T_dx*T_dx + T_dy*T_dy;
    }
    public float calDistance(Point another){
        return (float) Math.sqrt((x-another.x) * (x-another.x) + (y-another.y) * (y-another.y));
    }
    public float calTranDistance(Point another){
        return (T_dx-another.T_dx) * (T_dx-another.T_dx) + (T_dy-another.T_dy) * (T_dy-another.T_dy);
    }

    public Point(float x, float y) {
        // 从像素坐标映射到图像坐标，是否该放在这里呢？
        this.x = x;
        this.y = y;
        this.map_x = x * CONFIG.xRate;
        this.map_y = y * CONFIG.yRate;
    }
}
