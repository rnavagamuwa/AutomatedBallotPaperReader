/**
 * @author MaN
 * on 11/9/2016.
 */

import org.opencv.core.*;
import org.opencv.imgproc.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgcodecs.Imgcodecs.imwrite;
import static org.opencv.imgproc.Imgproc.moments;
import static org.opencv.imgproc.Imgproc.rectangle;

public class FeatureExtractor {


    List<PartyFeature> partyList = new ArrayList<>();
    Mat mHierarchy = new Mat();
    Mat im;

    public FeatureExtractor(String dirPath) {
        try {
          //  getPartyMoments("resources/images");
        } catch (Exception e) {

        }
        try {
            getPartyMoments(dirPath);
        } catch (Exception e) {

        }

    }

    public void createMoments() {
        Mat gray = new Mat(im.rows(), im.cols(), CvType.CV_8SC1);
        List<MatOfPoint> contours = new ArrayList<>();

        Imgproc.cvtColor(im, gray, Imgproc.COLOR_RGB2GRAY);
        Imgproc.Canny(gray, gray, 50, 50);
        Imgproc.findContours(gray, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        Moments moments = Imgproc.moments(gray);
        double m01 = moments.get_m01();
        double m10 = moments.get_m10();
        double area = moments.get_m00();
        int m_x = (int) (m10 / area);
        int m_y = (int) (m01 / area);
        Point p1 = new Point();
        p1.x = m_x;
        p1.y = m_y;
        Imgproc.line(im, p1, p1, new Scalar(0, 0, 255), 3);
        Mat hu = new Mat(1, 7, CvType.CV_8UC1);
        Imgproc.HuMoments(moments, hu);
        for (int i = 0; i < 7; i++) {
            // System.out.println(hu.get(i,0)[0]);
        }
        for (MatOfPoint p : contours) {
            Point[] points = p.toArray();
            for (int i = 0; i < points.length - 1; i++) {
                Imgproc.line(im, points[i], points[i + 1], new Scalar(0, 0, 255), 1);

            }
        }
       // imwrite("E:/croppedGray.jpg", gray);
        //imwrite("E:/croppedFinal.jpg", im);


    }

    public String getFeature(Mat img, VotedSquares featureSquare) {
        Mat im = img.submat(featureSquare.getStartRow() + 10, featureSquare.getEndRow(), featureSquare.getStartCol() + 5, featureSquare.getEndCol());
        Mat gray = new Mat(im.rows(), im.cols(), CvType.CV_8SC1);

        Imgproc.cvtColor(im, gray, Imgproc.COLOR_RGB2GRAY);
        Imgproc.Canny(gray, gray, 50, 50);

        //imwrite("E:/feature.jpg", gray);
        Moments moments = Imgproc.moments(gray);
        double area = moments.get_m00();
        Mat hu = new Mat(1, 7, CvType.CV_8UC1);
        Imgproc.HuMoments(moments, hu);
        int max = 0;
        int count = 0;
        String name = "";
        for (PartyFeature pf : partyList) {
            for (int i = 0; i < 7; i++) {
                if ((pf.getHu().get(i, 0)[0] - pf.getHu().get(i, 0)[0] * 0.2) < hu.get(i, 0)[0] && pf.getHu().get(i, 0)[0] + (pf.getHu().get(i, 0)[0] * 0.2) > hu.get(i, 0)[0]) {
                    count++;
                }
            }
            if (max < count) {
                max = count;
            }
            if (pf.getArea() - 10000 < area && pf.getArea() + 10000 > area && count == max) {
                name = pf.getName();
            }
            count = 0;
        }
        return name;
    }

    public void getPartyMoments(String path) throws Exception {
        File[] files = new File(path).listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
            } else {
                PartyFeature partyFeature = new PartyFeature();
                partyFeature.setName(file.getName());

                Mat im = imread(path + "/" + partyFeature.getName());
                Mat gray = new Mat(im.rows(), im.cols(), CvType.CV_8SC1);

                Imgproc.cvtColor(im, gray, Imgproc.COLOR_RGB2GRAY);
                Imgproc.Canny(gray, gray, 50, 50);
                Moments moments = Imgproc.moments(gray);
                Mat hu = new Mat(1, 7, CvType.CV_8UC1);
                Imgproc.HuMoments(moments, hu);

                partyFeature.setHu(hu);
                partyList.add(partyFeature);
                //additional work
                double m01 = moments.get_m01();
                double m10 = moments.get_m10();
                double area = moments.get_m00();
                partyFeature.setArea(area);
                int m_x = (int) (m10 / area);
                int m_y = (int) (m01 / area);
                Point p1 = new Point();
                p1.x = m_x;
                p1.y = m_y;
                Imgproc.line(im, p1, p1, new Scalar(0, 0, 255), 3);

              //  imwrite("E:/" + partyFeature.getName(), im);
                System.out.println("File: " + file.getName());
            }
        }
    }
}

class PartyFeature {
    Mat hu;
    String name;
    double area;

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public Mat getHu() {
        return hu;
    }

    public void setHu(Mat hu) {
        this.hu = hu;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
