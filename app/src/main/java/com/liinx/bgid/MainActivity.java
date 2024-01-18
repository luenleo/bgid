package com.liinx.bgid;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;

import java.util.Collections;
import java.util.List;


public class MainActivity extends CameraActivity {

    private final static String TAG = "BGID";

    private CameraBridgeViewBase mCameraView;

    private Sensor accelerometer;
    private Sensor gyroscope;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        OpenCVLoader.initDebug();

        FrameProcess.activity = this;
        Button button = findViewById(R.id.button);
        button.setOnClickListener(FrameProcess.getInstance());
        button = findViewById(R.id.switchMode);
        button.setOnClickListener(FrameProcess.getInstance());

        // CameraViw
        mCameraView = findViewById(R.id.jcv);
        mCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);//启用后置摄像头
        mCameraView.setVisibility(View.VISIBLE);
        mCameraView.setMaxFrameSize(-1, 720);//设置最大帧
        mCameraView.setCvCameraViewListener(FrameProcess.getInstance());//设置帧监听器

        // Sensor
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);//获得传感器管理器实例
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);//获得线性加速度计实例
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);//获得未校准的陀螺仪实例
        ImuListener.activity = this;//设置ImuListener类的静态属性activity为当前Activity的实例
        sensorManager.registerListener(ImuListener.getInstance(), accelerometer, SensorManager.SENSOR_DELAY_FASTEST);//注册线性加速度传感器的监听器
        sensorManager.registerListener(ImuListener.getInstance(), gyroscope, SensorManager.SENSOR_DELAY_FASTEST);//同上
    }

    @Override
    public void onPause(){
        super.onPause();
        if (mCameraView != null)
            mCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        mCameraView.enableView();
    }

    @Override
    public void onDestroy() {
        if (mCameraView != null)
            mCameraView.disableView();
        super.onDestroy();
    }

    /**
     * 获取安卓摄像头权限，高版本不可少
     */
    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mCameraView);
    }
}