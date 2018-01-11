package com.company;

import com.sun.javafx.geom.Vec3f;
import com.sun.javafx.geom.Vec4f;
import org.jetbrains.annotations.NotNull;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import javax.lang.model.type.ArrayType;
import java.util.*;

import static java.lang.Math.PI;
import static java.lang.Math.pow;
import static org.opencv.core.Core.FONT_HERSHEY_PLAIN;
import static org.opencv.imgproc.Imgproc.circle;
import static org.opencv.imgproc.Imgproc.minEnclosingCircle;
import static org.opencv.imgproc.Imgproc.putText;

public class HandGesture {
    HandGesture(){
        frameNumber = 0;
        nrNoFinger = 0;
        fontFace = FONT_HERSHEY_PLAIN;//temporary
    }
    public MyImage m;
//    public List<MatOfPoint> contours = new ArrayList<>(150);
//    @NotNull
//    public List<MatOfInt> hullI = new ArrayList<>(150);;
//    @NotNull
//    public List<MatOfPoint> hullP = new ArrayList<>(150);;
//    public HashMap<Integer, MatOfPoint> contours = new HashMap<>();
//    @NotNull
//    public HashMap<Integer, MatOfInt> hullI = new HashMap<>();
//    @NotNull
//    public HashMap<Integer, MatOfPoint> hullP = new HashMap<>();;
    @NotNull
    public List<MatOfPoint> contours = new ArrayList<>(150);
    @NotNull
    public HashMap<Integer, MatOfInt> hullI = new HashMap<>();
    @NotNull
    public HashMap<Integer, MatOfPoint> hullP = new HashMap<>();
//    public List<MatOfInt> hullI = new ArrayList<>(150);;
//    @NotNull
//    public List<MatOfPoint> hullP = new ArrayList<>(150);;
//    @NotNull
    public HashMap<Integer, MatOfInt4> defects = new HashMap<>();
    @NotNull
    public Vector<Point> fingerTips = new Vector<>();
    @NotNull
    public Rect rect = new Rect();
    public int cIdx;
    public int frameNumber;
    public void printGestureInfo(@NotNull Mat src){
        int fontFace = FONT_HERSHEY_PLAIN;
        Scalar fColor = new Scalar (245,200,200);
        int xpos= (int) (src.cols()/1.5);
        int ypos= (int) (src.rows()/1.6);
        float fontSize=0.7f;
        int lineChange=14;
        String info= "Figure info:";
        putText(src,info,new Point(ypos,xpos),fontFace,fontSize,fColor);
        xpos+=lineChange;
        info=new String("Number of defects: ") + new String(intToString(nrOfDefects)) ;
        putText(src,info,new Point(ypos,xpos),fontFace,fontSize  ,fColor);
        xpos+=lineChange;
        info=new String("bounding box height, width ") + new String(intToString((int) bRect_height)) + new String(" , ") + new String(intToString((int) bRect_width)) ;
        putText(src,info,new Point(ypos,xpos),fontFace,fontSize ,fColor);
        xpos+=lineChange;
        info=new String("Is hand: ") + new String(bool2string(isHand));
        putText(src,info,new Point(ypos,xpos),fontFace,fontSize  ,fColor);

    }
    public int mostFrequentFingerNumber;
    public int nrOfDefects;
    public Rect bRect = new Rect();
    public double bRect_width;
    public double bRect_height;
    public boolean isHand;
    public boolean detectIfHand(){
        analyzeContours();
        double h = bRect_height;
        double w = bRect_width;
        isHand=true;
        if(fingerTips.size() > 5 ){
            isHand=false;
        }else if(h==0 || w == 0){
            isHand=false;
        }else if(h/w > 4 || w/h >4){
            isHand=false;
        }else if(bRect.x<20){
            isHand=false;
        }
        return isHand;

    }


