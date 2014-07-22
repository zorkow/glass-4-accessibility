package videoProcessing;

import java.util.Observable;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.highgui.Highgui;

import ballpointLocating.BallpointLocator;
import penFinding.PenLocator;
import penTracking.KalmanFilter;
import strokeData.*;
import upDownClassifier.StrokeClassifier;

/**
 * Abstract class used to process an input video.
 * The class has concrete implementations for different input video types (e.g. webcam, video file, 
 * collection of jpg images).
 * This forms the Model part of the Model-View pattern used for the GUI.
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-07-16
 */
public abstract class ProcessVideo extends Observable {
	
	private Mat img;	//the current frame.
	private Mat roi;	//the region of interest within which the template is matched.
	private Mat filteredImg;	//the current frame with some filtering or other processing applied.
	private PenLocator pl;	//the object used to find the template.
	private KalmanFilter filter;	//the object used to track the template.
	private BallpointLocator bpl;	//the object used to find the very tip of the pen.
	private StrokeClassifier sc;	//the object used to classify if a stroke is pen-up or pen-down.
	
	//the threshold which determines whether the tracker has lost the template.  If the template-match error
	//has exceeded this value, the program reverts to searching the whole image to try to re-find the template.
	private int errorThreshold = 70000;
	
	//constant which effectively defines the size of the ROI. The ROI is extended by SEARCH_SIZE above, 
	//below, left and right of the predicted pen-tip location, so the ROI has dimensions 
	//2*SEARCH_SIZE x 2*SEARCH_SIZE.
	private static final int SEARCH_SIZE = 20;	
	
	/**
	 * Constructor for ProcessVideo with automatic template extraction.
	 */
	public ProcessVideo() {
		pl = new PenLocator();
		initialise();
	}
	
	/**
	 * Constructor for ProcessVideo with user-defined template.
	 * 
	 * @param template - the full file path including extension of the image to use as the template.
	 */
	public ProcessVideo(String template) {
		Mat temp = Highgui.imread(template);
		temp = ProcessImage.filterColour(temp, ProcessImage.BLACK_LOW_HSV, ProcessImage.BLACK_HIGH_HSV);
		pl = new PenLocator(temp);
		initialise();
	}
	
