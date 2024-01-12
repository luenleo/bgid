package com.liinx.bgid.dataLoader;

import android.util.Log;

import com.liinx.bgid.CONFIG;
import com.liinx.bgid.dataLoader.datas.AccelerationData;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.FileReader;
import java.util.Scanner;

public class AcceLoader {
    private AccelerationData data;

    public AcceLoader(String filePath){
        Long time;
        Mat acceMat = new Mat(3, 1, CvType.CV_32F);
        Mat temp = new Mat(3, 1, CvType.CV_32F);
        Mat temp0;
        float[] a = new float[3];
        data = new AccelerationData();
        try {
            FileReader file = new FileReader(filePath);
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLong()){
                time = scanner.nextLong();
                a[0] = scanner.nextFloat();
                a[1] = scanner.nextFloat();
                a[2] = scanner.nextFloat();
                acceMat.put(0, 0, a);

                // 加速度计标定
                Core.add(acceMat, CONFIG.biasMat, temp);
                temp0 = CONFIG.MulMat.matMul(temp);
                temp0.get(0, 0, a);

                data.addData(time, a[0], a[1], a[2]);
            }
            scanner.close();
            file.close();
            Log.i("BGID", "成功加载"+data.size()+"条加速度计数据并修正");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public AccelerationData getData() {
        return data;
    }
}
