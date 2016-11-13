/**
 * @author MaN
 * on 11/7/2016.
 */

import org.opencv.core.*;
import org.opencv.imgproc.*;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgcodecs.Imgcodecs.imwrite;

public class Test {
    // Compulsory
    BufferedImage outputImg;
    int mainVote = -1;
    List<Integer> prefVotes;
    int featureStartCol=0;
    int featureEndCol=0;
    String partySymbol="";
    String partySymbolDir="";
    boolean isSuccess;

    static {
        try {

            System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // used for tests. This library in classpath only
        } catch (UnsatisfiedLinkError e) {
            try {
                NativeUtils.loadLibraryFromJar(File.separator + System.mapLibraryName(Core.NATIVE_LIBRARY_NAME)); // during runtime. .DLL within .JAR
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            }
        }
    }

    public Test(String path,String symbolDirPath) {
        partySymbolDir=symbolDirPath;
        imageProcess(path);
    }

    public String getPartySymbol() {
        return partySymbol;
    }

    public void imageProcess(String path) {
        FeatureExtractor fe=new FeatureExtractor(partySymbolDir);
        int offset=30;
        List<CountByRow> countByRow = new ArrayList<CountByRow>();
        List<CountByRow> filteredRows = new ArrayList<CountByRow>();
        List<CountByCol> countByCol = new ArrayList<>();
        List<CountByCol> filteredCols = new ArrayList<>();

        Mat im = imread(path);
        Mat originalImg = im.clone();
        Mat gray = new Mat(im.rows(), im.cols(), CvType.CV_8SC1);
        Mat finalImg = im.clone();

        Imgproc.cvtColor(im, gray, Imgproc.COLOR_RGB2GRAY);
        Imgproc.Canny(gray, gray, 80, 100);

        Mat lines = new Mat();
        Imgproc.HoughLinesP(gray, lines, 1, Math.PI / 180, 25, 50, 3);

        double[] data;
        Point pt1 = new Point();
        Point pt2 = new Point();
        for (int j = 0; j < lines.rows(); j++) {
            for (int i = 0; i < lines.cols(); i++) {
                data = lines.get(j, i);
                pt1.x = data[0];
                pt1.y = data[1];
                pt2.x = data[2];
                pt2.y = data[3];
                Imgproc.line(im, pt1, pt2, new Scalar(0, 0, 255), 3);
            }
        }
        for (int i = offset; i < gray.rows()-offset; i++) {
            CountByRow singleRow = new CountByRow(gray.cols());
            singleRow.row = i;
            for (int j = offset; j < gray.cols()-offset; j++) {
                double[] rgb = im.get(i, j);
                if (rgb[0] == 0 && rgb[1] == 0 && rgb[2] == 255.0) {
                    singleRow.incrementCount();
                    singleRow.setSinglePosition(j, 1);
                } else {
                    singleRow.setSinglePosition(j, 0);
                }
            }
            countByRow.add(singleRow);
        }
        int prevRow = 0;
        for (CountByRow row : countByRow) {
            if (row.count > 500 && (prevRow == row.row || row.row - prevRow > 20)) {
                filteredRows.add(row);
                prevRow = row.row;
            }
        }
        createRowLines(finalImg, filteredRows, getFirstColFromRows(filteredRows), getLastColFromRows(filteredRows));

        for (int i = offset; i < im.cols()-offset; i++) {
            CountByCol singleCol = new CountByCol(gray.rows());
            singleCol.col = i;
            for (int j = offset; j < im.rows()-offset; j++) {
                double[] rgb = im.get(j, i);
                if (rgb[0] == 0 && rgb[1] == 0 && rgb[2] == 255.0) {
                    singleCol.incrementCount();
                    singleCol.setSinglePosition(j, 1);
                } else {
                    singleCol.setSinglePosition(j, 0);
                }
            }
            countByCol.add(singleCol);
        }
        int prevCol = 0;
        CountByCol previousCol = countByCol.get(0);
        for (CountByCol row : countByCol) {
            if (row.count > 200 && (prevCol == row.col || row.col - prevCol > 15)) {
                filteredCols.add(row);
                prevCol = row.col;
                previousCol = row;
            } else if (row.count > 200 && (row.col - prevCol < 3)) {
                for (int j = 0; j < row.positions.length; j++) {
                    if (row.positions[j] == 1) {
                        if (previousCol.positions[j] == 0) {
                            previousCol.positions[j] = 1;
                            previousCol.count++;
                        }
                    }
                }
            }
        }
        //refilter col lines
        for(int j=0;j<filteredCols.size();j++){
            int count=0;
            if(filteredCols.get(j).col>(filteredCols.get(filteredCols.size()-3).col-1.75*(filteredCols.get(filteredCols.size()-1).col-filteredCols.get(filteredCols.size()-3).col))&&filteredCols.get(j).col<filteredCols.get(filteredCols.size()-3).col-10){
                System.out.println("");
                for(int i=0;i<filteredCols.get(j).positions.length;i++){
                    if(i>filteredRows.get(0).row&&i<filteredRows.get(filteredRows.size()-6).row){
                        if(filteredCols.get(j).positions[i]==1){
                            count++;
                        }
                    }
                }
            }
            if(count>150){
                filteredCols.remove(j);
            }
        }

        List<VotedSquares> votedSquares = createMainColLines(finalImg, filteredRows, filteredCols,false);
        List<VotedSquares> prefVotedSquares = createMainColLines(finalImg, filteredRows, filteredCols,true);

        mainVote=identifyVote(gray, finalImg, votedSquares,100,false).get(0)+1;
        prefVotes=identifyVote(gray, finalImg, prefVotedSquares,300,true);

        if(mainVote==-1||prefVotes.get(0)==-1){
            isSuccess=false;
        }else{
            isSuccess=true;
            System.out.println("Voted to the party : "+mainVote);
            System.out.print("Preferential votes : ");
            for(int i:prefVotes){
                System.out.print(i+1+"  ");
            }

            VotedSquares featureSquare = new VotedSquares();
            featureSquare.setStartCol(featureStartCol);
            featureSquare.setEndCol(featureEndCol);
            featureSquare.setStartRow(filteredRows.get(mainVote-1).row);
            featureSquare.setEndRow(filteredRows.get(mainVote).row);

            createSquareLines(finalImg,featureSquare);

            String partySymbolRaw = fe.getFeature(originalImg,featureSquare);
            String[] split=partySymbolRaw.split("\\.");
            System.out.println(fe.getFeature(originalImg,featureSquare));
            partySymbol=split[0];

            matToBufferedImage(finalImg,new BufferedImage(finalImg.width(),finalImg.height(), BufferedImage.TYPE_INT_RGB));
           // imwrite("E:\\test2.jpg", im);
           // imwrite("E:\\final.jpg", finalImg);
           // imwrite("E:\\gray.jpg", gray);
        }


    }

