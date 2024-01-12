package com.liinx.bgid.cluster;

import androidx.annotation.NonNull;

import org.opencv.core.Core;
import org.opencv.core.Mat;

public abstract class ClusterAtom<E extends ClusterAtom<E>> {
    private Cluster<E> cluster;
    private Mat coordinate;

    @NonNull
    abstract public E clone();

    public float similarCal(E anotherAtom){
        return coorDistance(anotherAtom.getCoordinate());
    }

    public boolean isSameCluster(E atom){
        return atom.getCluster()==cluster;
    }

    public float coorDistance(Mat coor){
        Mat newMat = new Mat();
        Core.subtract(coordinate, coor, newMat);
        return (float) Core.norm(newMat, Core.NORM_L2);
    }

    public void removeCluster(){
        if (cluster != null) {
            cluster.removeAtom((E) this);
            cluster = null;
        }
    }

    public ClusterAtom(Mat coordinate){this.coordinate = coordinate;}
    public Cluster<E> getCluster(){return cluster;}
    public void setCluster(Cluster<E> cluster){this.cluster = cluster;}
    public Mat getCoordinate(){return coordinate;}
    public void setCoordinate(Mat coordinate) {this.coordinate = coordinate;}
}