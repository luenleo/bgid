package com.liinx.bgid.dataLoader;

import android.util.Log;

import com.liinx.bgid.dataLoader.datas.AccelerationData;
import com.liinx.bgid.dataLoader.datas.FeaturePointData;
import com.liinx.bgid.dataLoader.datas.GyroscopeData;
import com.liinx.bgid.dataLoader.datas.SyncFrame;
import com.liinx.bgid.dataLoader.datas.VideoData;
import com.liinx.bgid.utils.MadgwickAHRS;
import com.liinx.bgid.utils.Quaternion;
import com.liinx.bgid.utils.Vector;

import java.sql.Timestamp;
import java.util.ArrayList;

public class DataSyncer {
    private MadgwickAHRS imuFusion = new MadgwickAHRS();
    private ArrayList<SyncFrame> frames;

    public DataSyncer(AccelerationData acce, GyroscopeData gyro, VideoData video, FeaturePointData features) {
        frames = new ArrayList<>();
        // 当前帧的位姿四元数
        Quaternion curFramePose = null;
        // 前一帧的位姿四元数
        Quaternion preFramePose = null;
        // IMU数据游标，表示IMU积累到的数据位置，两个游标是同时移动的
        int acceCursor = -1;
        int gyroCursor = -1;

        // 计算每一帧的位姿四元数，从有轨迹标记帧开始计算，到最后一帧多一帧
        for (int i = features.getBegin(); i < features.getEnd()+1; i++) {
            Log.i("BGID", "正在同步帧"+i);
            Long videoTimestamp = video.getTimestampByFrame(i);

            // 当前帧时间戳在加速度计时间戳的位置
            int curAcceIndex = acce.getIndex(videoTimestamp);
            // 当前帧时间戳在陀螺仪时间戳的位置
            int curGyroIndex = gyro.getIndex(videoTimestamp);

            // 初始化
            if (acceCursor == -1)
                acceCursor = curAcceIndex;
            if (gyroCursor == -1)
                gyroCursor = curGyroIndex;

            // 累积视频帧中间的运动过程，使得cursor=curIndex
            // WARNING: 此处假设陀螺仪和加速度计的timestamp相同，实际上应做处理
            for (int j = 0; j < curAcceIndex-acceCursor; j++) {
                // 获取陀螺仪和加速度计数据
                Vector curAcce = acce.getAcce(acceCursor++);
                Vector curGyro = gyro.getGyro(gyroCursor++);

                // 使用IMU数据融合算法计算位姿四元数
                imuFusion.updateIMU(curGyro.e0, -curGyro.e2, curGyro.e1, curAcce.e0, -curAcce.e2, curAcce.e1);
            }

            // IMU融合算法中前一时间戳的位姿四元数
            Quaternion leftPose = new Quaternion(imuFusion.getq0(), -imuFusion.getq1(), imuFusion.getq2(), -imuFusion.getq3());

            // 计算
            Vector curAcce = acce.getAcce(acceCursor++);
            Vector curGyro = gyro.getGyro(gyroCursor++);
            imuFusion.updateIMU(curGyro.e0, -curGyro.e2, curGyro.e1, curAcce.e0, -curAcce.e2, curAcce.e1);

            // IMU融合算法中后一时间戳的位姿四元数
            Quaternion rightPose = new Quaternion(imuFusion.getq0(), -imuFusion.getq1(), imuFusion.getq2(), -imuFusion.getq3());;

            // 获取插值的比例，即视频时间戳在IMU时间戳中的前后比例
            float ratio = acce.getRatio(curAcceIndex, videoTimestamp);

            // 当为标记帧时，插值计算当前帧的位姿四元数，新建前一帧的SyncFrame对象
            if (preFramePose != null) {
                preFramePose = curFramePose;
                curFramePose = Quaternion.interpolate(leftPose, rightPose, ratio);
                frames.add(new SyncFrame(
                        video.getTimestampByFrame(i-1),
                        i-1,
                        features.getFeatureByFrame(i-1),
                        preFramePose,
                        curFramePose
                ));
            }
            // 当为标记帧前一帧时，计算但不新建SyncFrame
            else {
                curFramePose = Quaternion.interpolate(leftPose, rightPose, ratio);
                preFramePose = curFramePose;
            }
        }

        Log.i("BGID", "DataSyncer: 同步完成");
    }

    public ArrayList<SyncFrame> getFrames() {
        return frames;
    }
}
