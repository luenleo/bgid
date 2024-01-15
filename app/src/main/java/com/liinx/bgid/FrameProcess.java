package com.liinx.bgid;

import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.liinx.bgid.utils.Quaternion;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class FrameProcess implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {
    private static final String TAG = "GRAY-POINT";

    private static final Scalar red      = new Scalar(255,  50,  50);
    private static final Scalar green    = new Scalar( 50, 255,  50);
    private static final Scalar blue     = new Scalar( 50,  50, 255);
    private static final Scalar white    = new Scalar(255, 255, 255);

    private static final FrameProcess fp = new FrameProcess();
    private FrameProcess(){}
    public static FrameProcess getInstance(){
        return fp;
    }

    private boolean WhiteBalanceOn = false;

    private Mat preRGBFrame = null;
    private Mat preGrayFrame = null;
    private Mat curRGBFrame = null;
    private Mat curGrayFrame = null;
    private Quaternion prePose = null;
    private Quaternion curPose = null;
    private MatOfPoint2f preGrayPoints = new MatOfPoint2f();
    private MatOfPoint2f curGrayPoints = new MatOfPoint2f();
    private int grayPointNumber = CONFIG.grayPointNumber;

    // 当前帧单帧颜色估计结果
    private Point3 L_0;
    // 前一帧单帧光源颜色估计结果
    private Point3 L_0_pre = new Point3(0,0,0);
    // 前一帧最终光源颜色估计结果
    private Point3 L_f_pre = new Point3(0,0,0);
    // 当前帧最终光源估计结果
    private Point3 L_f = new Point3(0,0,0);
    // 当前帧加权平均后的参考结果
    private Point3 L_ref = new Point3(0,0,0);


    private MatOfByte status = new MatOfByte();
    private MatOfFloat err = new MatOfFloat();

    /**
     * 用于优先队列排序的包装类
     */
    class WrapPoint extends Point{
        public double value;

        public double getValue() {
            return value;
        }

        public WrapPoint(double x, double y, double value) {
            this.x = x;
            this.y = y;
            this.value = value;
        }
    }
    private final Comparator<WrapPoint> cmp = Comparator.comparingDouble(WrapPoint::getValue);

    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    @Override
    public void onCameraViewStopped() {
    }

    private int counter = 0;
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button) {
                Mat save = new Mat();
                Imgproc.cvtColor(curRGBFrame, save, Imgproc.COLOR_RGBA2BGR);
                String filename = "/storage/emulated/0/Download/photo_"+ counter++ +".jpg";
                Imgcodecs.imwrite(filename, save);
                Toast.makeText(ImuListener.activity, "保存至"+filename, Toast.LENGTH_SHORT).show();
            }
        else if (v.getId() == R.id.switchMode){
            WhiteBalanceOn = !WhiteBalanceOn;
        }
    }

    private double calAngel(Point3 a, Point3 b){
        return Math.acos(a.dot(b)
                /(Math.sqrt(a.x*a.x + a.y*a.y + a.z*a.z)
                *Math.sqrt(b.x*b.x + b.y*b.y + b.z*b.z)));
    }

    private Point3 singleFrameLightEst(Mat frame, Mat draw){
        List<Mat> channels = new ArrayList<>();
        Core.split(frame, channels);
        Mat redFrame = new Mat();
        Mat blueFrame = new Mat();
        Mat greenFrame = new Mat();
        channels.get(2).convertTo(redFrame, CvType.CV_64F);
        channels.get(1).convertTo(greenFrame, CvType.CV_64F);
        channels.get(0).convertTo(blueFrame, CvType.CV_64F);

        Mat temp = new Mat(frame.size(), CvType.CV_64F);
        Core.add(redFrame, greenFrame, temp);
        Core.add(blueFrame, temp, temp);
        Core.log(temp, temp);

        Core.log(redFrame, redFrame);
        Core.log(blueFrame, blueFrame);

        Core.subtract(redFrame, temp, redFrame);
        Core.subtract(blueFrame, temp, blueFrame);

        // LoG
        Imgproc.GaussianBlur(redFrame, redFrame, new Size(3,3), 2);
        Imgproc.GaussianBlur(blueFrame, blueFrame, new Size(3,3), 2);
        Imgproc.Laplacian(redFrame, redFrame, CvType.CV_64F, 1);
        Imgproc.Laplacian(blueFrame, blueFrame, CvType.CV_64F, 1);

        Core.pow(redFrame, 2, redFrame);
        Core.pow(blueFrame, 2, blueFrame);
        Core.add(redFrame, blueFrame, temp);
        Core.sqrt(temp, temp);

        PriorityQueue<WrapPoint> grayPoints = new PriorityQueue<>(cmp);
        for (int i = 1; i < temp.rows()-1; i += 1)
            for (int j = 1; j < temp.cols()-1; j += 1) {
                grayPoints.add(new WrapPoint(i, j, temp.get(i, j)[0]));
            }

        L_0 = new Point3(0,0,0);
        List<Point> gps = new ArrayList<>();
        for (int i = 0; i < grayPointNumber; i++) {
            Point p = grayPoints.poll();
            gps.add(p);

            byte[] BGR = new byte[4];
            curRGBFrame.get((int) p.x, (int) p.y, BGR);
            L_0.x += BGR[2];
            L_0.y += BGR[1];
            L_0.z += BGR[0];

            Imgproc.circle(draw, p, 1, green, -1);
        }
        L_0.x /= grayPointNumber;
        L_0.y /= grayPointNumber;
        L_0.z /= grayPointNumber;

        curGrayPoints.fromList(gps);
        return L_0;
    }

    private Point3 grayPointShiftLightEst(Mat result){
        curGrayPoints = new MatOfPoint2f();
        Video.calcOpticalFlowPyrLK(preGrayFrame, curGrayFrame, preGrayPoints, curGrayPoints, status, err);

        // 计算旋转矩阵
        Mat R_cur = curPose.quaternionToR();
        Mat R_pre = prePose.quaternionToR();
        Mat R = R_cur.matMul(R_pre.inv());
        Mat cameraR = CONFIG.innerMat.matMul(R).matMul(CONFIG.innerMat.inv());
        //Mat rotateMat = CONFIG.M1.inv().matMul(cameraR).matMul(CONFIG.M2.inv());

        // 对个特征点计算旋转分量
        Point[] pre = preGrayPoints.toArray(), cur = curGrayPoints.toArray();
        List<Point> shiftedGrayPoints = new ArrayList<>();
        byte[] s = status.toArray();
        for (int i = 0; i < pre.length; i++) {
            if (s[i] == 1) {
                // 当前坐标
                Mat location = new Mat(3, 1, CvType.CV_64F);
                double[] temp = {pre[i].x, pre[i].y, 1.0f};
                location.put(0, 0, temp);

                // 乘上旋转矩阵，获得旋转后坐标
                Mat rotateLocation = cameraR.matMul(location);

                // 归一化旋转后坐标，旋转后的成像平面坐标
                rotateLocation.get(0, 0, temp);
                double x_rotate = temp[0] / temp[2];
                double y_rotate = temp[1] / temp[2];

                // 计算旋转分量、平移分量
                double u_trans = cur[i].x - x_rotate;
                double v_trans = cur[i].y - y_rotate;

                //更新灰点位置，得到映射过后的灰点集合,判断是否出界
                Point shifted = new Point(pre[i].x + u_trans, pre[i].y + v_trans);
                if (0<=shifted.x && shifted.x<curRGBFrame.rows() && 0<=shifted.y && shifted.y<curRGBFrame.cols()){
                    shiftedGrayPoints.add(shifted);
                    Imgproc.circle(result, shifted, 1, red, -1);
                }
            }
        }

        if(shiftedGrayPoints.size() < pre.length/2){
            curGrayPoints.fromArray(pre);//多数出界回退到上一帧灰点位置集合
        }else{
            curGrayPoints.fromList(shiftedGrayPoints);
        }

        //计算当前帧的L_s
        Point3 L_s = new Point3(0,0,0);
        Point[] gps = curGrayPoints.toArray();
        for (Point grayPoint : gps){
            byte[] RGB = new byte[4];
            curRGBFrame.get((int) grayPoint.x, (int) grayPoint.y, RGB);
            L_s.x += RGB[2];
            L_s.y += RGB[1];
            L_s.z += RGB[0];
        }
        L_s.x /= gps.length;
        L_s.y /= gps.length;
        L_s.z /= gps.length;
        return L_s;
    }

    private Point3 correctLight(){
        L_f.x = 0;
        L_f.y = 0;
        L_f.z = 0;
        PriorityQueue<WrapPoint> grayPoints = new PriorityQueue<>(cmp);
        for(int i = 0; i < curRGBFrame.rows(); i = i + 10){
            for(int j = 0; j < curRGBFrame.cols(); j = j + 10){
                double[] pix = curRGBFrame.get(i, j);
                //计算灰度指数
                Point3 pix3 = new Point3(pix[2], pix[1], pix[0]);
                double g = calAngel(pix3, L_ref);
                grayPoints.add(new WrapPoint(i, j, g));
            }
        }

        byte[] BGR = new byte[4];
        for (int i = 0; i < grayPointNumber; i++) {
            WrapPoint p = grayPoints.poll();
            curRGBFrame.get((int) p.x, (int) p.y, BGR);

            L_f.x += BGR[2];
            L_f.y += BGR[1];
            L_f.z += BGR[0];
        }

        L_f.x /= grayPointNumber;
        L_f.y /= grayPointNumber;
        L_f.z /= grayPointNumber;
        return L_f;
    }

    private Mat adjustWhiteBalance(Mat frame, Point3 L_f){
        //进行白平衡
        List<Mat> channels = new ArrayList<>();
        Core.split(frame, channels);
        double LightSourceMean = (L_f.x + L_f.y + L_f.z)/3.0;

        Scalar gainR = new Scalar(LightSourceMean / L_f.x);
        Scalar gainG = new Scalar(LightSourceMean / L_f.y);
        Scalar gainB = new Scalar(LightSourceMean / L_f.z);
        Scalar[] gain = {gainB, gainG, gainR};
        for(int i = 0; i < 3; i++){
            Mat channel = channels.get(i);
            Core.multiply(channel, gain[i], channel);
        }
        Core.merge(channels, frame);
        return frame;
    }

    //灰点检测，光源融合部分
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        if (!WhiteBalanceOn){
            preGrayPoints = new MatOfPoint2f();
            return inputFrame.rgba();
        }
        curRGBFrame = inputFrame.rgba();
        curGrayFrame = inputFrame.gray();
        curPose = ImuListener.getInstance().getPose();

        Mat result = (preRGBFrame==null) ?curRGBFrame :preRGBFrame.clone();
        preRGBFrame = curRGBFrame;
        Log.i(TAG, "准备工作");

        singleFrameLightEst(curRGBFrame, result);
        Log.i(TAG, "L0:"+L_0);

        //非第一帧，有前一帧
        if (!preGrayPoints.empty()){
            // 计算光流，计算映射过后的灰点位置集合
            Point3 L_s = grayPointShiftLightEst(result);
            Log.i(TAG, "L_s:"+L_s);

            //计算角误差与加权权重
            double theta_s = calAngel(L_f_pre, L_0);
            double theta_0 = calAngel(L_0, L_0_pre);
            double w = Math.exp(-0.3 * Math.min(theta_0, theta_s) * Math.min(theta_0, theta_s));
            Log.i(TAG, "ts:"+theta_s + ",\tt0:"+theta_0+",\tw:"+w);

            //加权融合光源
            L_ref = new Point3(
                    L_f_pre.x * w + L_0.x * (1-w),
                    L_f_pre.y * w + L_0.y * (1-w),
                    L_f_pre.z * w + L_0.z * (1-w)
            );
            Log.i(TAG, "Lref:"+L_ref);

            //根据融合光源估计重新进行灰点检测
            correctLight();
            Log.i(TAG, "L_f:"+L_f);

            adjustWhiteBalance(result, L_f);
            Log.i(TAG, "画面白平衡\n");
            L_f_pre = L_f;
        } else {//为第一帧,单帧检测结果即为最终光源估计（论文中为最终光源融合结果L_ref，这里不再通过新的灰度指数计算方法进行灰点检测）
            Log.i(TAG, "第一帧");
            L_f_pre = L_0;
        }

        L_0_pre = L_0;
        preGrayPoints = curGrayPoints;
        preGrayFrame = curGrayFrame;
        prePose = curPose;
        return result;
    }
}
