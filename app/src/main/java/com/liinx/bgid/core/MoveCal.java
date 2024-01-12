package com.liinx.bgid.core;

import com.liinx.bgid.CONFIG;
import com.liinx.bgid.dataLoader.datas.SyncFrame;
import com.liinx.bgid.dataLoader.datas.Trajectory;
import com.liinx.bgid.utils.Point;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class MoveCal {
    public static void run(SyncFrame frame){
        // 计算旋转矩阵
        Mat R_cur = frame.getPose().quaternionToR();
        Mat R_next = frame.getPostPose().quaternionToR();

        // 三维旋转矩阵R
        Mat R = R_next.matMul(R_cur.inv());
        frame.setR(R);
        // 乘上相机内参
        Mat cameraR = CONFIG.innerMat.matMul(R).matMul(CONFIG.innerMat.inv());
        // 乘上平面旋转矩阵
        Mat rotateMat = CONFIG.M1.inv().matMul(cameraR).matMul(CONFIG.M2.inv());

        // 对每条轨迹计算旋转分量
        int frameNumber = frame.getFrameInVideo();
        // 旋转后的成像平面坐标
        double r_x, r_y;
        for (Trajectory t : frame) {
            // 前后帧位置
            Point curPoint = t.getLocationByFrame(frameNumber);
            Point nextPoint = t.getLocationByFrame(frameNumber + 1);
            if (nextPoint != null) {
                // 当前坐标
                Mat location = new Mat(3, 1, CvType.CV_64F);
                double[] temp = {curPoint.map_x, curPoint.map_y, 1.0f};
                location.put(0, 0, temp);

                // 乘上旋转矩阵，获得旋转后坐标
                Mat rotateLocation = rotateMat.matMul(location);

                // 归一化旋转后坐标，旋转后的成像平面坐标
                rotateLocation.get(0, 0, temp);
                r_x = temp[0] / temp[2];
                r_y = temp[1] / temp[2];

                // 计算旋转分量、平移分量
                curPoint.setRotate((float) (r_x - curPoint.map_x), (float) (r_y - curPoint.map_y));
                curPoint.setTrans((float) (nextPoint.map_x - r_x), (float) (nextPoint.map_y - r_y));
            }
        }
    }
}