package com.liinx.bgid.dataLoader;

import android.util.Log;

import com.liinx.bgid.dataLoader.datas.GyroscopeData;

import java.io.FileReader;
import java.util.Scanner;

public class GyroLoader {
    private GyroscopeData data;

    public GyroLoader(String filePath){
        Long time;
        float a0;
        float a1;
        float a2;
        data = new GyroscopeData();
        try {
            Scanner scanner = new Scanner(new FileReader(filePath));
            while (scanner.hasNextLong()){
                time = scanner.nextLong();
                a0 = scanner.nextFloat();
                a1 = scanner.nextFloat();
                a2 = scanner.nextFloat();
                data.addData(time, a0, a1, a2);
            }
            Log.i("BGID", "成功加载"+data.size()+"条陀螺仪数据");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public GyroscopeData getData() {
        return data;
    }
}
