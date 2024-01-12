package com.liinx.bgid.utils;

public class Vector {
    public float e0;
    public float e1;
    public float e2;

    public static Vector interpolate(Vector v1, Vector v2, float ratio){
        return new Vector(
                v1.e0*(1-ratio)+v2.e0*ratio,
                v1.e1*(1-ratio)+v2.e1*ratio,
                v1.e2*(1-ratio)+v2.e2*ratio
        );
    }

    public Vector(){
        e0 = e1 = e2 = 0;
    }

    public Vector(float element0, float element1, float element2) {
        this.e0 = element0;
        this.e1 = element1;
        this.e2 = element2;
    }
}
