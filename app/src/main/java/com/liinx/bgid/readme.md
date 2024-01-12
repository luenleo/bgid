# 概述

本项目是[IMU-Assisted Online Video Background Identification](https://ieeexplore.ieee.org/document/9802831)的复现

# 运行

1. 相机标定：使用`MATLAB`标定相机，将得到的相机内参矩阵写入`CONFIG.java`（论文里面的Urban是骗人的）

2. 导入OPENCV：详见百度或[总结](http://10.108.8.190:65433/Android%20Studio%20Giraffe%E5%AF%BC%E5%85%A5OpenCV.md)

3. AS运行项目

# 项目结构

- `MainActivity`主页面和注册相机、IMU监听
- `FrameProcess`使用实时Harris检测和KTL光流处理图像
- `ImuLinster`实时更新IMU数据，进行位姿检测
- `cluster`聚类集合，包含Kmeans和谱聚类
- `core`为算法核心，离线使用
- `dataLoader`加载离线的数据并进行同步
- `utils`为工具类集合