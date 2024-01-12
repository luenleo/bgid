package com.liinx.bgid.core;

import android.util.Log;

import androidx.annotation.NonNull;

import com.liinx.bgid.CONFIG;
import com.liinx.bgid.cluster.Cluster;
import com.liinx.bgid.cluster.ClusterAtom;
import com.liinx.bgid.cluster.SpectralClustering;
import com.liinx.bgid.dataLoader.datas.SyncFrame;
import com.liinx.bgid.dataLoader.datas.Trajectory;

import org.opencv.core.Mat;

import java.util.ArrayList;

public class BKSelect {
    public static class TrajectoryAtom extends ClusterAtom<TrajectoryAtom>{
        private Trajectory element;
        private int frameNumber;

        public TrajectoryAtom(Trajectory p, int frame, Mat coor) {
            super(coor);
            element = p;
            frameNumber = frame;
        }

        @Override
        public float similarCal(TrajectoryAtom anotherAtom) {
            int begin = Math.max(element.getBeginFrame(), anotherAtom.getElement().getBeginFrame());

            // affinity
            float A = 0;
            for (int i = begin; i <= frameNumber; i++) {
                A += element.getLocationByFrame(i).calTranDistance(
                        anotherAtom.getElement().getLocationByFrame(i)
                );
            }
            A = (float) Math.sqrt(A);

            // spatial relationship
            float B = element.getLocationByFrame(frameNumber).calDistance(
                    anotherAtom.getElement().getLocationByFrame(frameNumber)
            );

            return A + CONFIG.tau * B;
        }

        public Trajectory getElement() {return element;}
        public int getFrameNumber() {return frameNumber;}
        @NonNull
        @Override
        public TrajectoryAtom clone() {
            return null;
        }
    }

    static public ArrayList<Cluster<TrajectoryAtom>> cluster(SyncFrame frame) {
        // 聚类
        ArrayList<TrajectoryAtom> ts = new ArrayList<>();
        for (Trajectory t : frame) {
            ts.add(new TrajectoryAtom(t, frame.getFrameInVideo(), null));
        }
        Log.i("BGID", "\t聚类: 封装完成");
        return SpectralClustering.run(ts, CONFIG.nCluster);
    }

    static public Cluster<TrajectoryAtom> selectMinCluster(ArrayList<Cluster<TrajectoryAtom>> clusters){
        // 选择最小簇
        Cluster<TrajectoryAtom> minCluster = null;
        float minLength = 999999999999f;
        for (Cluster<TrajectoryAtom> c : clusters){
            float l = averageLength(c);
            if (l < minLength){
                minLength = l;
                minCluster = c;
            }
        }
        return minCluster;
    }

    private static float averageLength(Cluster<TrajectoryAtom> c){
        float length = 0;
        for (TrajectoryAtom t : c){
            length += t.getElement().getLocationByFrame(t.frameNumber).calTranLen();
        }
        return length / c.size();
    }
}
