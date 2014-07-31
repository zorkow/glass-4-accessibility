package upDownClassifier;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import strokeData.Coord;
import strokeData.Stroke;
import strokeData.SubStroke;
import videoProcessing.ProcessImage;

/**
 * Class to carry out operations relating to determining if a pen sub-stroke is pen-up (not writing) or 
 * pen-down (writing).
 * Includes a method to draw all the pen-down Strokes onto an image.
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-07-30
 */
public class StrokeClassifier {

	private List<SubStroke> subStrokeRecord;	//the collection of all the pen sub-strokes in sequence.
	private List<Stroke> strokeRecord; 		//the collection of all complete strokes.
	
	private final int strokeContinuityThresh = 5; 	//if the length of a stroke is below this value, it is 
													//assimilated into the adjacent strokes. (i.e. if there 
													//is a pen-up stroke of length 4 between two pen-down 
													//strokes, it is assumed that the Strokes should actually 
													//be continuous).
	private final int lineErrorThresh = 80;	//when a sub-stroke is compared to an ink trace, if the 
											//difference exceeds this value, it is assumed to be pen-up.
	
	/**
	 * Constructor for StrokeClassifier.
	 * Initialises the subStrokeRecord and strokeRecord field variables.
	 */
	public StrokeClassifier() {
		subStrokeRecord = new ArrayList<SubStroke>();
		strokeRecord = new ArrayList<Stroke>();
	}
	
	/**
	 * Method to look through the full sub-stroke record and determine if each sub-stroke is pen-up or 
	 * pen-down.
	 * The provided source image is used to examine an area around each sub-stroke and look for ink traces.
	 * 
	 * @param src - the image to examine for ink traces.
	 */
	public void analyseRecord(Mat src) {
		
		for(int i=0; i<subStrokeRecord.size(); i++) {
			double error = checkLine(src, subStrokeRecord.get(i).getStart(), subStrokeRecord.get(i).getEnd());
			if(error<lineErrorThresh) {
				subStrokeRecord.get(i).setPenDown(true);
			} else {
				subStrokeRecord.get(i).setPenDown(false);
			}
		}
		
	}
	
	/**
	 * Method to compare a line defined by start and end coordinates to an ink trace.  Returns the difference
	 * between the original ink trace image and the ink trace image with the line drawn on it.  If the line
	 * represents ink, the returned value will be low.  If the line is across background, the returned value
	 * will be high.
	 * 
	 * @param bin - binary image of the ink trace.
	 * @param start - the first point on the line to compare to the ink trace.
	 * @param end - the last point on the line to compare to the ink trace.
	 * @return the difference between the original ink trace image and the ink trace image with the line 
	 * drawn on it
	 */
	private double checkLine(Mat bin, Coord start, Coord end) {
		
		int rowStart = Math.min(start.getY(), end.getY())-5;
		int rowEnd = Math.max(start.getY(), end.getY())+5;
		int colStart = Math.min(start.getX(), end.getX())-5;
		int colEnd = Math.max(start.getX(), end.getX())+5;
		
		Mat original = new Mat();
		original = bin.submat(rowStart, rowEnd, colStart, colEnd);
		Mat origWithLine = new Mat();
		original.copyTo(origWithLine);
		
		Core.line(origWithLine, new Point(start.getX()-colStart, start.getY()-rowStart), 
				new Point(end.getX()-colStart, end.getY()-rowStart), new Scalar(0,0,0));
		
		Mat diff = new Mat();
		Core.absdiff(original, origWithLine, diff);
		
		Scalar out = Core.sumElems(diff);
		double[] val = out.val;
		
		double lineLength = Math.sqrt(Math.pow(start.getX()-end.getX(),2) + Math.pow(start.getY()-end.getY(),2));
		
		return val[0]/lineLength;
	}
	
	/**
	 * Method to draw out all of the determined pen-down sub-strokes on a given image.
	 * Sub-strokes are drawn as a continuous red line.
	 * 
	 * @param src - the image on which to draw the pen-down sub-strokes.
	 */
	public void drawSubStrokes(Mat src) {
		
		for(int i=0; i<subStrokeRecord.size(); i++) {
			SubStroke s = subStrokeRecord.get(i);
			if(s.isPenDown()) {
				ProcessImage.drawRedLine(src, s.getStart(), s.getEnd());
			}
		}
		
	}
	
	/**
	 * Method to draw out all of the determined pen-down strokes on a given image.
	 * Each stroke is drawn drawn as a separate random colour line.
	 * 
	 * @param src - the image on which to draw the pen-down strokes.
	 */
	public void drawStrokes(Mat src) {
		
		for(int i=0; i<strokeRecord.size(); i++) {
			List<SubStroke> lss = strokeRecord.get(i).getPoints();
			Scalar colour = new Scalar((int) (Math.random()*220), (int) (Math.random()*220), (int) (Math.random()*220));
			for(int j=0; j<lss.size(); j++) {
				SubStroke ss = lss.get(j);
				if(ss.isPenDown()) {
					ProcessImage.drawColouredLine(src, ss.getStart(), ss.getEnd(), colour);
				}
			}
		}
		
	}
	
	/**
	 * Method to populate the strokeRecord field variable.
	 * Uses the subStrokeRecord to separate sequences of pen-down and pen-up sub-strokes into strokes.
	 */
	public void createStrokes() {
		
		boolean currentPenState = subStrokeRecord.get(0).isPenDown();
		Stroke strk = new Stroke();
		
		for(int i=0; i<subStrokeRecord.size(); i++) {
			SubStroke ss = subStrokeRecord.get(i);
			if(ss.isPenDown()==currentPenState) {
				strk.getPoints().add(ss);
			} else {
				strokeRecord.add(strk);
				strk = new Stroke();
				strk.getPoints().add(ss);
				currentPenState = !currentPenState;
			}
		}
		
		//correct for short Strokes:
		for(int i=0; i<strokeRecord.size(); i++) {
			Stroke strk2 = strokeRecord.get(i);
			if(strk2.getPoints().size()<strokeContinuityThresh) {
				//first swap the Pen Up/Down state to match adjacent Stroke(s)
				strokeRecord.get(i).changePenState();	
				if(i==0) {
					//merge first and second Strokes.
					strokeRecord.get(i).addPoints(strokeRecord.get(i+1).getPoints());
					strokeRecord.remove(i+1);
				} else if(i==strokeRecord.size()-1) {
					//merge last and second-last Strokes
					strokeRecord.get(i-1).addPoints(strokeRecord.get(i).getPoints());
					strokeRecord.remove(i);
				} else {
					//merge (i-1)th, ith, and (i+1)th Strokes
					strokeRecord.get(i-1).addPoints(strokeRecord.get(i).getPoints());
					strokeRecord.get(i-1).addPoints(strokeRecord.get(i+1).getPoints());
					strokeRecord.remove(i+1);
					strokeRecord.remove(i);
					i--;
				}
			}
		}
		
	}
	
	public List<Stroke> getStrokeRecord() {
		return strokeRecord;
	}
	
	public void setStrokeRecord(List<Stroke> newRecord) {
		this.strokeRecord = newRecord;
	}
	
	
	public void addSubStroke(SubStroke s) {
		subStrokeRecord.add(s);
	}
	
	public List<SubStroke> getSubStrokeRecord() {
		return subStrokeRecord;
	}
	
}
