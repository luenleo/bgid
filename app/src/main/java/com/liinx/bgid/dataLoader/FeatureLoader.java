package com.liinx.bgid.dataLoader;

import android.util.Log;

import com.liinx.bgid.CONFIG;
import com.liinx.bgid.dataLoader.datas.FeaturePointData;
import com.liinx.bgid.dataLoader.datas.Trajectory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;

public class FeatureLoader {
    private FeaturePointData data;
    public FeatureLoader(String filePath){
        try {
            FileReader fileReader = new FileReader(filePath);
            Scanner scanner = new Scanner(fileReader);
            int frameSpan = scanner.nextInt();
            if (scanner.hasNextInt()){
                data = new FeaturePointData(CONFIG.beginFrame, CONFIG.endFrame);
                if (scanner.hasNextInt()) {
                    int trajectoryNumber = scanner.nextInt();
                    for (int i = 0; i < trajectoryNumber; i++) {
                        // 不明所以的0
                        scanner.nextInt();
                        // 轨迹持续的帧数
                        int trajectoryDuration = scanner.nextInt();
                        Trajectory t = new Trajectory(trajectoryDuration);

                        boolean setBeginFrame = true;
                        for (int j = 0; j < trajectoryDuration; j++) {
                            float x = scanner.nextFloat();
                            float y = scanner.nextFloat();
                            int frame = scanner.nextInt();
                            frame += CONFIG.beginFrame;
                            t.addData(x, y, frame);
                            if (setBeginFrame){
                                t.setBeginFrame(frame);
                                setBeginFrame = false;
                            }
                        }
                        data.addTrajectory(t);
                    }
                }
            }
            scanner.close();
            fileReader.close();
            Log.i("BGID", "成功加载"+data.size()+"条轨迹");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public FeaturePointData getData() {
        return data;
    }
}
