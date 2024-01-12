package com.liinx.bgid.cluster;

import android.util.Log;

import androidx.annotation.NonNull;

import com.liinx.bgid.CONFIG;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Range;
import org.opencv.core.Scalar;

import java.util.ArrayList;

public class SpectralClustering{
    public static void test(){
        float[] a = {2.0F, 1.0F, 0.0F, 1.0F, 3.0F, 1.0F, 0.0F, 1.0F, 2.0F};
        Mat m = new Mat(3, 3, CvType.CV_32F);
        m.put(0, 0, a);

        Mat value = new Mat();
        Mat vector = new Mat();
        Core.eigen(m, value, vector);

        Mat F = vector.submat(new Range(0, 3), Range.all());

        Log.i("TESTING", "Before: "+F.dump());
        for (int i = 0; i < F.rows(); i++) {
            Mat f = F.row(i);
            double norm = Core.norm(f, Core.NORM_L2);
            Core.divide(f, Scalar.all(norm), f);
        }
        Log.i("TESTING", "After: "+F.dump());

    }

    /**
     * 谱聚类算法，详见<a href='https://zhuanlan.zhihu.com/p/29849122'>谱聚类算法</a>
     *
     * @param atoms     符合接口的聚类元素的集合
     * @param nClusters 聚类数量
     */
    public static <E extends ClusterAtom<E>> ArrayList<Cluster<E>> run(@NonNull ArrayList<E> atoms, int nClusters){
        int size = atoms.size();
        Log.i("BGID", "\t聚类原子数量"+size);

        // 构建邻接矩阵
        Mat W = new Mat(size, size, CvType.CV_32F);
        float[] temp = new float[1];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < i; j++) {
                temp[0] = atoms.get(i).similarCal(atoms.get(j));
                temp[0] = (float) Math.exp(- temp[0] / 2f);
                W.put(i, j, temp);
                W.put(j, i, temp);
            }
        }
        Log.i("BGID", "\t谱聚类: 构建邻接矩阵W");

        // 构建度矩阵和D^(-1/2)
        Mat D = Mat.zeros(size, size, CvType.CV_32F);
        Mat D_half = Mat.zeros(size, size, CvType.CV_32F);
        for (int i = 0; i < size; i++) {
            float val = 0;
            for (int j = 0; j < size; j++) {
                W.get(i, j, temp);
                val += temp[0];
            }
            temp[0] = val;
            D.put(i, i, temp);
            temp[0] = (float) Math.sqrt(1/temp[0]);
            D_half.put(i, i, temp);
        }
        Log.i("BGID", "\t谱聚类: 构建度矩阵D和D^(-1/2)");

        // 计算拉普拉斯矩阵
        Mat L = new Mat();
        Core.subtract(D, W, L);
        Log.i("BGID", "\t谱聚类: 计算拉普拉斯矩阵L");

        // 计算标准化拉普拉斯矩阵 = D^(-1/2) * L * D^(-1/2)
        Mat tempMat;
        Mat resultMat;
        tempMat = D_half.matMul(L);
        resultMat = tempMat.matMul(D_half);
        Log.i("BGID", "\t谱聚类: 计算标准化拉普拉斯矩阵L");

        // 计算特征值
        Mat eigenValues = new Mat();
        Mat eigenVectors = new Mat();
        Core.eigen(resultMat, eigenValues, eigenVectors);
        Log.i("BGID", "\t谱聚类: 计算特征值");

        // 取最小的nCluster个特征值并标准化，F∈R^(nCluster×size)
        Mat F = eigenVectors.submat(size-nClusters, size, 0, size);
        for (int i = 0; i < F.rows(); i++) {
            Mat f = F.row(i);
            double norm = Core.norm(f, Core.NORM_L2);
            Core.divide(f, Scalar.all(norm), f);
        }
        Log.i("BGID", "\t谱聚类: 取最小的nCluster个特征值并标准化F");

        // 对E进行封装提供给K-means聚类
        class InnerAtom extends ClusterAtom<InnerAtom> {
            private E originAtom;

            public InnerAtom(E originAtom, Mat coor){
                super(coor);
                this.originAtom = originAtom;
            }

            @NonNull
            @Override
            public InnerAtom clone(){return new InnerAtom(originAtom, getCoordinate().clone());}

            public E getOriginAtom() {return originAtom;}
        }

        // 使用Kmeans聚类
        ArrayList<InnerAtom> n = new ArrayList<>();
        for (int i = 0; i < size; i++)
            n.add(new InnerAtom(atoms.get(i), F.col(i)));
        Log.i("BGID", "\t谱聚类: 执行Kmeans聚类");
        ArrayList<Cluster<InnerAtom>> clusters = KmeansClustering.run(n, nClusters, CONFIG.terminalTime);
        ArrayList<Cluster<E>> results = new ArrayList<>();
        for (Cluster<InnerAtom> c : clusters) {
            Cluster<E> mapCluster = new Cluster<>();
            for (InnerAtom a : c) {
                mapCluster.addAtom(a.getOriginAtom());
            }
            results.add(mapCluster);
        }
        Log.i("BGID", "\t谱聚类: 完成Kmeans");
        return results;
    }
}
