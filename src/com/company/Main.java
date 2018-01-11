package com.company;

import com.atul.JavaOpenCV.Imshow;
import org.jetbrains.annotations.NotNull;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.KalmanFilter;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import static org.opencv.core.Core.FONT_HERSHEY_PLAIN;
import static org.opencv.core.Core.meanStdDev;
import static org.opencv.imgproc.Imgproc.*;

public class Main {
    private final int ORIGCOL2COL = Imgproc.COLOR_BGR2HLS;
    private final int COL2ORIGCOL = Imgproc.COLOR_HLS2BGR;
    private final int NSAMPLES = 7;
    public int fontFace = FONT_HERSHEY_PLAIN;
    public int square_len;
    @NotNull
    public int avgColor[][] = new int[NSAMPLES][3] ;
    @NotNull
    public int c_lower[][] = new int[NSAMPLES][3];
    @NotNull
    public int c_upper[][] = new int[NSAMPLES][3];
    @NotNull
    public int avgBGR[] = new int[3];
    public int nefects;
    public int iSinceKFInit;
    @NotNull
    public Point boundingDim = new Point();
    @NotNull
    public VideoWriter out = new VideoWriter();
    @NotNull
    public Mat edges = new Mat();
    public My_ROI roi1, roi2,roi3,roi4,roi5,roi6;
    @NotNull
    public Vector<My_ROI> roi = new Vector<>();
    @NotNull
    public Vector <KalmanFilter> kf = new Vector<>();
    @NotNull
    public Vector <MatOfFloat> measurement = new Vector<>();

    public void init(MyImage m){
        square_len=20;
        iSinceKFInit=0;
    }
    void col2origCol(int hsv[], int bgr[], Mat src){
        //Không thấy gọi làm nên bỏ qua
//        Mat avgBGRMat=src.clone();
//        for(int i=0;i<3;i++){
//            avgBGRMat.put();
//            avgBGRMat.data[i]=hsv[i];
//        }
//        cvtColor(avgBGRMat,avgBGRMat,COL2ORIGCOL);
//        for(int i=0;i<3;i++){
//            bgr[i]=avgBGRMat.data[i];
//        }
    }
    void printText(@NotNull Mat src, String text){
        int fontFace = FONT_HERSHEY_PLAIN;
        putText(src,text,new Point(src.cols()/2, src.rows()/10),fontFace, 1.2f,new Scalar(200,0,0),2);
    }

    void waitForPalmCover(@NotNull MyImage m){
        //Lấy frame tiếp theo
        m.cap.retrieve(m.src);
    
        Core.flip(m.src,m.src,1);
        roi.add(new My_ROI(new Point(m.src.cols()/3, m.src.rows()/6),new Point(m.src.cols()/3+square_len,m.src.rows()/6+square_len),m.src));
        roi.add(new My_ROI(new Point(m.src.cols()/4, m.src.rows()/2),new Point(m.src.cols()/4+square_len,m.src.rows()/2+square_len),m.src));
        roi.add(new My_ROI(new Point(m.src.cols()/3, m.src.rows()/1.5),new Point(m.src.cols()/3+square_len,m.src.rows()/1.5+square_len),m.src));
        roi.add(new My_ROI(new Point(m.src.cols()/2, m.src.rows()/2),new Point(m.src.cols()/2+square_len,m.src.rows()/2+square_len),m.src));
        roi.add(new My_ROI(new Point(m.src.cols()/2.5, m.src.rows()/2.5),new Point(m.src.cols()/2.5+square_len,m.src.rows()/2.5+square_len),m.src));
        roi.add(new My_ROI(new Point(m.src.cols()/2, m.src.rows()/1.5),new Point(m.src.cols()/2+square_len,m.src.rows()/1.5+square_len),m.src));
        roi.add(new My_ROI(new Point(m.src.cols()/2.5, m.src.rows()/1.8),new Point(m.src.cols()/2.5+square_len,m.src.rows()/1.8+square_len),m.src));


        for(int i =0;i<50;i++){
            m.cap.retrieve(m.src);
            Core.flip(m.src,m.src,1);
            for(int j=0;j<NSAMPLES;j++){
                roi.get(j).draw_recangle(m.src);
            }
            String imgText=new String("Cover rectangles with palm");
            printText(m.src,imgText);

            if(i==30){
                //	imwrite("./images/waitforpalm1.jpg",m->src);
            }

            displayImage(Mat2BufferedImage(m.src));
            out.write(m.src);
            //out << m->src;
            if (waitKey(30) >= 0) break;
            //if(Cv::waitKey(30) >= 0) break;
        }
    }