    public int getFirstColFromRows(List<CountByRow> countByRow) {
        int first = 3000;
        for (CountByRow row : countByRow) {
            for (int i = 0; i < row.positions.length; i++) {
                if (row.positions[i] == 1 && first > i) {
                    first = i;
                    break;
                }
            }
        }
        return first;
    }

    public int getLastColFromRows(List<CountByRow> countByRow) {
        int last = 0;
        for (CountByRow row : countByRow) {
            for (int i = 0; i < row.positions.length; i++) {
                if (row.positions[i] == 1 && last < i) {
                    last = i;
                }
            }
        }
        return last;
    }

    public void createRowLines(Mat im, List<CountByRow> countByRow, int first, int last) {
        for (CountByRow row : countByRow) {
            Point pt1 = new Point();
            Point pt2 = new Point();
            pt1.y = row.row;
            pt1.x = first;
            pt2.y = row.row;
            pt2.x = last;
            Imgproc.line(im, pt1, pt2, new Scalar(0, 255, 0), 3);
        }
    }

    public List<VotedSquares> createMainColLines(Mat im, List<CountByRow> countByRows, List<CountByCol> countByCols, boolean isPref) {
        List<VotedSquares> votedSquares = new ArrayList<>();
        int startRow;
        int endRow;
        int rowContainLimit;
        if(isPref){
            startRow=countByRows.get(countByRows.size() - 5).row;
            endRow=countByRows.get(countByRows.size() - 1).row;
            rowContainLimit=30;
        }else{
            startRow=countByRows.get(0).row;
            endRow=countByRows.get(countByRows.size() - 6).row;
            rowContainLimit=170;
        }
        CountByCol lastCol = null;
        CountByCol prevCol = null;
        List<CountByCol> prefCol = new ArrayList<>();
        List<CountByCol> mainCol = new ArrayList<>();
        for (CountByCol col : countByCols) {
            Point pt1 = new Point();
            Point pt2 = new Point();
            int rowContain = 0;
            for (int i = 0; i < col.positions.length; i++) {
                if (col.positions[i] == 1 && i > startRow && i < endRow) {
                    rowContain++;
                }
            }
            if (rowContain > rowContainLimit && !isPref) {
                if (col == countByCols.get(countByCols.size() - 1)) {
                    lastCol = col;
                } else {
                    lastCol = col;
                    prevCol = col;
                    mainCol.add(col);
                }
                pt1.y = startRow;
                pt1.x = col.col;
                pt2.y = endRow;
                pt2.x = col.col;
                Imgproc.line(im, pt1, pt2, new Scalar(0, 255, 0), 3);
            }else if (rowContain > rowContainLimit && isPref){
                prefCol.add(col);
                pt1.y = startRow;
                pt1.x = col.col;
                pt2.y = endRow;
                pt2.x = col.col;
                Imgproc.line(im, pt1, pt2, new Scalar(0, 255, 0), 3);
            }

        }
        if(!isPref) {
            featureStartCol = mainCol.get(mainCol.size() - 2).col;
            featureEndCol = mainCol.get(mainCol.size() - 1).col;
        }
        if(!isPref) {
            for (int i = 0; i < countByRows.size() - 6; i++) {
                VotedSquares square = new VotedSquares();
                square.setStartCol(prevCol.col);
                square.setEndCol(lastCol.col);
                square.setStartRow(countByRows.get(i).row);
                square.setEndRow(countByRows.get(i + 1).row);
                votedSquares.add(square);
            }
        }else{
            for(int i=5;i>1;i--){
                for(int j=0;j<prefCol.size()-1;j++){
                    VotedSquares square = new VotedSquares();
                    square.setStartCol(prefCol.get(j).col);
                    square.setEndCol(prefCol.get(j+1).col);
                    square.setStartRow(countByRows.get(countByRows.size()-i).row);
                    square.setEndRow(countByRows.get(countByRows.size()-i+1).row);
                    votedSquares.add(square);
                }
            }

        }
        return votedSquares;
    }
    public List<Integer> identifyVote(Mat gray, Mat im, List<VotedSquares> votedSquares,int threshold,boolean isPref) {
        List<Integer> votes = new ArrayList<>();
        int voteCount = 0;
        int whiteCount = 0;
        for (int i = 0; i < votedSquares.size(); i++) {
            VotedSquares square = votedSquares.get(i);
            for (int j = square.getStartRow() + 10; j < square.getEndRow() - 10; j++) {
                for (int k = square.getStartCol() + 10; k < square.getEndCol() - 10; k++) {
                    double[] rgb = gray.get(j, k);
                    if (rgb[0] == 255.0) {
                        whiteCount++;
                    }
                }
            }
            if(isPref){
                int trueThreshold;
                if(i<9){
                    trueThreshold=200;
                }else{
                    trueThreshold=threshold;
                }
                if (whiteCount > trueThreshold) {
                    votes.add(i);
                    voteCount++;
                }
            }else{
                if (whiteCount > threshold) {
                    votes.add(i);
                    voteCount++;
                }
            }
            whiteCount = 0;
        }
        if(!isPref){
            if (voteCount != 1) {
                votes.set(0,-1);
            } else {
                createSquareLines(im, votedSquares.get(votes.get(0)));
            }
        }else{
            if(voteCount<4){
                for(int i=0;i<votes.size();i++){
                    createSquareLines(im, votedSquares.get(votes.get(i)));
                }
            }else{
                votes=new ArrayList<>();
                votes.add(-1);
            }

        }

        return votes;
    }

