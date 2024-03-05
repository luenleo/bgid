package com.liinx.bgid.utils;

import android.widget.Toast;

import com.liinx.bgid.FrameProcess;

import org.opencv.core.Mat;
import org.opencv.core.Point3;
import org.opencv.core.Size;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DataOutput {
    private String rootPath;
    private Map<String, FileWriter> files = new HashMap<>();
    private Map<String, VideoWriter> videos = new HashMap<>();

    public DataOutput(String rootPath) {
        this.rootPath = rootPath;
    }

    public void release(){
        int Nv = 0, Nf = 0;
        for (FileWriter fileWriter : files.values()){
            try {
                fileWriter.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Nv++;
        }
        for (VideoWriter videoWriter : videos.values()){
            videoWriter.release();
            Nf++;
        }

        Toast.makeText(
            FrameProcess.activity,
            "共保存"+Nf+"个文件和"+Nv+"个视频在"+rootPath,
            Toast.LENGTH_LONG
        ).show();
    }

    public void write(String type, Mat frame){
        VideoWriter writer;
        if (videos.containsKey(type)){
            writer = videos.get(type);
        }
        else {
            writer = new VideoWriter(
                rootPath + type + ".avi",
                Videoio.CAP_OPENCV_MJPEG,
                VideoWriter.fourcc('M', 'J', 'P', 'G'),
                12,
                new Size(960, 720),
                true
            );
            videos.put(type,writer);
        }

        writer.write(frame);
    }

    public void write(String type, boolean separator, double... data) {
        FileWriter writer;
        if (files.containsKey(type)) {
            writer = files.get(type);
        } else {
            try {
                writer = new FileWriter(rootPath + type + ".txt");
                writer.write("");
                writer.close();
                writer = new FileWriter(rootPath + type + ".txt", true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            files.put(type, writer);
        }

        StringBuilder temp = new StringBuilder();
        for (double x : data)
            if (separator) temp.append(String.format("%1.6f", x)).append(',').append(' ');
            else temp.append(String.format("%1.6f", x));
        temp.append('\n');

        try {
            writer.append(temp.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void write(String type, double... data) {
        write(type, true, data);
    }

    public void write(String type, Point3 p) {
        write(type, p.x, p.y, p.z);
    }

    public void write(String type, Quaternion q){
        write(type, q.w, q.x, q.y, q.z);
    }
}