    int getMedian(@NotNull Vector<Integer> val){
        int median;
        int size = val.size();
        val.sort((a,b)-> a.compareTo(b));
        if (size  % 2 == 0)  {
            median = val.get(size / 2 - 1);
        } else{
            median = val.get(size / 2);
        }
        return median;
    }

    void getAvgColor(MyImage m, @NotNull My_ROI roi, int avg[]){
        Mat r = new Mat();
        roi.rot_ptr.copyTo(r);
        Vector<Integer>hm = new Vector<>();
        Vector<Integer>sm = new Vector<>();
        Vector<Integer>lm = new Vector<>();
        // generate vectors
        for(int i=2; i<r.rows()-2; i++){
            for(int j=2; j<r.cols()-2; j++){
                  hm.add((int) r.get(i, j)[0]);
                  sm.add((int) r.get(i, j)[1]);
                  lm.add((int) r.get(i, j)[2]);

            }
        }
        avg[0]=getMedian(hm);
        avg[1]=getMedian(sm);
        avg[2]=getMedian(lm);
    }
    void average(@NotNull MyImage m){
        m.cap.retrieve(m.src);
        Core.flip(m.src, m.src, 1);
        for(int i=0;i<30;i++){
            m.cap.retrieve(m.src);
            Core.flip(m.src, m.src, 1);
            cvtColor(m.src, m.src, ORIGCOL2COL);
            for(int j=0;j<NSAMPLES;j++){
                getAvgColor(m,roi.get(j),avgColor[j]);
                roi.get(j).draw_recangle(m.src);
            }
            cvtColor(m.src,m.src,COL2ORIGCOL);
            String imgText= new String("Finding average color of hand");
            printText(m.src,imgText);
            displayImage(Mat2BufferedImage(m.src));
            if(waitKey(30) >= 0) break;
        }
    }

    void initTrackbars(){
        for(int i=0;i<NSAMPLES;i++){
            c_lower[i][0]=12;
            c_upper[i][0]=7;
            c_lower[i][1]=30;
            c_upper[i][1]=40;
            c_lower[i][2]=80;
            c_upper[i][2]=80;
        }

//        createTrackbar("lower1","trackbars",&c_lower[0][0],255);
//        createTrackbar("lower2","trackbars",&c_lower[0][1],255);
//        createTrackbar("lower3","trackbars",&c_lower[0][2],255);
//        createTrackbar("upper1","trackbars",&c_upper[0][0],255);
//        createTrackbar("upper2","trackbars",&c_upper[0][1],255);
//        createTrackbar("upper3","trackbars",&c_upper[0][2],255);
    }

    void normalizeColors(MyImage  myImage){
        // copy all boundries read from trackbar
        // to all of the different boundries
        for(int i=1;i<NSAMPLES;i++){
            for(int j=0;j<3;j++){
                c_lower[i][j]=c_lower[0][j];
                c_upper[i][j]=c_upper[0][j];
            }
        }
        // normalize all boundries so that
        // threshold is whithin 0-255
        for(int i=0;i<NSAMPLES;i++){
            if((avgColor[i][0]-c_lower[i][0]) <0){
                c_lower[i][0] = avgColor[i][0] ;
            }if((avgColor[i][1]-c_lower[i][1]) <0){
                c_lower[i][1] = avgColor[i][1] ;
            }if((avgColor[i][2]-c_lower[i][2]) <0){
                c_lower[i][2] = avgColor[i][2] ;
            }if((avgColor[i][0]+c_upper[i][0]) >255){
                c_upper[i][0] = 255-avgColor[i][0] ;
            }if((avgColor[i][1]+c_upper[i][1]) >255){
                c_upper[i][1] = 255-avgColor[i][1] ;
            }if((avgColor[i][2]+c_upper[i][2]) >255){
                c_upper[i][2] = 255-avgColor[i][2] ;
            }
        }
    }

