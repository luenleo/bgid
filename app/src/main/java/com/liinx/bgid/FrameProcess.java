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
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FrameProcess implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {

    private static final Scalar red      = new Scalar(255,  50,  50);
    private static final Scalar green    = new Scalar( 50, 255,  50);
    private static final Scalar blue     = new Scalar( 50,  50, 255);
    private static final Scalar white    = new Scalar(255, 255, 255);

    private static final FrameProcess fp = new FrameProcess();
    private FrameProcess(){}
    public static FrameProcess getInstance(){
        return fp;
    }

    private Mat preRGBFrame = null;
    private Mat preGrayFrame = null;
    private Mat curRGBFrame = null;
    private Mat curGrayFrame = null;
    private Quaternion prePose = null;
    private Quaternion curPose = null;
    private MatOfPoint2f preGrayPoints = new MatOfPoint2f();
    private MatOfPoint2f curGrayPoints = new MatOfPoint2f();
    private double threshold = 0.001;//灰度指数小于阈值则加入灰点组,后续可以用堆排序实现灰点数量控制
    private Point3 L_f = new Point3(0,0,0);
    private Point3 L_ref = new Point3(0,0,0);
    private Point3 L_0_pre = new Point3(0,0,0);
    private MatOfByte status = null;
    private MatOfFloat err = null;

    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    @Override
    public void onCameraViewStopped() {
    }

    private int counter = 0;
    @Override
    public void onClick(View v) {
        Mat save = new Mat();
        Imgproc.cvtColor(curRGBFrame, save, Imgproc.COLOR_RGBA2BGR);
        String filename = "/storage/emulated/0/Download/photo_"+ counter++ +".jpg";
        Imgcodecs.imwrite(filename, save);
        Toast.makeText(ImuListener.activity, "保存至"+filename, Toast.LENGTH_SHORT).show();
    }
    //灰点检测，光源融合部分
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        curRGBFrame = inputFrame.rgba();
        curGrayFrame = inputFrame.gray();
        curPose = ImuListener.getInstance().getPose();

        Mat result = (preRGBFrame==null) ?curRGBFrame :preRGBFrame.clone();
        preRGBFrame = curRGBFrame;

        //计算L_0
        List<Point> pointList = new ArrayList<>();//储存检测到的灰点坐标
        Point3 L_0 = new Point3(0, 0, 0);//计算单帧灰点检测估计的光源颜色
        for(int i = 0; i < curRGBFrame.rows(); i = i + 10){
            for(int j = 0; j < curRGBFrame.cols(); j = j + 10){
                int count = 0;//对应四个方向
                double sumb = 0;
                double sumr = 0;
                double[] pix = curRGBFrame.get(i, j);
                double I = pix[0] + pix[1] + pix[2];
                double t = Math.log(pix[0]/I);
                double p = Math.log(pix[2]/I);
                if(curRGBFrame.get(i - 1, j) != null){
                    count = count + 1;
                    double I1 = curRGBFrame.get(i - 1, j)[0] + curRGBFrame.get(i - 1, j)[1] + curRGBFrame.get(i - 1, j)[2];
                    double t1 = Math.log(curRGBFrame.get(i - 1, j)[0]/I1);
                    double p1 = Math.log(curRGBFrame.get(i - 1, j)[2]/I1);
                    sumb = sumb + t1 - t;
                    sumr = sumr + p1 - p;
                }
                if(curRGBFrame.get(i, j - 1) != null){
                    count = count + 1;
                    double I2 = curRGBFrame.get(i, j - 1)[0] + curRGBFrame.get(i, j - 1)[1] + curRGBFrame.get(i, j - 1)[2];
                    double t2 = Math.log(curRGBFrame.get(i, j - 1)[0]/I2);
                    double p2 = Math.log(curRGBFrame.get(i, j - 1)[2]/I2);
                    sumb = sumb + t2 - t;
                    sumr = sumr + p2 - p;
                }
                if(curRGBFrame.get(i, j + 1) != null){
                    count = count + 1;
                    double I3 = curRGBFrame.get(i, j + 1)[0] + curRGBFrame.get(i, j + 1)[1] + curRGBFrame.get(i, j + 1)[2];
                    double t3 = Math.log(curRGBFrame.get(i, j + 1)[0]/I3);
                    double p3 = Math.log(curRGBFrame.get(i, j + 1)[2]/I3);
                    sumb = sumb + t3 - t;
                    sumr = sumr + p3 - p;
                }
                if(curRGBFrame.get(i + 1, j) != null){
                    count = count + 1;
                    double I4 = curRGBFrame.get(i + 1, j)[0] + curRGBFrame.get(i + 1, j)[1] + curRGBFrame.get(i + 1, j)[2];
                    double t4 = Math.log(curRGBFrame.get(i + 1, j)[0]/I4);
                    double p4 = Math.log(curRGBFrame.get(i + 1, j)[2]/I4);
                    sumb = sumb + t4 - t;
                    sumr = sumr + p4 - p;
                }
                double G = Math.sqrt(Math.pow(sumb/count, 2) + Math.pow(sumr/count, 2));//该点的灰度指数
                if(G <= threshold){
                    pointList.add(new Point(i, j));
                    //按RGB存储
                    L_0.x = L_0.x + pix[2];
                    L_0.y = L_0.y + pix[1];
                    L_0.z = L_0.z + pix[0];

                }

            }
        }
        curGrayPoints.fromList(pointList);
        L_0.x = L_0.x/curGrayPoints.toList().size();//单帧光源颜色估计
        L_0.y = L_0.y/curGrayPoints.toList().size();
        L_0.z = L_0.z/curGrayPoints.toList().size();
        //非第一帧，有前一帧
        if (!preGrayPoints.empty()){
            // 计算光流，计算映射过后的灰点位置集合
            curGrayPoints = new MatOfPoint2f();
            status = new MatOfByte();
            err = new MatOfFloat();
            Video.calcOpticalFlowPyrLK(preGrayFrame, curGrayFrame, preGrayPoints, curGrayPoints, status, err);

            // 计算旋转矩阵
            Mat R_cur = curPose.quaternionToR();
            Mat R_pre = prePose.quaternionToR();
            Mat R = R_cur.matMul(R_pre.inv());
            Mat cameraR = CONFIG.innerMat.matMul(R).matMul(CONFIG.innerMat.inv());
            //Mat rotateMat = CONFIG.M1.inv().matMul(cameraR).matMul(CONFIG.M2.inv());

            // 对个特征点计算旋转分量
            Point[] pre = preGrayPoints.toArray(), cur = curGrayPoints.toArray();
            List<Point> mapped = new ArrayList<>();
            byte[] s = status.toArray();
            for (int i = 0; i < pre.length; i++) {
            //Imgproc.circle(curRGBFrame, pre[i], 3, green, Imgproc.FILLED);
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
                    if(0 <= pre[i].x + u_trans && pre[i].x + u_trans < curRGBFrame.rows() && pre[i].y + v_trans >= 0 && pre[i].y + v_trans < curRGBFrame.cols()){
                        //未出界，加入到映射灰点集
                        Point added = new Point(pre[i].x + u_trans, pre[i].y + v_trans);
                        mapped.add(added);
                        Imgproc.circle(result, added, 1, red, -1);
                    }
                }
            }
            if(mapped.size() < pre.length/2){
                curGrayPoints.fromArray(pre);//多数出界回退到上一帧灰点位置集合
            }else{
                curGrayPoints.fromList(mapped);
            }
            //计算当前帧的L_s
            Point3 L_s = new Point3(0,0,0);
            Point[] ls = curGrayPoints.toArray();
            for(int i = 0; i < curGrayPoints.toArray().length; i++){
                L_s.x = L_s.x + curRGBFrame.get((int)ls[i].x, (int)ls[i].y)[2];
                L_s.y = L_s.y + curRGBFrame.get((int)ls[i].x, (int)ls[i].y)[1];
                L_s.z = L_s.z + curRGBFrame.get((int)ls[i].x, (int)ls[i].y)[0];
            }
            L_s.x = L_s.x/ ls.length;
            L_s.y = L_s.y/ ls.length;
            L_s.z = L_s.z/ ls.length;

            //计算角误差与加权权重
            double theta_s = Math.acos(L_s.dot(L_f)
                    /(Math.sqrt(L_s.x*L_s.x + L_s.y*L_s.y + L_s.z*L_s.z)
                            *Math.sqrt(L_f.x*L_f.x + L_f.y*L_f.y + L_f.z*L_f.z)));
            double theta_0 = Math.acos(L_0.dot(L_0_pre)
                    /(Math.sqrt(L_0.x*L_0.x + L_0.y*L_0.y + L_0.z*L_0.z)
                            *Math.sqrt(L_0_pre.x*L_0_pre.x + L_0_pre.y*L_0_pre.y + L_0_pre.z*L_0_pre.z)));
            double w = Math.exp(-0.3*Math.min(theta_0, theta_s)*Math.min(theta_0, theta_s));

            //加权融合光源
            Point3 mid_L_f = new Point3(L_f.x*w, L_f.y*w, L_f.z*w);
            Point3 mid_L_0 = new Point3(L_0.x*(1-w), L_0.y*(1-w), L_0.z*(1-w));
            L_ref = new Point3(mid_L_0.x + mid_L_f.x, mid_L_0.y + mid_L_f.y, mid_L_0.z + mid_L_f.z);

            //根据融合光源估计重新进行灰点检测
            L_f.x = 0;
            L_f.y = 0;
            L_f.z = 0;
            List<Point> pl = new ArrayList<>();
            for(int i = 0; i < curRGBFrame.rows(); i = i + 10){
                for(int j = 0; j < curRGBFrame.cols(); j = j + 10){
                    double[] pix = curRGBFrame.get(i, j);
                    //计算灰度指数
                    Point3 pix3 = new Point3(pix[2], pix[1], pix[0]);
                    double g = Math.acos(pix3.dot(L_ref)
                    /(Math.sqrt(pix3.x*pix3.x + pix3.y*pix3.y + pix3.z*pix3.z)
                    *Math.sqrt(L_ref.x*L_ref.x + L_ref.y*L_ref.y + L_ref.z*L_ref.z)));
                    if(g <= threshold){
                        pl.add(new Point(i, j));
                        L_f.x += pix3.x;
                        L_f.y += pix3.y;
                        L_f.z += pix3.z;
                    }
                }
            }
            curGrayPoints.fromList(pl);
            L_f.x = L_f.x/curGrayPoints.toList().size();
            L_f.y = L_f.y/curGrayPoints.toList().size();
            L_f.z = L_f.z/curGrayPoints.toList().size();

            //进行白平衡
            List<Mat> channels = Arrays.asList(new Mat(), new Mat(), new Mat());
            Core.split(curRGBFrame, channels);
            double LightSourceMean = (L_f.x + L_f.y + L_f.z)/3.0;
            Scalar gainR = new Scalar(LightSourceMean/L_f.x) ;
            Scalar gainG = new Scalar(LightSourceMean/L_f.y) ;
            Scalar gainB = new Scalar(LightSourceMean/L_f.z) ;
            Scalar[] gain = {gainB, gainG, gainR};
            for(int i = 0; i < 3; i++){
                Mat channel = channels.get(i);
                Core.multiply(channel, gain[i], channel);
            }
            Core.merge(channels, curRGBFrame);


        }else{//为第一帧,单帧检测结果即为最终光源估计（论文中为最终光源融合结果L_ref，这里不再通过新的灰度指数计算方法进行灰点检测）
            L_f = L_0;
            L_0_pre = L_0;
        }
        preGrayPoints = curGrayPoints;
        preGrayFrame = curGrayFrame;
        prePose = curPose;
        return result;
    }
}