    public void createSquareLines(Mat im, VotedSquares votedSquare) {
        Point pt1 = new Point();
        Point pt2 = new Point();
        Point pt3 = new Point();
        Point pt4 = new Point();
        pt1.x = votedSquare.getStartCol();
        pt1.y = votedSquare.getStartRow();
        pt2.x = votedSquare.getEndCol();
        pt2.y = votedSquare.getStartRow();
        pt3.x = votedSquare.getStartCol();
        pt3.y = votedSquare.getEndRow();
        pt4.x = votedSquare.getEndCol();
        pt4.y = votedSquare.getEndRow();

        Imgproc.line(im, pt1, pt2, new Scalar(0, 0, 255), 3);
        Imgproc.line(im, pt1, pt3, new Scalar(0, 0, 255), 3);
        Imgproc.line(im, pt3, pt4, new Scalar(0, 0, 255), 3);
        Imgproc.line(im, pt2, pt4, new Scalar(0, 0, 255), 3);
    }
    public void matToBufferedImage(Mat matrix, BufferedImage bimg)
    {
        if ( matrix != null ) {
            int cols = matrix.cols();
            int rows = matrix.rows();
            int elemSize = (int)matrix.elemSize();
            byte[] data = new byte[cols * rows * elemSize];
            int type=0;
            matrix.get(0, 0, data);
            switch (matrix.channels()) {
                case 1:
                    type = BufferedImage.TYPE_BYTE_GRAY;
                    break;
                case 3:
                    type = BufferedImage.TYPE_3BYTE_BGR;
                    // bgr to rgb
                    byte b;
                    for(int i=0; i<data.length; i=i+3) {
                        b = data[i];
                        data[i] = data[i+2];
                        data[i+2] = b;
                    }
                    break;
                default:;
            }

            // Reuse existing BufferedImage if possible
            if (bimg == null || bimg.getWidth() != cols || bimg.getHeight() != rows || bimg.getType() != type) {
                bimg = new BufferedImage(cols, rows, type);
            }
            bimg.getRaster().setDataElements(0, 0, cols, rows, data);
        } else { // mat was null
            bimg = null;
        }
        outputImg=bimg;
    }