    void produceBinaries(@NotNull MyImage m){
        Scalar lowerBound;
        Scalar upperBound;
        Mat foo;
        for(int i=0;i<NSAMPLES;i++){
            normalizeColors(m);
            lowerBound=new Scalar( avgColor[i][0] - c_lower[i][0] , avgColor[i][1] - c_lower[i][1], avgColor[i][2] - c_lower[i][2] );
            upperBound=new Scalar( avgColor[i][0] + c_upper[i][0] , avgColor[i][1] + c_upper[i][1], avgColor[i][2] + c_upper[i][2] );
            m.bwList.add(new Mat(m.srcLR.rows(),m.srcLR.cols(),CvType.CV_8U));
            Core.inRange(m.srcLR,lowerBound,upperBound,m.bwList.get(i));
        }
        m.bwList.get(0).copyTo(m.bw);
        for(int i=1;i<NSAMPLES;i++){
            Core.add(m.bw, m.bwList.get(i), m.bw);
        }
        Imgproc.medianBlur(m.bw, m.bw,7);
    }

    void showWindows(@NotNull MyImage m){
        Imgproc.pyrDown(m.bw,m.bw);
        Imgproc.pyrDown(m.bw,m.bw);
        Rect rot = new Rect(new Point(3*m.src.cols() / 4,0), m.bw.size());

        Vector<Mat> channels = new Vector<>();
        Mat result = new Mat();
        for(int i=0;i<3;i++)
            channels.add(m.bw);
        Core.merge(channels, result);
        Mat nresult = new Mat();
        result.copyTo(nresult);

        result.copyTo( m.src.submat(rot));
        displayImage(Mat2BufferedImage(m.src));

    }


    int findBiggestContour(@NotNull List<MatOfPoint> contours){
        int indexOfBiggestContour = -1;
        int sizeOfBiggestContour = 0;
        for (int i = 0; i < contours.size(); i++){
            if(contours.get(i).rows() > sizeOfBiggestContour){
                sizeOfBiggestContour = contours.get(i).rows();
                indexOfBiggestContour = i;
            }
        }
        return indexOfBiggestContour;
    }

    void myDrawContours(@NotNull MyImage m, @NotNull HandGesture hg){
        List<MatOfPoint> hullP = new ArrayList<>();
        for(int i = 0; i <= hg.cIdx; i++){
            if(hg.hullP.containsKey(i))
            {
                hullP.add(hg.hullP.get(i));
            } else {
                hullP.add(new MatOfPoint());
            }

        }
        Imgproc.drawContours(m.src,hullP,hg.cIdx,new Scalar(200,0,0),2, 8, new MatOfInt4(), 0, new Point());
        Imgproc.rectangle(m.src,hg.bRect.tl(),hg.bRect.br(),new Scalar(0,0,200));

        int fontFace = FONT_HERSHEY_PLAIN;


        ArrayList<Mat> channels = new ArrayList<>();
        Mat result = new Mat();
        for(int i=0;i<3;i++)
            channels.add(m.bw);
        Core.merge(channels, result);

        //	drawContours(result,hg->contours,hg->cIdx,cv::Scalar(0,200,0),6, 8, vector<Vec4i>(), 0, Point());
        List<MatOfPoint> hullP2 = new ArrayList<>();
        for(int i = 0; i <= hg.cIdx; i++){
            if(hg.hullP.containsKey(i))
            {
                hullP2.add(hg.hullP.get(i));
            } else {
                hullP2.add(new MatOfPoint());
            }

        }
        Imgproc.drawContours(result, hullP2,hg.cIdx,new Scalar(0,0,250),10, 8, new MatOfInt4(), 0, new Point());

        int startidx, endidx, faridx;
        for(int i = 0; i < hg.defects.get(hg.cIdx).rows(); i++){
            double[] d = hg.defects.get(hg.cIdx).get(i, 0);
            startidx= (int) d[0];
            Point ptStart = new Point(hg.contours.get(hg.cIdx).get(startidx, 0)[0], hg.contours.get(hg.cIdx).get(startidx, 0)[1]);
            endidx=(int)d[1];
            Point ptEnd = new Point(hg.contours.get(hg.cIdx).get(endidx, 0)[0], hg.contours.get(hg.cIdx).get(endidx, 0)[1]);
            faridx=(int)d[2];
            Point ptFar = new Point(hg.contours.get(hg.cIdx).get(faridx, 0)[0], hg.contours.get(hg.cIdx).get(faridx, 0)[1]);
            float depth = (float) (d[3] / 256);
            Imgproc.circle( result, ptFar,   9, new Scalar(0,205,0), 5 );
        }

//	imwrite("./images/contour_defects_before_eliminate.jpg",result);

    }