    public void getFingerNumber(@NotNull MyImage m){
        removeRedundantFingerTips();
        if(bRect.height > m.src.rows()/2 && nrNoFinger>12 && isHand ){
            numberColor=new Scalar(0,200,0);
            addFingerNumberToVector();
            if(frameNumber>12){
                nrNoFinger=0;
                frameNumber=0;
                computeFingerNumber();
                numbers2Display.add(mostFrequentFingerNumber);
                fingerNumbers.clear();
            }else{
                frameNumber++;
            }
        }else{
            nrNoFinger++;
            numberColor= new Scalar(200,200,200);
        }
        addNumberToImg(m);

    }
    public void eleminateDefects(MyImage m){
        int tolerance =  (int)(bRect_height/5);
        float angleTol=95;
        Vector<double[]> newDefects = new Vector<>();
        int startidx, endidx, faridx;
        for(int i = 0; i < defects.get(cIdx).rows(); i++){
            double[] d = defects.get(cIdx).get(i, 0);
            startidx= (int) d[0];
            Point ptStart = new Point(contours.get(cIdx).get(startidx, 0)[0], contours.get(cIdx).get(startidx, 0)[1]);
            endidx=(int)d[1];
            Point ptEnd = new Point(contours.get(cIdx).get(endidx, 0)[0], contours.get(cIdx).get(endidx, 0)[1]);
            faridx=(int)d[2];
            Point ptFar = new Point(contours.get(cIdx).get(faridx, 0)[0], contours.get(cIdx).get(faridx, 0)[1]);
            if(distanceP2P(ptStart, ptFar) > tolerance && distanceP2P(ptEnd, ptFar) > tolerance && getAngle(ptStart, ptFar, ptEnd  ) < angleTol ){
                if( ptEnd.y > (bRect.y + bRect.height -bRect.height/4 ) ){
                }else if( ptStart.y > (bRect.y + bRect.height -bRect.height/4 ) ){
                }else {
                    double[] vector = new double[]{ d[0], d[1],  d[2], d[3]};
                    newDefects.add(vector);
                }
            }
        }

//        for(Vec4f d : defects.get(cIdx)) {
//            Vec4f v = new Vec4f(d);
//            startidx=(int)v.w;Point ptStart = contours.get(cIdx).get(startidx);
//            endidx=(int)v.x; Point ptEnd = contours.get(cIdx).get(endidx);
//            faridx=(int)v.y; Point ptFar = contours.get(cIdx).get(endidx);
//            if(distanceP2P(ptStart, ptFar) > tolerance && distanceP2P(ptEnd, ptFar) > tolerance && getAngle(ptStart, ptFar, ptEnd  ) < angleTol ){
//                if( ptEnd.y > (bRect.y + bRect.height -bRect.height/4 ) ){
//                }else if( ptStart.y > (bRect.y + bRect.height -bRect.height/4 ) ){
//                }else {
//                    newDefects.add(v);
//                }
//            }
//        }
        nrOfDefects=newDefects.size();
        MatOfInt4 temp = new MatOfInt4();
        for(int i = 0; i < newDefects.size(); i++){
            temp.put(i, 0, newDefects.get(i));
        }
        System.out.println(temp);
        defects.put(cIdx, temp);
        removeRedundantEndPoints(newDefects, m);

    }
    public void getFingerTips(@NotNull MyImage m){
        fingerTips.clear();
        int j=0;
        int startidx, endidx, faridx;
        if(defects.get(cIdx) == null)
            return;
        for(int i = 0; i < defects.get(cIdx).rows(); i++){
            double[] d = defects.get(cIdx).get(i, 0);
            startidx= (int) d[0];
            Point ptStart = new Point(contours.get(cIdx).get(startidx, 0)[0], contours.get(cIdx).get(startidx, 0)[1]);
            endidx=(int)d[1];
            Point ptEnd = new Point(contours.get(cIdx).get(endidx, 0)[0], contours.get(cIdx).get(endidx, 0)[1]);
            faridx=(int)d[2];
            Point ptFar = new Point(contours.get(cIdx).get(faridx, 0)[0], contours.get(cIdx).get(faridx, 0)[1]);
            if(i == 0) {
                fingerTips.add(ptStart);
                j++;
            }
            fingerTips.add(ptEnd);
            j++;
        }



        if(fingerTips.size()==0){
            checkForOneFinger(m);
        }

    }
    public void drawFingerTips(@NotNull MyImage m){
        Point p;
        int k=0;
        for(int i=0;i<fingerTips.size();i++){
            p=fingerTips.get(i);
            putText(m.src,intToString(i),new Point(p.x -  0,p.y - 30),fontFace, 1.2f,new Scalar(200,200,200),2);
            Imgproc.circle( m.src,p,   5, new Scalar(100,255,100), 4 );
        }

    }

