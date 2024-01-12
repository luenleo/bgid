package com.liinx.bgid.dataLoader.datas;

import androidx.annotation.NonNull;

import com.liinx.bgid.utils.Quaternion;

import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.Iterator;

public class SyncFrame implements Iterable<Trajectory> {
    private Long timestamp;
    private Integer frameInVideo;
    // 当前帧中包含的轨迹
    private ArrayList<Trajectory> trajectories;
    // 当前帧的位姿四元数
    private Quaternion pose;
    // 后一帧位姿四元数
    private Quaternion postPose;
    // 当前帧相对前一帧的旋转矩阵
    private Mat R;

    public SyncFrame(Long timestamp, int frame, ArrayList<Trajectory> trajectories, Quaternion pose, Quaternion postPose) {
        this.timestamp = timestamp;
        this.frameInVideo = frame;
        this.trajectories = trajectories;
        this.pose = pose;
        this.postPose = postPose;
    }

    public void setR(Mat r) {
        R = r;
    }

    public Long getTimestamp() {return timestamp;}
    public Integer getFrameInVideo() {return frameInVideo;}
    public ArrayList<Trajectory> getTrajectories() {return trajectories;}
    public Quaternion getPose() {return pose;}
    public Quaternion getPostPose() {return postPose;}

    @NonNull
    @Override
    public Iterator<Trajectory> iterator() {
        return trajectories.iterator();
    }
}
