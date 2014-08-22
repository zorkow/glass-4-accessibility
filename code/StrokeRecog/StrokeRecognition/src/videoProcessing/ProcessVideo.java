package videoProcessing;

import imageRegistration.MovementTracker;
import ink.canvas.TraceList;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Observable;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import ballpointLocating.BallpointLocator;
import penFinding.PenLocator;
import penTracking.KalmanFilter;
import postProcessing.StrokePostProcess;
import recognizer.RecognizeStroke;
import strokeData.*;
import upDownClassifier.StrokeClassifier;

/**
 * Abstract class used to process an input video.
 * The class has concrete implementations for different input video types (e.g. webcam, video file, 
 * collection of jpg images).
 * This forms the Model part of the Model-View pattern used for the GUI.
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-08-20
 */
public abstract class ProcessVideo extends Observable {
	
	private Mat img;	//the current frame.
	private Mat roi;	//the region of interest within which the template is matched.
	private Mat filteredImg;	//the current frame with some filtering or other processing applied.
	private PenLocator pl;	//the object used to find the template.
	private KalmanFilter filter;	//the object used to track the template.
	private BallpointLocator bpl;	//the object used to find the very tip of the pen.
	private StrokeClassifier sc;	//the object used to classify if a stroke is pen-up or pen-down.
	private MovementTracker mt;		//the object used to perform the video frame registration.
	
	//the threshold which determines whether the tracker has lost the template.  If the template-match fitness
	//drops below this value, the program reverts to searching the whole image to try to re-find the template.
	private final double FITNESS_THRESHOLD = 0.98;
	
	//constant which effectively defines the size of the ROI within which to look for the pen head. The ROI 
	//is extended by SEARCH_SIZE above, below, left and right of the predicted pen-tip location, so the ROI 
	//has dimensions 2*SEARCH_SIZE x 2*SEARCH_SIZE.
	private final int SEARCH_SIZE = 20;	
	
