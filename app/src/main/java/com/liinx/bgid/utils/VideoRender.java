package com.liinx.bgid.utils;


import android.util.Log;

import com.liinx.bgid.CONFIG;
import com.liinx.bgid.cluster.Cluster;
import com.liinx.bgid.core.BKSelect.TrajectoryAtom;
import com.liinx.bgid.dataLoader.datas.FeaturePointData;
import com.liinx.bgid.dataLoader.datas.Trajectory;
import com.liinx.bgid.dataLoader.datas.VideoData;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import java.util.ArrayList;

public class VideoRender {
    // 安卓唯一(？)支持的编码方式
    private final int fourcc = VideoWriter.fourcc('M', 'J', 'P', 'G');
    
    // 懒得排序，按顺序添加吧
    private VideoCapture video;
    private FeaturePointData featurePoints;
    private ArrayList<Integer> frames;
    private ArrayList<ArrayList<Point>> background;
    private String outputFilePath;

    public VideoRender(VideoData videoData, FeaturePointData featurePoints, String outputFilePath) {
        this.video = videoData.getVideo();
        this.featurePoints = featurePoints;
        this.outputFilePath = outputFilePath;
        this.frames = new ArrayList<>();
        this.background = new ArrayList<>();
    }

    public void test(){
        video.set(Videoio.CAP_PROP_POS_FRAMES, CONFIG.beginFrame);

        Size size = new Size(video.get(Videoio.CAP_PROP_FRAME_WIDTH), video.get(Videoio.CAP_PROP_FRAME_HEIGHT));
        VideoWriter vw = new VideoWriter(
                outputFilePath,
                Videoio.CAP_OPENCV_MJPEG,
                fourcc,
                video.get(Videoio.CAP_PROP_FPS),
                size,
                true
        );
        Mat image = new Mat();
        Mat cvtImage = new Mat();
        assert vw.isOpened();

        for (int i = CONFIG.beginFrame; i < CONFIG.endFrame; i++) {
            video.read(image);
            Imgproc.cvtColor(image, cvtImage, Imgproc.COLOR_RGB2BGR);
            vw.write(cvtImage);
            Log.i("BGID", "test: 渲染第"+i+"帧");
        }
        vw.release();
        Log.i("BGID", "test: 测试完成");
    }

    public void addBackgroundPoint(Cluster<TrajectoryAtom> cluster){
        int frame = cluster.getCluster().get(0).getFrameNumber();
        ArrayList<Point> points = new ArrayList<>();
        for (TrajectoryAtom t : cluster){
            points.add(t.getElement().getLocationByFrame(frame));
        }

        frames.add(frame);
        background.add(points);
    }

    public void render(){
        // 将视频当前帧调整到beginFrame
//        video.set(Videoio.CAP_PROP_POS_FRAMES, CONFIG.beginFrame-1);
        for (int i = 0; i < CONFIG.beginFrame; i++)
            video.grab();

        // 创建视频
        Size size = new Size(video.get(Videoio.CAP_PROP_FRAME_WIDTH), video.get(Videoio.CAP_PROP_FRAME_HEIGHT));
        VideoWriter vw = new VideoWriter(
                outputFilePath,
                Videoio.CAP_OPENCV_MJPEG,
                fourcc,
                video.get(Videoio.CAP_PROP_FPS),
                size
        );
        Mat image = new Mat();
        Mat cvtImage = new Mat();
        Scalar red      = new Scalar(255,  50,  50);
        Scalar green    = new Scalar( 50, 255,  50);
        Scalar blue     = new Scalar( 50,  50, 255);
        Scalar white    = new Scalar(255, 255, 255);

        // 绘制背景点
        for (int i = 0; i < background.size(); i++) {
            Log.i("BGID", "\t渲染第"+i+"帧");

            video.read(image);
            int frame = frames.get(i);
            Log.i("BGID", "\t读取帧"+frame);

            // 特征点设置为蓝色
            ArrayList<Trajectory> ts = featurePoints.getFeatureByFrame(frame);
            for (Trajectory t : ts) {
                Point p = t.getLocationByFrame(frame);
//                Imgproc.circle(image, new org.opencv.core.Point(p.x, p.y), 5, blue, Imgproc.FILLED);
                Imgproc.line(image, new org.opencv.core.Point(p.x, p.y), new org.opencv.core.Point(p.x+p.R_dx, p.y+p.R_dy), red, 3);
                Imgproc.line(image, new org.opencv.core.Point(p.x, p.y), new org.opencv.core.Point(p.x+p.T_dx, p.y+p.T_dy), blue, 3);
                Point p_next = t.getLocationByFrame(frame+1);
                if (p_next != null)
                    Imgproc.line(image, new org.opencv.core.Point(p.x, p.y), new org.opencv.core.Point(p_next.x, p_next.y), white, 3);
            }

            // 背景点设置为绿色
            for (Point p : background.get(i)){
                Imgproc.circle(image, new org.opencv.core.Point(p.x, p.y), 5, green, Imgproc.FILLED);
            }

            // 从RGB转化为BGR给安卓编码
            Imgproc.cvtColor(image, cvtImage, Imgproc.COLOR_RGB2BGR);
            vw.write(cvtImage);
        }

        vw.release();
        video.release();

        Log.i("BGID", "render: 视频渲染完成");
    }
}