    public BufferedImage getOutputImg() {
        return outputImg;
    }

    public int getMainVote() {
        return mainVote;
    }

    public List<Integer> getPrefVotes() {
        return prefVotes;
    }

    public boolean isSuccess() {
        return isSuccess;
    }
}

class CountByRow {
    int count = 0;
    int row;
    int[] positions;

    CountByRow(int count) {
        positions = new int[count];
    }

    public void incrementCount() {
        count++;
    }

    public void setSinglePosition(int i, int value) {
        positions[i] = value;
    }
}

class CountByCol {
    int col;
    int count = 0;
    int[] positions;

    CountByCol(int count) {
        positions = new int[count];
    }

    public void incrementCount() {
        count++;
    }

    public void setSinglePosition(int i, int value) {
        positions[i] = value;
    }
}

class VotedSquares {
    int startRow;
    int startCol;
    int endRow;
    int endCol;

    public int getStartRow() {
        return startRow;
    }

    public void setStartRow(int startRow) {
        this.startRow = startRow;
    }

    public int getStartCol() {
        return startCol;
    }

    public void setStartCol(int startCol) {
        this.startCol = startCol;
    }

    public int getEndRow() {
        return endRow;
    }

    public void setEndRow(int endRow) {
        this.endRow = endRow;
    }

    public int getEndCol() {
        return endCol;
    }

    public void setEndCol(int endCol) {
        this.endCol = endCol;
    }
}