package com.liinx.bgid.dataLoader;

import android.util.Log;

import com.liinx.bgid.CONFIG;
import com.liinx.bgid.dataLoader.datas.VideoData;

import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class VideoLoader {
    // Time delay for Mi 8
    private long timeDelay = CONFIG.timeDelay;
    private VideoData video;


    public VideoLoader(String videoPath, String filePath){
        VideoCapture video = new VideoCapture(videoPath, Videoio.CAP_ANDROID);
        Log.i("BGID", "加载视频："+video.isOpened());
        HashMap<Long, Integer> time2frame;
        ArrayList<Long> frame2time;

        // 加载视频的时间戳标记
        try {
            FileReader file = new FileReader(filePath);
            Scanner scanner = new Scanner(file);
            time2frame = new HashMap<>();
            frame2time = new ArrayList<>();
            int counter = 0;
            while(scanner.hasNextLong()) {
                Long timestamp = scanner.nextLong() + timeDelay;
                time2frame.put(timestamp, counter++);
                frame2time.add(timestamp);
            }
            file.close();
            scanner.close();
            Log.i("BGID", "成功加载"+frame2time.size()+"条时间戳");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        this.video = new VideoData(video, time2frame, frame2time);
    }

    public VideoData getVideo() {
        return video;
    }
}