    private String bool2string(boolean tf){
        return String.valueOf(tf);

    }
    private int fontFace;
    private int prevNrFingerTips;
    private void checkForOneFinger(MyImage m){
        int yTol = bRect.height/6;
        Point highestP = new Point();
        highestP.y=m.src.rows();

        for(int i = 0; i < contours.get(cIdx).rows(); i++){
            Point v = new Point(contours.get(cIdx).get(i, 0)[0], contours.get(cIdx).get(i, 0)[1]);
            if(v.y < highestP.y) {
                highestP = v;
                System.out.println(highestP.y);
            }
        }

        int n=0;
        for(int i = 0; i < hullP.get(cIdx).rows(); i++){
            Point v = new Point(hullP.get(cIdx).get(i, 0)[0], hullP.get(cIdx).get(i, 0)[1]);
            System.out.println("x " + v.x + " y " + v.y + " highestpY " + highestP.y + "ytol " +  yTol);
            if(v.y < highestP.y + yTol && v.y != highestP.y && v.x != highestP.x) {
                n++;
            }
        }

      if(n == 0) {
          fingerTips.add(highestP);
      }


    }
    private float getAngle(@NotNull Point s, @NotNull Point f, @NotNull Point e){
        float l1 = distanceP2P(f,s);
        float l2 = distanceP2P(f,e);
        float dot= (float)((s.x-f.x)*(e.x-f.x) + (s.y-f.y)*(e.y-f.y));
        float angle = (float)(Math.acos(dot/(l1*l2)));
        angle= (float)(angle*180/PI);
        return angle;

    }
    private Vector<Integer> fingerNumbers = new Vector<>();
    private void analyzeContours(){
        bRect_width = bRect.width;
        bRect_height = bRect.height;

    }
    private String intToString(int number){
        return  String.valueOf(number);

    }
    private void computeFingerNumber(){
        fingerNumbers.sort(new Comparator<Integer>() {
            @Override
            public int compare(@NotNull Integer lhs, @NotNull Integer rhs) {
                return rhs.compareTo(lhs);
            }
        });
        int frequentNr;
        int thisNumberFreq=1;
        int highestFreq=1;
        frequentNr=fingerNumbers.get(0);
        for(int i=1;i<fingerNumbers.size(); i++){
            if(fingerNumbers.get(i-1)!= fingerNumbers.get(i)){
                if(thisNumberFreq>highestFreq){
                    frequentNr=fingerNumbers.get(i-1);
                    highestFreq=thisNumberFreq;
                }
                thisNumberFreq=0;
            }
            thisNumberFreq++;
        }
        if(thisNumberFreq>highestFreq){
            frequentNr=fingerNumbers.get(fingerNumbers.size()-1);
        }
        mostFrequentFingerNumber=frequentNr;
    }

    private void drawNewNumber(MyImage m){

    }
    private void addNumberToImg(@NotNull MyImage m){
        int xPos=10;
        int yPos=10;
        int offset=30;
        float fontSize=1.5f;
        int fontFace =1;
        for(int i=0;i<numbers2Display.size();i++){
            Imgproc.rectangle(m.src,new Point(xPos,yPos),new Point(xPos+offset,yPos+offset),numberColor, 2);
            putText(m.src, intToString(numbers2Display.get(i)),new Point(xPos+7,yPos+offset-3),fontFace,fontSize,numberColor);
            xPos+=40;
            if(xPos>(m.src.cols()-m.src.cols()/3.2)){
                yPos+=40;
                xPos=10;
            }
        }

    }
    private Vector<Integer> numbers2Display = new Vector<>();
    private void addFingerNumberToVector(){
        int i=fingerTips.size();
        fingerNumbers.add(i);

    }
    private Scalar numberColor;
    private int nrNoFinger;
    private float distanceP2P(Point a,Point b){
        float d = (float)(Math.sqrt(Math.abs(pow(a.x - b.x, 2) + pow(a.y-b.y,2))));
        return d;

    }
    private void removeRedundantEndPoints(Vector<double[]> newDefects, MyImage m){
        float avgX, avgY;
        float tolerance = (float)bRect_width/6;
        int startidx, endidx, faridx;
        int startidx2, endidx2;
        for(int i=0;i<newDefects.size();i++){
            for(int j=i;j<newDefects.size();j++){
                startidx = (int)newDefects.get(i)[0];  Point ptStart = new Point(contours.get(cIdx).get(startidx, 0)[0], contours.get(cIdx).get(startidx, 0)[1]);
                endidx = (int)newDefects.get(i)[1]; Point ptEnd = new Point(contours.get(cIdx).get(endidx, 0)[0], contours.get(cIdx).get(endidx, 0)[1]);
                startidx2 = (int)newDefects.get(j)[0];  Point ptStart2 = new Point(contours.get(cIdx).get(startidx2, 0)[0], contours.get(cIdx).get(startidx2, 0)[1]);
                endidx2 = (int)newDefects.get(j)[1];  Point ptEnd2 = new Point(contours.get(cIdx).get(endidx2, 0)[0], contours.get(cIdx).get(endidx2, 0)[1]);

                if(distanceP2P(ptStart,ptEnd2) < tolerance ){
                    double[] vector = new double[]{ptEnd2.x, ptEnd2.y};
                    contours.get(cIdx).put(startidx, 0, vector);
                    break;
                }if(distanceP2P(ptEnd,ptStart2) < tolerance ){
                    double[] vector = new double[]{ptEnd.x, ptEnd.y};
                    contours.get(cIdx).put(startidx, 0, vector);
                }
            }
        }


    }
    private void removeRedundantFingerTips(){
        Vector<Point> newFingers = new Vector<>();
        for(int i=0;i<fingerTips.size();i++){
            for(int j=i;j<fingerTips.size();j++){
                if(distanceP2P(fingerTips.get(i),fingerTips.get(j)) < 10 && i!=j){
                }else{
                    newFingers.add(fingerTips.get(i));
                    break;
                }
            }
        }
        fingerTips = newFingers;

    }
}
