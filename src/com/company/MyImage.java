package com.company;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import java.security.PublicKey;
import java.util.ArrayList;

public class MyImage {
    public Mat srcLR = new Mat();
    public Mat src = new Mat();
    public Mat bw = new Mat();
    ArrayList<Mat> bwList = new ArrayList<>();
    VideoCapture cap;
    int cameraSrc;
    MyImage(){

    }
    MyImage(int webCamera){
        cameraSrc = webCamera;
        cap = new VideoCapture(webCamera);

    }

}