    void makeContours(@NotNull MyImage m, @NotNull HandGesture hg){
        Mat aBw = new Mat();
        Mat hieratic = new Mat();
        MatOfInt hull = new MatOfInt();
        Imgproc.pyrUp(m.bw, m.bw);
        m.bw.copyTo(aBw);

        Imgproc.findContours(aBw, hg.contours, hieratic, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE );
        hg.cIdx = findBiggestContour(hg.contours);
        hg.hullI.clear();
        hg.hullP.clear();


        if(hg.cIdx!=-1) {
            if(!hg.hullP.containsKey(hg.cIdx)){
                hg.hullP.put(hg.cIdx, new MatOfPoint());
                hg.hullI.put(hg.cIdx, new MatOfInt());
                hg.defects.put(hg.cIdx, new MatOfInt4());
            }
            hg.bRect = Imgproc.boundingRect(hg.contours.get(hg.cIdx));
            Imgproc.convexHull(hg.contours.get(hg.cIdx), hull, false);
            hg.hullP.put(hg.cIdx, convertIndexesToPoints(hg.contours.get(hg.cIdx), hull));
            convexHull(hg.contours.get(hg.cIdx), hg.hullI.get(hg.cIdx), false);
            MatOfPoint2f hg2f = new MatOfPoint2f(hg.hullP.get(hg.cIdx).toArray());
            approxPolyDP(hg2f, hg2f, 18, true);
            MatOfPoint hg2i = new MatOfPoint(hg2f.toArray());
            hg.hullP.put(hg.cIdx, hg2i);
            if(hg.contours.get(hg.cIdx).rows() > 3){
                convexityDefects(hg.contours.get(hg.cIdx), hg.hullI.get(hg.cIdx), hg.defects.get(hg.cIdx));
                hg.eleminateDefects(m);
            }
            boolean isHand = hg.detectIfHand();
            hg.printGestureInfo(m.src);
            if (isHand) {
                hg.getFingerTips(m);
                hg.drawFingerTips(m);
                myDrawContours(m, hg);
            }
        }
    }













