package com.liinx.bgid.cluster;

import android.util.Log;

import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.Random;
import java.util.stream.IntStream;

public class KmeansClustering {
    public static <T extends ClusterAtom<T>> ArrayList<Cluster<T>> run(ArrayList<T> atoms, int nCluster, int terminalTime){
//        Log.i("BGID", "\tKmeans: 初始化");
        // 初始化聚类中心
        Random r = new Random();
        IntStream ints = r.ints(nCluster, 0, atoms.size());
        ArrayList<Cluster<T>> clusters = new ArrayList<>();
        ints.forEach(i -> {
            Cluster<T> cluster = new Cluster<>();
            cluster.setCenter(atoms.get(i).getCoordinate());
            clusters.add(cluster);
        });

//        Log.i("BGID", "\tKmeans: 开始聚类");
        int N = atoms.size();
        float[][] distance = new float[N][nCluster];
        while(terminalTime-- != 0){
            // 计算各原子与中心的距离
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < nCluster; j++) {
                    Mat center = clusters.get(j).getCenter();
                    if (center != null)
                        distance[i][j] = atoms.get(i).coorDistance(center);
                }
            }

            // 选择最小值作为新簇
            for (int i = 0; i < N; i++) {
                int index = 0;
                for (int j = 1; j < nCluster; j++) {
                    if (distance[i][index] > distance[i][j])
                        index = j;
                }
                atoms.get(i).removeCluster();
                clusters.get(index).addAtom(atoms.get(i));
            }
        }
//        Log.i("BGID", "\tKmeans: 算完了");
        return clusters;
    }
}