	/**
	 * Method to initialise the main field variables.
	 */
	private void initialise() {
		img = new Mat();
		roi = new Mat();
		bpl = new BallpointLocator(new Coord(-10,-10), new Coord(pl.getTemplate().cols()/2,
				pl.getTemplate().rows()/2));
		sc = new StrokeClassifier();
	}
	
	
	/**
	 * Method to actually carry out the processing of the input video.
	 * This is the key method in the Stroke Recognition system.
	 * The method follows the following pattern:
	 * 	-	find the template in the first frame.
	 *  -	loop through all the frames:
	 *  		- predict the next location of the pen using a filter.
	 *  		- find the template in a small region of interest around the predicted location.
	 *  		- if the error on the template match is too high in the ROI, search for the template in the 
	 *  		whole image.
	 *  		- if the error is not too high, find the ballpoint of the pen-tip and record it.
	 *  		- update the filter with the actual pen location.
	 *  - analyse the ballpoint location record to determine if the pen was in pen-up or pen-down state.
	 *  - carry out post-processing of the strokes (NOT YET IMPLEMENTED).
	 */
	public void startProcessing() {
		
		//extract the first frame from the input, process it and find the best template match location.  
		//Indicate the location with a green rectangle on the source image.
		img = getFrame();
		setROI(new Coord(0,0));
		filteredImg = ProcessImage.filterColour(img, ProcessImage.BLACK_LOW_HSV, ProcessImage.BLACK_HIGH_HSV);
		TempMatchOutput initialMatch = pl.findTemplate(filteredImg);
		ProcessImage.drawGreenRect(img, new Point(initialMatch.getBestMatch().getX(), 
				initialMatch.getBestMatch().getY()), pl.getTemplate().cols(), pl.getTemplate().rows());
		
		//initialise the filter with the initial location.
		filter = new KalmanFilter(initialMatch.getBestMatch(), 1.0, 0.5, 1.5);
		
		//loop through all the frames.
		while(frameAvailable()) {
			//update the GUI.
			setChanged();
	    	notifyObservers();
	    	
	    	//predict the next location of the template.
	    	Coord predictedPos = filter.kalmanFilterPredict();
	    	
	    	//get the next frame and process it.
			img = getFrame();
			filteredImg = ProcessImage.filterColour(img, ProcessImage.BLACK_LOW_HSV, ProcessImage.BLACK_HIGH_HSV);
			
			//find the template in the ROI around the predicted position.
			Coord roiPos = setROI(predictedPos);
			TempMatchOutput localMatch = pl.findTemplate(ProcessImage.filterColour(roi, 
					ProcessImage.BLACK_LOW_HSV, ProcessImage.BLACK_HIGH_HSV));
			Coord globalPos = new Coord(roiPos.getX() + localMatch.getBestMatch().getX(), 
					roiPos.getY() + localMatch.getBestMatch().getY());
			
			Coord bPoint = null;
			//if the template match error is too high, search again for the template within the whole image:
			if(localMatch.getError()>errorThreshold) {
				globalPos = pl.findTemplate(filteredImg).getBestMatch();
			} else {
			//otherwise, find the 'ballpoint' of the pen within the region of interest:
				bPoint = bpl.findBallpoint(img.submat(globalPos.getY(), globalPos.getY()+pl.getTemplate().rows(), globalPos.getX(), globalPos.getX()+pl.getTemplate().cols()));
				if(bPoint!=null) {
					bPoint.setX(bPoint.getX() + globalPos.getX());
					bPoint.setY(bPoint.getY() + globalPos.getY());
					//record the ballpoint location as a Stroke. (All Strokes are initially assumed to be pen-up
					//and the full record is process later for pen-down strokes).
					sc.addStroke(new Stroke(bPoint, true));
				}
			}
			//indicate the template location with a green rectangle on the source image.
			ProcessImage.drawGreenRect(img, new Point(globalPos.getX(), globalPos.getY()), 
					pl.getTemplate().cols(), pl.getTemplate().rows());
			
			//update the filter with the actual template location.
			filter.kalmanFilterMeasure(globalPos);
			
			//print out a summary for this frame.
			printSummary(predictedPos, globalPos, localMatch.getError(), bPoint);
		}
		
		//determine the pen-down strokes and draw them on the final frame.
//		sc.analyseRecord(img);
		sc.drawStrokes(img);
	    
		setChanged();
		notifyObservers();
		
	}
	
	
	/**
	 * Method to extract the region of interest based on the specified central coordinate and the class-wide
	 * search size.  
	 * 
	 * @param centre - the coordinates of the centre point of the region of interest
	 * @return the coordinates of the top left corner of the region of interest.
	 */
	private Coord setROI(Coord centre) {
		int colStart = (centre.getX()-SEARCH_SIZE>0) ? centre.getX()-SEARCH_SIZE : 0;
		int colEnd = (centre.getX()+SEARCH_SIZE+pl.getTemplate().cols()<img.cols()) ? centre.getX()+SEARCH_SIZE+pl.getTemplate().cols() : img.cols();
		int rowStart = (centre.getY()-SEARCH_SIZE>0) ? centre.getY()-SEARCH_SIZE : 0;
		int rowEnd = (centre.getY()+SEARCH_SIZE+pl.getTemplate().rows()<img.rows()) ? centre.getY()+SEARCH_SIZE+pl.getTemplate().rows() : img.rows();
		roi = img.submat(rowStart, rowEnd, colStart, colEnd);
		return new Coord(colStart, rowStart);
	}
	
	/**
	 * Method to print out a summary of the current frame's data.
	 * 
	 * @param predicted - the predicted position of the template.
	 * @param actual - the location of the best template match.
	 * @param error - the error on the template match.
	 * @param bPoint - the estimated coordinates of the pen ballpoint.
	 */
	private void printSummary(Coord predicted, Coord actual, double error, Coord bPoint) {
		System.out.println("Frame " + getFrameNum() + ":");
		System.out.println("Predicted position: X = " + predicted.getX() + ", Y = " + predicted.getY());
		System.out.println("Actual position: X = " + actual.getX() + ", Y = " + actual.getY());
		System.out.println("Template match error = " + error);
		if(bPoint!=null) {
			System.out.println("Estimated ballpoint location: X = " + bPoint.getX() + ", Y = " + bPoint.getY());
		}
		System.out.print("\n");
	}
	
	/**
	 * Method to return the next frame from the video input.
	 * 
	 * @return a Mat of the next frame of the video.
	 */
	public abstract Mat getFrame();
	
	/**
	 * Method to check whether the video input has ended or if there is another frame available.
	 * 
	 * @return true if there is another frame available, false otherwise (the video has ended).
	 */
	public abstract boolean frameAvailable();
	
	/**
	 * Method to return the current frame number.
	 * 
	 * @return the current frame number.
	 */
	public abstract int getFrameNum();
	
	/**
	 * getter for the PenLocator object.
	 * 
	 * @return pL - the PenLocator object.
	 */
	public PenLocator getPL() {
		return pl;
	}
	
	/**
	 * getter for the Mat image representing the current frame of the video.
	 * 
	 * @return a Mat of the current frame image.
	 */
	public Mat getImg() {
		return img;
	}
	
	/**
	 * getter for the Mat image representing the region of interest in the current frame.
	 * 
	 * @return a Mat of the current region of interest.
	 */
	public Mat getROI() {
		return roi;
	}
	
	/**
	 * getter method for the filtered/processed version of the current frame image.
	 * 
	 * @return a Mat of the filtered/processed version of the current frame image.
	 */
	public Mat getFilteredImg() {
		return filteredImg;
	}
	
}