    // Compulsory
    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public void run(String[] args) {
        MyImage m = new MyImage(0);
        HandGesture hg = new HandGesture();
        init(m);
        m.cap.retrieve(m.src);
        out.open("out.avi", VideoWriter.fourcc('M', 'J', 'P', 'G'), 15, m.src.size(), true);
        waitForPalmCover(m);
        average(m);
        initTrackbars();
        for(;;){
            hg.frameNumber++;
            m.cap.retrieve(m.src);
            Core.flip(m.src,m.src,1);
            pyrDown(m.src,m.srcLR);
            blur(m.srcLR,m.srcLR,new Size(3,3));
            cvtColor(m.srcLR,m.srcLR,ORIGCOL2COL);
            produceBinaries(m);
            cvtColor(m.srcLR,m.srcLR,COL2ORIGCOL);
            makeContours(m, hg);
            hg.getFingerNumber(m);
            showWindows(m);
            out.write(m.src);

            //imwrite("./images/final_result.jpg",m.src);
            if(waitKey(30) >= 30) break;
        }
        out.release();
        m.cap.release();
    }
    @NotNull
    public static BufferedImage Mat2BufferedImage(Mat m){

        int type = BufferedImage.TYPE_BYTE_GRAY;
        if ( m.channels() > 1 ) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        int bufferSize = m.channels()*m.cols()*m.rows();
        byte [] b = new byte[bufferSize];
        m.get(0,0,b); // get all the pixels
        BufferedImage image = new BufferedImage(m.cols(),m.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(b, 0, targetPixels, 0, b.length);
        return image;

    }
    @NotNull
    private static JFrame jframe = new JFrame();
    public static void displayImage(@NotNull Image img2)
    {
        //BufferedImage img=ImageIO.read(new File("/HelloOpenCV/lena.png"));
        ImageIcon icon=new ImageIcon(img2);

        jframe.setLayout(new FlowLayout());
        jframe.setSize(img2.getWidth(null)+50, img2.getHeight(null)+50);
        JLabel lbl=new JLabel();
        lbl.setIcon(icon);
        jframe.add(lbl);
        jframe.setVisible(true);
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jframe.invalidate();
        jframe.remove(lbl);

    }
    //waitkey
    @NotNull
    private static int keyUserdata[] = new int[2];      // [0] = id [1] = keycode

    private static boolean isWaitKey = false;
    public static int waitKey(int delay) {
        isWaitKey = true;
        int timeout = delay;
        try {
            synchronized (keyUserdata) {
                while( isWaitKey && (delay == 0 || timeout > 0)  ) {
                    if( delay > 0 ) {
                        int seconds = timeout / 1000;
                        timeout -= 50;
                        if( timeout < 0 ) {
                            return -1;
                        }
                    }
                    keyUserdata.wait(50);
                }
            }
            return keyUserdata[1];
        } catch(Exception e) {
        }
        return -1;
    }

    static {
        KeyEventDispatcher dispatcher = new KeyEventDispatcher() {
            public boolean dispatchKeyEvent(KeyEvent e) {
                synchronized (keyUserdata) {
                    if( e.getID() == KeyEvent.KEY_PRESSED ) {
                        keyUserdata[0] = e.getID();
                        keyUserdata[1] = e.getKeyCode();
                        isWaitKey = false;
                        keyUserdata.notify();
                    }
                }
                return false;
            }
        };

        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(dispatcher);
    }

    //MatOfPoint to max of hull
    @NotNull
    public static MatOfPoint convertIndexesToPoints(MatOfPoint contour, MatOfInt indexes) {
        int[] arrIndex = indexes.toArray();
        Point[] arrContour = contour.toArray();
        Point[] arrPoints = new Point[arrIndex.length];

        for (int i=0;i<arrIndex.length;i++) {
            arrPoints[i] = arrContour[arrIndex[i]];
        }

        MatOfPoint hull = new MatOfPoint();
        hull.fromArray(arrPoints);
        return hull;
    }

    @NotNull
    public static MatOfPoint2f convertTo2f(MatOfPoint mat){
        MatOfPoint2f result = new MatOfPoint2f();
        for(int xx = 0; xx < mat.rows(); xx++){
            for(int yy =0; yy < mat.cols(); yy++){
                int[] data = new int[2];
                mat.get(xx,yy,data);
                double[] data2 = new double[]{(float)(data[0]), (float)(data[1])};
                System.out.println(data2);
                result.put(xx, yy, data2);
            }
        }
        System.out.println(result);
        return result;
    }

    @NotNull
    public static MatOfPoint convertTo2(MatOfPoint2f mat){
        MatOfPoint result = new MatOfPoint();
        for(int xx = 0; xx < mat.rows(); xx++){
            for(int yy =0; yy < mat.cols(); yy++){
                float[] data = new float[2];
                mat.get(xx,yy,data);

                int[] data2 = new int[]{(int) data[0], (int) data[1]};
                result.put(xx, yy, data2);
            }
        }
        return result;
    }


//    public static void main(String[] args) {
//
//        System.out.println("Welcome to OpenCV " + Core.VERSION);
//        Mat m = new Mat();
//        VideoCapture cap = new VideoCapture(0);
//
//        cap.retrieve(m);
//
//
//        Mat gray = new Mat();
//        Imgproc.cvtColor(m, gray, Imgproc.COLOR_RGBA2GRAY);
//
//        Imgproc.Canny(gray, gray, 50, 200);
//        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
//        Mat hierarchy = new Mat();
//// find contours:
//        Imgproc.findContours(gray, contours, hierarchy, Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);
//        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
//            Imgproc.drawContours(m, contours, contourIdx, new Scalar(0, 0, 255), -1);
//        }
//        displayImage(Mat2BufferedImage(m));
//        System.out.println(contours.get(0));
//        System.out.println("OpenCV Mat data:\n" + contours.get(0).get(0,1));
//
//
//        //System.out.println("OpenCV Mat data:\n" + m.dump());
//    }


}