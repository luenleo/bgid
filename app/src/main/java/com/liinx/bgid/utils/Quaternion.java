package com.liinx.bgid.utils;

import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class Quaternion {
    public double w, x, y, z;

    public Quaternion(double w, double x, double y, double z) {
        this.w = w;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Quaternion add(Quaternion q){return new Quaternion(w+q.w, x+q.x, y+q.y, z+q.z);}
    public Quaternion sub(Quaternion q){return new Quaternion(w-q.w, x-q.x, y-q.y, z-q.z);}
    public Quaternion mul(Quaternion q){
        double new_w = w*q.w - x*q.x - y*q.y - z*q.z;
        double new_x = x*q.w + w*q.x - z*q.y + y*q.z;
        double new_y = y*q.w + z*q.x + w*q.y - x*q.z;
        double new_z = z*q.w - y*q.x + x*q.y + w*q.z;
        return new Quaternion(new_w, new_x, new_y, new_z);
    }

    public Quaternion conjugate(){
        return new Quaternion(w, -x, -y, -z);
    }
    public double norm(){
        return sqrt(w*w + x*x + y*y + z*z);
    }
    public Quaternion inv(){
        Quaternion conjugate = conjugate();
        double norm = norm();
        double temp = norm * norm;
        return new Quaternion(conjugate.w / temp, conjugate.x / temp, conjugate.y / temp, conjugate.z / temp);
    }
    static public Quaternion eulerToQuaternion(double roll, double pitch, double hdg) {
        Quaternion q_ret = new Quaternion(0,0,0,0);
        double cosRoll = cos(roll  * 0.5);
        double sinRoll = sin(roll  * 0.5);
        double cosPitch = cos(pitch * 0.5);
        double sinPitch = sin(pitch * 0.5);
        double cosHeading = cos(hdg   * 0.5);
        double sinHeading = sin(hdg   * 0.5);
        q_ret.w = cosRoll * cosPitch * cosHeading + sinRoll * sinPitch * sinHeading;
        q_ret.x = sinRoll * cosPitch * cosHeading - cosRoll * sinPitch * sinHeading;
        q_ret.y = cosRoll * sinPitch * cosHeading + sinRoll * cosPitch * sinHeading;
        q_ret.z = cosRoll * cosPitch * sinHeading - sinRoll * sinPitch * cosHeading;
        return q_ret;
    }
    static public Quaternion slerp(Quaternion q1, Quaternion q2, double t) {
        Quaternion result = new Quaternion(0,0,0,0);
        Quaternion start = q1;
        Quaternion end = q2;
        double cosa = start.w * end.w + start.x * end.x + start.y * end.y + start.z * end.z;
        double k0, k1;
        if(cosa < 0.0){
            end.w = -end.w;
            end.x = -end.x;
            end.y = -end.y;
            end.z = -end.z;
            cosa = -cosa;
        }
        if(cosa > 0.9995){
            k0 = 1.0 - t;
            k1 = t;
        } else {
            double sina = sqrt(1.0 - cosa * cosa);
            double a = atan2(sina, cosa);
            k0 = sin((1.0 - t) * a) / sina;
            k1 = sin(t*a) / sina;
        }
        result.w = start.w * k0 + end.w * k1;
        result.x = start.x * k0 + end.x * k1;
        result.y = start.y * k0 + end.y * k1;
        result.z = start.z * k0 + end.z * k1;
        return result;
    }
    public Mat quaternionToR() {
        Mat mat = new Mat(3, 3, CvType.CV_64F);
        double w = this.w;
        double x = this.x;
        double y = this.y;
        double z = this.z;
        mat.put(0, 0, 1 - 2 * y * y - 2 * z * z);
        mat.put(0, 1, 2 * x * y - 2 * z * w);
        mat.put(0, 2, 2 * x * z + 2 * y * w);
        mat.put(1, 0, 2 * x * y + 2 * z * w);
        mat.put(1, 1, 1 - 2 * x * x - 2 * z * z);
        mat.put(1, 2, 2 * y * z - 2 * x * w);
        mat.put(2, 0, 2 * x * z - 2 * y * w);
        mat.put(2, 1, 2 * y * z + 2 * x * w);
        mat.put(2, 2, 1 - 2 * x * x - 2 * y * y);
        return mat;
    }
    static public Mat rToEuler(Mat R) {
        double[]  temp1 = new double[1];
        double[]  temp2 = new double[1];
        double[]  temp3 = new double[1];
        R.get(2, 1, temp1);
        R.get(2, 2, temp2);
        double x = atan2(temp1[0], temp2[0]);
        R.get(2, 0, temp1);
        R.get(2, 1, temp2);
        R.get(2, 2, temp3);
        double y = atan2(-temp1[0], sqrt(temp2[0] * temp2[0] + temp3[0] * temp3[0]));
        R.get(1, 0, temp1);
        R.get(0, 0, temp2);
        double z = atan2(temp1[0], temp2[0]);

        Mat eu = new Mat(3, 1, CvType.CV_64F);
        eu.put(0, 0, x);
        eu.put(0, 1, y);
        eu.put(0, 2, z);
        return eu;
    }

    static public Quaternion interpolate(Quaternion q1, Quaternion q2, float ratio){
        return new Quaternion(
                q1.w * (1-ratio) + q2.w * ratio,
                q1.x * (1-ratio) + q2.x * ratio,
                q1.y * (1-ratio) + q2.y * ratio,
                q1.z * (1-ratio) + q2.z * ratio
        );
    }
}