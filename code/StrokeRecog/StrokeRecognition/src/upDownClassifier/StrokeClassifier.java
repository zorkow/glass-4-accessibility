package upDownClassifier;

import java.util.ArrayList;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import strokeData.Coord;
import strokeData.Stroke;
import videoProcessing.ProcessImage;

/**
 * Class to carry out operations relating to determining if a pen stroke is pen-up (not writing) or pen-down
 * (writing).
 * Includes a method to draw all the pen-down Strokes onto an image.
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-07-16
 */
public class StrokeClassifier {

	private ArrayList<Stroke> strokeRecord;	//the collection of all the pen-strokes in sequence.
	
	private static final int SEARCH_SIZE = 5;	//the area to search for ink traces (the search are will have
												//dimensions of 2*STROKE_SIZE by 2*STROKE_SIZE)
	private static final int STROKE_GAP = 3;	//Parameter to determine whether to connect two pen-down events
												//as a single or separate strokes.  If there are more strokes
												//than the parameter here between consecutive pen-downs, then
												//they are assumed to not be connected.
	private static final int INK_TRACE_THRESHOLD = 30;	//if this value is exceeded in the processed image 
														//around a stroke location, it is assumed that an ink
														//trace is present and hence the stroke in pen-down.
	
	
	/**
	 * Constructor for StrokeClassifier.
	 * Initialises the strokeRecord field variable.
	 */
	public StrokeClassifier() {
		strokeRecord = new ArrayList<Stroke>();
	}
	
	/**
	 * Method to look through the full stroke record and determine if each Stroke is pen-up or pen-down.
	 * The provided source image is used to examine an area around each stroke and look for ink traces.
	 * 
	 * @param src - the image to examine for ink traces.
	 */
	public void analyseRecord(Mat src) {
		
		for(int i=0; i<strokeRecord.size(); i++) {
			Stroke strk = strokeRecord.get(i);
			
			//determine the region of interest to examine.
			int colStart = (strk.getLocation().getX()-SEARCH_SIZE>0) ? strk.getLocation().getX()-SEARCH_SIZE : 0;
			int colEnd = (strk.getLocation().getX()+SEARCH_SIZE<src.cols()) ? strk.getLocation().getX()+SEARCH_SIZE : src.cols();
			int rowStart = (strk.getLocation().getY()-SEARCH_SIZE>0) ? strk.getLocation().getY()-SEARCH_SIZE : 0;
			int rowEnd = (strk.getLocation().getY()+SEARCH_SIZE<src.rows()) ? strk.getLocation().getY()+SEARCH_SIZE : src.rows();			
			Mat roi = src.submat(rowStart, rowEnd, colStart, colEnd);
//			Highgui.imwrite("C:\\Users\\Simon\\Desktop\\frames7\\inkroi-" + i + ".jpg", roi);
			
			//convert the image to gray and Gaussian blur, then threshold it.
			Mat result = new Mat();
			Imgproc.cvtColor(roi, result, Imgproc.COLOR_BGR2GRAY);
			Imgproc.GaussianBlur(result, result, new Size(9,9), 0);
			Imgproc.adaptiveThreshold(result, result, 1, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 7, 0);
//			Highgui.imwrite("C:\\Users\\Simon\\Desktop\\frames7\\inkroithresh-" + i + ".jpg", result);
			
			//find how many pixels remain on after processing.
			int sum = 0;
			for(int j=0; j<3; j++) {
				sum += Core.sumElems(result).val[j];
			}
			
			//if the threshold is exceeded, it is assumed an ink trace is present and penDown is true.
			//Otherwise, penDown is set to false.
			if(sum>INK_TRACE_THRESHOLD) {
				strokeRecord.get(i).setPenDown(true);
			} else {
				strokeRecord.get(i).setPenDown(false);
			}
			
			
		}
		
	}
	
	/**
	 * Method to draw out all of the determined pen-down Strokes on a given image.
	 * 
	 * @param src - the image on which to draw the pen-down Strokes.
	 */
	public void drawStrokes(Mat src) {
		
		//initialise parameters
		Coord c1 = new Coord(0,0), c2 = new Coord(0,0);
	    int lastDraw = 0;
	    
	    int start = findFirstPenDown();
	    c1 = new Coord(strokeRecord.get(start).getLocation().getX(), strokeRecord.get(start).getLocation().getY());
		lastDraw = start;
	    
	    for(int i=start+1; i<strokeRecord.size(); i++) {
	    	if(strokeRecord.get(i).isPenDown()) {
	    		c2 = new Coord(strokeRecord.get(i).getLocation().getX(), strokeRecord.get(i).getLocation().getY());
	    		if(i-lastDraw < STROKE_GAP) {
	    			ProcessImage.drawRedLine(src, c1, c2);
	    		}
		    	c1 = c2;
		    	lastDraw=i;
	    	}
	    }
	}
	
	/**
	 * Finds the first Stroke in the list to be classified as pen-down.
	 * 
	 * @return the index of the first Stroke within strokeRecord to be classified as pen-down.
	 */
	private int findFirstPenDown() {
		for(int i=0; i<strokeRecord.size(); i++) {
	    	if(strokeRecord.get(i).isPenDown()) {
	    		return i;
	    	}
	    }
		return -1;
	}
	
	
	public void addStroke(Stroke s) {
		strokeRecord.add(s);
	}
	
	public ArrayList<Stroke> getStrokeRecord() {
		return strokeRecord;
	}
	
}
