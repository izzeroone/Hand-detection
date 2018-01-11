package com.company;

import org.jetbrains.annotations.NotNull;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import java.security.PublicKey;
import java.util.ArrayList;

public class MyImage {
    @NotNull
    public Mat srcLR = new Mat();
    @NotNull
    public Mat src = new Mat();
    @NotNull
    public Mat bw = new Mat();
    @NotNull ArrayList<Mat> bwList = new ArrayList<>();
    VideoCapture cap;
    int cameraSrc;
    MyImage(){

    }
    MyImage(int webCamera){
        cameraSrc = webCamera;
        cap = new VideoCapture(webCamera);

    }

}
