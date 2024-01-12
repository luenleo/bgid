package com.liinx.bgid;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.widget.TextView;

import com.liinx.bgid.utils.Interpolate;
import com.liinx.bgid.utils.MadgwickAHRS;
import com.liinx.bgid.utils.Quaternion;

public class ImuListener implements SensorEventListener {

    private static final ImuListener listener = new ImuListener();
    private ImuListener(){}
    public static ImuListener getInstance(){return listener;}

    public static MainActivity activity;

    private final MadgwickAHRS madgwickAHRS = new MadgwickAHRS();

    private long preAcceTimestamp = -1;
    private long preGyroTimestamp = -1;
    private float[] acceValues;
    private float[] gyroValues;

    @Override
    public void onSensorChanged(SensorEvent event) {
        // TODO: IMU-Camera标定
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
            if (preAcceTimestamp != -1 && preGyroTimestamp != -1 && event.timestamp > preGyroTimestamp){
                float[] interAcce = Interpolate.run(preAcceTimestamp, acceValues, event.timestamp, event.values, preGyroTimestamp);
                madgwickAHRS.updateIMU(gyroValues[0], gyroValues[1], gyroValues[2], interAcce[0], interAcce[1], interAcce[2]);
                ((TextView) activity.findViewById(R.id.acceText)).setText("Madgwick\n"+madgwickAHRS.getq0()+'\n'+madgwickAHRS.getq1()+'\n'+madgwickAHRS.getq2()+'\n'+madgwickAHRS.getq3());
            }
            preAcceTimestamp = event.timestamp;
            acceValues = event.values;
//            ((TextView) activity.findViewById(R.id.acceText)).setText("ACCELERATION\n"+event.values[0]+'\n'+event.values[1]+'\n'+event.values[2]+"\nMadgwick\n"+madgwickAHRS.getq0()+'\n'+madgwickAHRS.getq1()+'\n'+madgwickAHRS.getq2()+'\n'+madgwickAHRS.getq3());
        }
        else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE_UNCALIBRATED){
            preGyroTimestamp = event.timestamp;
            gyroValues = event.values;
//            ((TextView) activity.findViewById(R.id.gyroText)).setText("GYROSCOPE\n"+event.values[0]+'\n'+event.values[1]+'\n'+event.values[2]+'\n');
        }
    }

    public Quaternion getPose(){
        return new Quaternion(madgwickAHRS.getq0(), -madgwickAHRS.getq1(), madgwickAHRS.getq2(), -madgwickAHRS.getq3());
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
