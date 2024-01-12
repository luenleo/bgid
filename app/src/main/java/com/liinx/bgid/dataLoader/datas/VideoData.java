package com.liinx.bgid.dataLoader.datas;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;

public class VideoData {
    private VideoCapture video;
    private HashMap<Long, Integer> time2frame;
    private ArrayList<Long> frame2time;

    public Long getTimestampByFrame(int frame){
        return frame2time.get(frame);
    }

    public VideoData(VideoCapture video, HashMap<Long, Integer> time2frame, ArrayList<Long> frame2time) {
        this.video = video;
        this.time2frame = time2frame;
        this.frame2time = frame2time;
    }

    public VideoCapture getVideo() {return video;}
}
