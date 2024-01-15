package com.liinx.bgid;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class CONFIG {
    /* ----算法配置---- */
    // 聚类簇数量
    public static final int nCluster = 3;
    // Kmeans聚类的迭代次数
    public static final int terminalTime = 15;
    // 聚类权重tau
    public static final float tau = 0.1f;
    // 灰度指数的threshold
    public static final int grayPointNumber = 100;
    public static final double downsampleFactor = 0.25;

    /* ----项目配置---- */
    public static final String accePath = "/storage/emulated/0/Download/acce.txt";
    public static final String gyroPath = "/storage/emulated/0/Download/gyro.txt";
    public static final String videoPath = "/storage/emulated/0/Download/video.mp4";
    public static final String timestampPath = "/storage/emulated/0/Download/ts.txt";
    public static final String featurePath = "/storage/emulated/0/Download/feature.dat";
    public static final String outputPath = "/storage/emulated/0/Download/result.avi";
    public static final float xRate = 1920.0f / 1280.0f;
    public static final float yRate = 1080.0f / 720.0f;
    public static final int videoHeight = 720;
    // 特征轨迹起始帧
    public static final int beginFrame = 140;
    // 结束帧
    public static final int endFrame = 180;



    /* ----硬件相关参数---- */
    // 相机内参矩阵
    public static final Mat innerMat = new Mat(3, 3, CvType.CV_64F);
    public static final Mat M1 = new Mat(3, 3, CvType.CV_64F);
    public static final Mat M2 = new Mat(3, 3, CvType.CV_64F);
    // 加速度计零偏修正
    public static final Mat biasMat = new Mat(3, 1, CvType.CV_32F);
    // 加速度计正交修正，猜的
    public static final Mat tranMat = new Mat(3, 3, CvType.CV_32F);
    // 加速度计尺度修正
    public static final Mat scaleMat = new Mat(3, 3, CvType.CV_32F);
    // 上面两个乘起来
    public static final Mat MulMat;
    // 视频时间戳的延迟
    public static final long timeDelay = 17000000L;

    static {
        // MATLAB camera calibration
        double[] inner = {
//                1490.1,    0.0, 533.1,
//                   0.0, 1481.7, 974.5,
//                   0.0,    0.0,   1.0
            668.386577725047,   0,                  481.035022498242,
            0,                  667.642510754716,   362.672901347417,
            0,                  0,                  1
        };
        innerMat.put(0,0, inner);

        double[] m1 = {
                0.0, -1.0, videoHeight,
                1.0, 0.0, 0.0,
                0.0, 0.0, 1.0
        };
        M1.put(0, 0, m1);

        double[] m2 = {
                0.0, 1.0, 0.0,
                -1.0, 0.0, videoHeight,
                0.0, 0.0, 1.0
        };
        M2.put(0, 0, m2);

        float[] bias = {0.0365f, 0.0403f, 0.0838f};
        biasMat.put(0, 0, bias);

        float[] tran = {
                1.0000f, -0.0004f, 0.0010f,
                     0f,  1.0000f, 0.0004f,
                     0f,       0f, 1.0000f
        };
        tranMat.put(0, 0, tran);

        float[] scale = {
                0.9981f, 0f, 0f,
                0f, 0.9970f, 0f,
                0f, 0f, 0.9991f
        };
        scaleMat.put(0, 0, scale);

        MulMat = tranMat.matMul(scaleMat);
    }
}
