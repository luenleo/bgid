package com.liinx.bgid;

import android.util.Log;
import android.view.View;
import android.widget.TextView;
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
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

class DataOutput{
    private String rootPath;
    private Map<String, File> files = new HashMap<>();

    public DataOutput(String rootPath) {
        this.rootPath = rootPath;
    }

    public void write(String type, boolean separator, double... data){
        File f;
        if (files.containsKey(type)){
            f = files.get(type);
        } else {
            f = new File(rootPath+type+".txt");
            try (FileWriter w = new FileWriter(f)){
                w.write("");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            files.put(type, f);
        }
        try (FileWriter writer = new FileWriter(f, true)){
            StringBuilder temp = new StringBuilder();
            for (double x : data)
                if (separator) temp.append(String.format("%1.6f", x)).append(',').append(' ');
                else temp.append(String.format("%1.6f", x));
            temp.append('\n');
            writer.append(temp.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void write(String type, double... data){
        write(type, true, data);
    }

    public void write(String type, Point3 p){
        write(type, p.x, p.y, p.z);
    }
}

public class FrameProcess implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {
    //<editor-fold desc="输出相关参数">
    private static final String TAG = "GRAY-POINT";
    private final String rootPath = "/storage/emulated/0/AAAAAA/";
    private DataOutput LOG = new DataOutput(rootPath);
    //原始视频保存路径
    String fileUrl_o = rootPath+"video_org";
    //处理视频保存路径
    String fileUrl_imu = rootPath+"video_imu";
    //单帧白平衡视频保存路径
    String fileUrl_SWB = rootPath+"video_SWB";
    //原始视频
    VideoWriter videoWriter_org = null;
    //经过ium灰点漂移加光源融合后的视频
    VideoWriter videoWriter_imu = null;
    //仅通过单帧白平衡得到的视频
    VideoWriter videoWriter_SWB = null;

    private static final Scalar red      = new Scalar(255,  50,  50);
    private static final Scalar green    = new Scalar( 50, 255,  50);
    private static final Scalar blue     = new Scalar( 50,  50, 255);
    private static final Scalar white    = new Scalar(255, 255, 255);
    //</editor-fold>

    //<editor-fold desc="系统状态以及算法参数">
    public static MainActivity activity;
    private boolean WhiteBalanceOn = false;
    //是否录制标识
    private boolean isRecording = false;
    private final boolean showInfo = true;
    private final int grayPointNumber = CONFIG.grayPointNumber;
    private final double downsampleFactor = CONFIG.downsampleFactor;//缩放大小
    private final double contrastThreshold = CONFIG.contrastThreshold;
    //</editor-fold>

    //<editor-fold desc="单例">
    private static final FrameProcess fp = new FrameProcess();
    private FrameProcess(){}
    public static FrameProcess getInstance(){
        return fp;
    }
    //</editor-fold>

    //<editor-fold desc="全局变量">
    private Mat preRGBFrame = null;
    private Mat preGrayFrame = null;
    private Mat curRGBFrame = null;
    private Mat curGrayFrame = null;
    private Mat SWB_only = null;
    private Quaternion prePose = null;
    private Quaternion curPose = null;
    private MatOfPoint2f preGrayPoints = new MatOfPoint2f();
    private MatOfPoint2f curGrayPoints = new MatOfPoint2f();

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
    //</editor-fold>

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

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button) {
            if(isRecording == false){
                isRecording = true;
                videoWriter_org = new VideoWriter();
                videoWriter_imu = new VideoWriter();
                videoWriter_SWB = new VideoWriter();
                videoWriter_org.open(fileUrl_o + ".avi", Videoio.CAP_OPENCV_MJPEG,VideoWriter.fourcc('M', 'J', 'P', 'G'), 30,
                        new Size(960, 720),true);
                videoWriter_imu.open(fileUrl_imu + ".avi", Videoio.CAP_OPENCV_MJPEG,VideoWriter.fourcc('M', 'J', 'P', 'G'), 30,
                        new Size(960, 720),true);
                videoWriter_SWB.open(fileUrl_SWB + ".avi", Videoio.CAP_OPENCV_MJPEG,VideoWriter.fourcc('M','J','P','G'),30,
                        new Size(960,720),true);

                ((TextView) activity.findViewById(R.id.button)).setText("结束录像");
            }else{
                isRecording = false;
                videoWriter_org.release();
                videoWriter_imu.release();
                videoWriter_SWB.release();

                Toast.makeText(ImuListener.activity, "保存至:" + fileUrl_o + "以及" + fileUrl_imu,Toast.LENGTH_SHORT).show();
                ((TextView) activity.findViewById(R.id.button)).setText("开始录像");
            }
        }
        else if (v.getId() == R.id.switchMode){
            WhiteBalanceOn = !WhiteBalanceOn;
        }
    }

    private double calAngel(Point3 a, Point3 b){
        double temp = a.dot(b)
                /(Math.sqrt(a.x*a.x + a.y*a.y + a.z*a.z)
                *Math.sqrt(b.x*b.x + b.y*b.y + b.z*b.z));
        return Math.acos(temp>1 ?1 :temp);
    }

    private Point3 singleFrameLightEst(Mat frame, Mat draw){
        List<Mat> channels = new ArrayList<>();
        Core.split(frame, channels);
        Mat redFrame = new Mat();
        Mat blueFrame = new Mat();
        Mat greenFrame = new Mat();
        channels.get(0).convertTo(redFrame, CvType.CV_64F);
        channels.get(1).convertTo(greenFrame, CvType.CV_64F);
        channels.get(2).convertTo(blueFrame, CvType.CV_64F);

        Size newSize = new Size(frame.cols()*downsampleFactor, frame.rows()*downsampleFactor);
        Imgproc.resize(redFrame, redFrame, newSize,0, 0, Imgproc.INTER_LINEAR);
        Imgproc.resize(blueFrame, blueFrame, newSize,0, 0, Imgproc.INTER_LINEAR);
        Imgproc.resize(greenFrame, greenFrame, newSize,0, 0, Imgproc.INTER_LINEAR);

        Mat temp = new Mat(newSize, CvType.CV_64F);
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

        Mat temp1 = new Mat(newSize, CvType.CV_64F);
        Mat temp2 = new Mat(newSize, CvType.CV_64F);
        Core.pow(redFrame, 2, temp1);
        Core.pow(blueFrame, 2, temp2);
        Core.add(temp1, temp2, temp);
        Core.sqrt(temp, temp);

        PriorityQueue<WrapPoint> grayPoints = new PriorityQueue<>(cmp);
        for (int i = 1; i < temp.rows()-1; i += 1)
            for (int j = 1; j < temp.cols()-1; j += 1) {
                if (redFrame.get(i, j)[0] > contrastThreshold && blueFrame.get(i, j)[0] > contrastThreshold)
                    grayPoints.add(new WrapPoint(i/downsampleFactor, j/downsampleFactor, temp.get(i, j)[0]));//点的位置还原回原图，像素值采用插值后的像素值
            }

        L_0 = new Point3(0,0,0);
        List<Point> gps = new ArrayList<>();
        for (int i = 0; i < grayPointNumber; ) {
            Point p = grayPoints.poll();
            gps.add(p);

            double[] RGB = curRGBFrame.get((int) p.x, (int) p.y);
            L_0.x += RGB[0];
            L_0.y += RGB[1];
            L_0.z += RGB[2];

            if (showInfo) Imgproc.circle(draw, p, 1, green, -1);
            i++;
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
                    if (showInfo) Imgproc.circle(result, shifted, 1, red, -1);
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
            double[] RGB = curRGBFrame.get((int) grayPoint.x, (int) grayPoint.y);
            L_s.x += RGB[0];
            L_s.y += RGB[1];
            L_s.z += RGB[2];
        }
        L_s.x /= gps.length;
        L_s.y /= gps.length;
        L_s.z /= gps.length;
        return L_s;
    }

    private Point3 correctLight(Mat result){
        L_f.x = 0;
        L_f.y = 0;
        L_f.z = 0;
        PriorityQueue<WrapPoint> grayPoints = new PriorityQueue<>(cmp);

        for(int i = 0; i < curRGBFrame.rows(); i = i + 4){
            for(int j = 0; j < curRGBFrame.cols(); j = j + 4){
                double[] pix = curRGBFrame.get(i, j);
                //计算灰度指数
                Point3 pix3 = new Point3(pix[0], pix[1], pix[2]);
                double g = calAngel(pix3, L_ref);
                grayPoints.add(new WrapPoint(i, j, g));
            }
        }

        for (int i = 0; i < grayPointNumber; i++) {
            WrapPoint p = grayPoints.poll();
            double[] RGB = curRGBFrame.get((int) p.x, (int) p.y);

            if (showInfo) Imgproc.circle(result, p, 1, blue, -1);

            L_f.x += RGB[0];
            L_f.y += RGB[1];
            L_f.z += RGB[2];
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
        if(isRecording) LOG.write("WB", LightSourceMean / L_f.x, LightSourceMean / L_f.y, LightSourceMean / L_f.z);
        Scalar[] gain = {gainR, gainG, gainB};
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
        curRGBFrame = inputFrame.rgba();
        curGrayFrame = inputFrame.gray();
        curPose = ImuListener.getInstance().getPose();

        Mat result = (preRGBFrame==null) ?curRGBFrame :preRGBFrame.clone();

        singleFrameLightEst(curRGBFrame, result);

        //非第一帧，有前一帧
        if (!preGrayPoints.empty()){

            // 计算光流，计算映射过后的灰点位置集合
            Point3 L_s = grayPointShiftLightEst(result);

            //计算角误差与加权权重
            double theta_s = calAngel(L_f_pre, L_s);
            double theta_0 = calAngel(L_0_pre, L_0);
            // 系数越大，得到的结果波动越大，调整为10或3比较稳定
            double w = Math.exp(-10 * Math.min(theta_0, theta_s) * Math.min(theta_0, theta_s));

            //加权融合光源
            L_ref = new Point3(
                    L_f_pre.x * w + L_0.x * (1-w),
                    L_f_pre.y * w + L_0.y * (1-w),
                    L_f_pre.z * w + L_0.z * (1-w)
            );

            //根据融合光源估计重新进行灰点检测
            correctLight(result);

            adjustWhiteBalance(result, L_f);
            L_f_pre = L_f;

            //<editor-fold desc="输出相关代码，与算法无关">
            if (showInfo) {
                int width = 960;
                int radius = 40;

                Imgproc.circle(result, new Point(width - 3 * radius, radius), radius, new Scalar(L_0.x, L_0.y, L_0.z), -1);
                Imgproc.circle(result, new Point(width - radius, radius), radius, new Scalar(L_s.x, L_s.y, L_s.z), -1);
                Imgproc.circle(result, new Point(width - 3 * radius, 3 * radius), radius, new Scalar(L_ref.x, L_ref.y, L_ref.z), -1);
                Imgproc.circle(result, new Point(width - radius, 3 * radius), radius, new Scalar(L_f.x, L_f.y, L_f.z), -1);

                Imgproc.putText(result, "L0       Ls", new Point(width - 3 * radius, radius), Imgproc.FONT_HERSHEY_TRIPLEX, 0.5, new Scalar(0, 255, 0));
                Imgproc.putText(result, "Lref     Lf", new Point(width - 3 * radius, 3 * radius), Imgproc.FONT_HERSHEY_TRIPLEX, 0.5, new Scalar(0, 255, 0));
            }

            if (isRecording) {
                SWB_only = preRGBFrame.clone();
                adjustWhiteBalance(SWB_only, L_0);

                LOG.write("L_0", L_0);
                LOG.write("L_s", L_s);
                LOG.write("L_ref", L_ref);
                LOG.write("L_f", L_f);
                LOG.write("ts", false, theta_s);
                LOG.write("t0", false, theta_0);
                LOG.write("w", false, w);

                videoWriter_org.write(preRGBFrame);
                videoWriter_imu.write(result);
                videoWriter_SWB.write(SWB_only);
            }
            //</editor-fold>
        } else {//为第一帧,单帧检测结果即为最终光源估计（论文中为最终光源融合结果L_ref，这里不再通过新的灰度指数计算方法进行灰点检测）
            L_f_pre = L_0;
        }

        L_0_pre = L_0;
        preGrayPoints = curGrayPoints;

        preGrayFrame = curGrayFrame;
        prePose = curPose;
        preRGBFrame = curRGBFrame;

        return WhiteBalanceOn ? result : inputFrame.rgba();
    }
}
