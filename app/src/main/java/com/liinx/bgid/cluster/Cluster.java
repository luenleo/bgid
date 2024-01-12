package com.liinx.bgid.cluster;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.util.ArrayList;
import java.util.Iterator;

public class Cluster<E extends ClusterAtom<E>> implements Iterable<E>{
    private ArrayList<E> cluster;
    private Mat center = null;

    public Mat getCenter(){
        if (center == null) {
            if (cluster.isEmpty()){
                return null;
            }
            Mat center = new Mat(cluster.get(0).getCoordinate().size(), CvType.CV_32F);
            cluster.forEach(a -> Core.add(center, a.getCoordinate(), center));
            Core.divide(center, Scalar.all(cluster.size()), center);
            return center;
        }
        else
            return center;
    }

    public void addAtom(E atom){
        cluster.add(atom);
        atom.setCluster(this);
        center = null;
    }
    public void removeAtom(E atom){
        cluster.remove(atom);
        atom.setCluster(null);
        center = null;
    }


    public Cluster() {cluster = new ArrayList<>();}
    public ArrayList<E> getCluster() {return cluster;}
    public void setCenter(Mat center) {this.center = center;}
    public int size(){return cluster.size();}
    @Override
    public Iterator<E> iterator() {return cluster.iterator();}
}
