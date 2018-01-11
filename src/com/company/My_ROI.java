package com.company;


import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class My_ROI {
    public My_ROI(){
        upper_corner = new Point(0,0);
        lower_corner = new Point(0,0);

    }
    public My_ROI(Point u_corner, Point l_cornet, Mat src){
        upper_corner = u_corner;
        lower_corner = l_cornet;
        color = new Scalar(0, 255,0);
        border_thickness = 2;
        Rect myRect = new Rect((int)u_corner.x, (int)u_corner.y, (int)(l_cornet.x - u_corner.x), (int)(l_cornet.y - u_corner.y));
        rot_ptr = src.submat(myRect);
    }
    public Point upper_corner = new Point();
    public Point lower_corner = new Point();
    public Mat rot_ptr = new Mat();
    public Scalar color;
    public int border_thickness;
    public void draw_recangle(Mat src){
        Imgproc.rectangle(src, upper_corner, lower_corner, color, border_thickness);
    }

}