	//the distance (in pixels) between points in the re-sampled record.
	private final int RESAMPLE_STEP = 2;	
	
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
		temp = ProcessImage.prepareImage(temp);
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
		mt = new MovementTracker(100);
	}
	
	
	/**
	 * Method to actually carry out the processing of the input video.
	 * This is the key method in the Stroke Recognition system.
	 * The method follows the following pattern:
	 * 	-	find the template in the first frame.
	 *  -	loop through all the frames:
	 *  		- predict the next location of the pen using a filter.
	 *  		- find the affine transformation between the current frame and the previous one.
	 *  		- find the template in a small region of interest around the predicted location.
	 *  		- if the fitness of the template match is too poor in the ROI, search for the template in the 
	 *  		whole image.
	 *  		- find the ballpoint of the pen-tip and record it.
	 *  		- update the filter with the actual pen location.
	 *  - analyse the ballpoint location record to determine if the pen was in pen-up or pen-down state.
	 *  - carry out post-processing of the ballpoint record to obtain full pen strokes.
	 */
	public void startProcessing() {
		
		//extract the first frame from the input, process it and find the best template match location.  
		//Indicate the location with a green rectangle on the source image.
		img = getFrame();
		Highgui.imwrite("C:\\Users\\Simon\\Desktop\\glassVids\\dump\\frame_" + 1 + ".jpg", img);
		setROI(new Coord(0,0));
		filteredImg = ProcessImage.prepareImage(img);
		TempMatchOutput initialMatch = pl.findTemplatePyramid(filteredImg);
		ProcessImage.drawGreenRect(img, new Point(initialMatch.getBestMatch().getX(), 
				initialMatch.getBestMatch().getY()), pl.getTemplate().cols(), pl.getTemplate().rows());
		
		//initialise the filter with the initial location.
		filter = new KalmanFilter(initialMatch.getBestMatch(), 1.0, 0.5, 1.5);
		long t0, t1, t2, t3, t4=0, t5;
		
		//loop through all the frames.
		while(frameAvailable() && getFrameNum()<551) {
			
			t0 = System.currentTimeMillis();
			
			//update the GUI.
			setChanged();
	    	notifyObservers();
	    	
	    	t1 = System.currentTimeMillis();
	    		
	    	//get the next frame and process it.
			img = getFrame();
			Highgui.imwrite("C:\\Users\\Simon\\Desktop\\glassVids\\dump\\frame_" + getFrameNum() + ".jpg", img);
			filteredImg = ProcessImage.prepareImage(img);
			
			//transform the frame into the base coordinate system.
			Mat trans = mt.trackMovement(img, getFrameNum());
			
			t2 = System.currentTimeMillis();
			
	    	//predict the next location of the template.
	    	Coord predictedPos = filter.kalmanFilterPredict();
			
			//find the template in the ROI around the predicted position.
			Coord roiPos = setROI(predictedPos);
			TempMatchOutput localMatch = pl.findTemplateSimple(ProcessImage.prepareImage(roi));
			Coord globalPos = new Coord(roiPos.getX() + localMatch.getBestMatch().getX(), 
					roiPos.getY() + localMatch.getBestMatch().getY());
			
			t3 = System.currentTimeMillis();
			
			Coord bPoint = null;
			//if the template match fitness is too poor, search again for the template within the whole image:
			if(localMatch.getFitness()<FITNESS_THRESHOLD) {
				globalPos = pl.findTemplatePyramid(filteredImg).getBestMatch();
			}
			
			t4 = System.currentTimeMillis();

			//find the 'ballpoint' of the pen within the region of interest (i.e. within the matched template location):
			bPoint = bpl.findBallpoint(img.submat(globalPos.getY(), globalPos.getY()+pl.getTemplate().rows(), globalPos.getX(), globalPos.getX()+pl.getTemplate().cols()));
			//if the ballpoint is found (i.e. not null) adjust the location into the frame's coordinate system, 
			//then convert the point into the reference frame's coordinate system.
			if(bPoint!=null) {
				Coord converted = MovementTracker.convertAffine(new Coord(bPoint.getX() + globalPos.getX(), bPoint.getY() + globalPos.getY()), trans);
				bPoint.setX(converted.getX());
				bPoint.setY(converted.getY());
//				bPoint.setX(bPoint.getX() + globalPos.getX());
//				bPoint.setY(bPoint.getY() + globalPos.getY());
			}

			t5 = System.currentTimeMillis();
			
			//indicate the template location with a green rectangle on the source image.
			ProcessImage.drawGreenRect(img, new Point(globalPos.getX(), globalPos.getY()), 
					pl.getTemplate().cols(), pl.getTemplate().rows());
			
			//update the filter with the actual template location.
			filter.kalmanFilterMeasure(globalPos);
			
			//print out a summary for this frame.
			System.out.println("Time 1 = " + (t1-t0) + ", Time 2 = " + (t2-t1) + ", Time 3 = " + (t3-t2)
					+ ", Time 4 = " + (t4-t3) + ", Time 5 = " + (t5-t4));
			printSummary(predictedPos, globalPos, localMatch.getFitness(), bPoint);
		}
		
		//get a clean version of the final frame without the green rectangle tracking box drawn on it.
		img = getCurrentFrame();
		filteredImg = ProcessImage.prepareImage(img);
		//use the final frame to extract the ink trace.
		Mat inkTrace = ProcessImage.extractInkTrace(img, 1, 15);
		Highgui.imwrite("C:\\Users\\Simon\\Desktop\\glassVids\\templates\\bPoint\\inktrace.jpg", inkTrace);
		
		//The full transform relates how to get from the reference frame's coordinate system to the final
		//frame's coordinate system.  We want the reverse of this, so we invert the transformation matrix
		//and then 'warp' the final frame into reference frame's coordinate system.
		Mat inverseTrans = MovementTracker.invertAffine(mt.getFullTransform());
		img = MovementTracker.transformFrame(img, inverseTrans);
		filteredImg = MovementTracker.transformFrame(filteredImg, inverseTrans);
		inkTrace = MovementTracker.transformFrame(inkTrace, inverseTrans);
		
		//remove any erroneous points from the record (e.g. due to loss of template during tracking), 
		//smooth the record by taking a weighted average of neighbouring points, and 
		///resample ballpoint record to regular intervals:
		bpl.checkRecord();
		bpl.smoothRecord(1,2,1);
		bpl.resampleRecord(RESAMPLE_STEP);
		
		//record the ballpoint location as a Stroke. (All Strokes are initially assumed to be pen-down
		//and the full record is analysed later for pen-up strokes).
		sc.populateSubStrokeRecord(bpl.getResampledRecord());
		
		//add the full tracked pen path to the filtered image for comparison.
		sc.drawSubStrokes(filteredImg);
		
		//determine the pen-down/up strokes using the ink trace from the final frame.
		sc.analyseRecord(inkTrace);
		sc.createStrokes();
//		sc.setStrokeRecord(StrokePostProcess.splitStrokesByDirection(sc.getStrokeRecord()));
		
		//remove any strokes which are 'redundant' (do not contribute sufficiently to the ink trace) and
		//smooth out the final strokes.
		sc.setStrokeRecord(StrokePostProcess.removeRedundancy(inkTrace, sc.getStrokeRecord()));
//		for(int i=0; i<sc.getStrokeRecord().size(); i++) {
//			List<SubStroke> lss = StrokePostProcess.smoothStroke(sc.getStrokeRecord().get(i).getPoints(), 3);
//			sc.getStrokeRecord().get(i).setPoints(lss);
//		}
		//draw the estimated strokes on the final frame.
		sc.drawStrokes(img);

		//finally, update the GUI with the images showing the estimated Strokes and release any resources if necessary.
		Imgproc.cvtColor(inkTrace, inkTrace, Imgproc.COLOR_GRAY2BGR);
		sc.drawStrokes(inkTrace);
		img=inkTrace;
		setChanged();
		notifyObservers();
		
		try {
			sc.printStrokeRecord("C:\\Users\\Simon\\Desktop\\forMeeting\\forBehrang\\test.txt");
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		}
		
		releaseResources();
		
		List<TraceList> characters = RecognizeStroke.compileCharacters(sc.getStrokeRecord());
		RecognizeStroke.recognizeCharacters(characters);
		
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
	 * @param fitness - the fitness of the template match (how well it matches).
	 * @param bPoint - the estimated coordinates of the pen ballpoint.
	 */
	private void printSummary(Coord predicted, Coord actual, double fitness, Coord bPoint) {
		System.out.println("Frame " + getFrameNum() + ":");
		System.out.println("Predicted position: X = " + predicted.getX() + ", Y = " + predicted.getY());
		System.out.println("Actual position: X = " + actual.getX() + ", Y = " + actual.getY());
		System.out.println("Template match fitness = " + fitness);
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
	 * Method to return the last extracted frame from the video input (as opposed to the next one).
	 * 
	 * @return a Mat of the last extracted frame of the video.
	 */
	public abstract Mat getCurrentFrame();
	
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
	 * Method to release any used resources (e.g. VidCap) if required.
	 */
	public abstract void releaseResources();
	
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
	
	
//	Method no longer used:
	@SuppressWarnings("unused")
	private void printList(List<Coord> result, int index) {
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter("C:\\Users\\Simon\\Desktop\\glassVids\\templates\\bPoint\\bpoint-" + index + ".txt"));
			for(int i=0; i<result.size(); i++) {
				out.write(result.get(i).getX() + ", " + result.get(i).getY() + System.lineSeparator());
			}
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			if(out!=null) {
				try {
					out.close();
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}


