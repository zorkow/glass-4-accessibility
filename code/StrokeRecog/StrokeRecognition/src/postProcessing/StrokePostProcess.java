package postProcessing;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
//import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import strokeData.Coord;
import strokeData.Stroke;
import strokeData.SubStroke;
import videoProcessing.ProcessImage;

/**
 * Class containing a number of methods for post-processing the collection of Strokes after they have been
 * obtained.
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-09-10
 */
public class StrokePostProcess {
	
	private static final int STROKE_REMOVE_THRESH = 70000;
	
	//if the angle (in degrees) between the two vectors of two consecutive sub-strokes is greater than 
	//this threshold, the strokes are split into separate strokes at this point..
	private static final int DIRECTION_CHANGE_THRESH = 160;
	
	/**
	 * Method to divide strokes where there is a sharp change of direction within a Stroke.
	 * If the angle between the vectors of two consecutive sub-strokes within a Stroke is greater than a 
	 * class threshold then the stroke is divided into two separate strokes at that point.
	 * (This is intended to round off the leading edges of strokes)
	 * 
	 * @param strokes - the list of strokes to analyse
	 * @return the list of strokes with any strokes containing sharp changes in direction divided into two.
	 */
	public static List<Stroke> splitStrokesByDirection(List<Stroke> strokes) {
		
		List<Stroke> corrected = new ArrayList<Stroke>();
		
		for(int i=0; i<strokes.size(); i++) {
			List<SubStroke> lss = strokes.get(i).getPoints();
			int splitRef = 0;
			
			for(int j=1; j<lss.size(); j++) {
				double angleDiff = (lss.get(j-1).getBearing()) - (lss.get(j).getBearing());
				angleDiff = Math.abs((angleDiff + 180) % 360) - 180;

				if(Math.abs(angleDiff)>DIRECTION_CHANGE_THRESH) {
					corrected.add(new Stroke(lss.subList(splitRef, j)));
					splitRef=j;
				}
			}
			corrected.add(new Stroke(lss.subList(splitRef, lss.size())));
		}
		
		return corrected;
	}
	
	
	/**
	 * Method which checks whether any Stroke does not contribute significantly to the ink trace (i.e. there
	 * is another stroke which covers the same area and is more important).  This can occur when the pen 
	 * partially traverses over an ink trace in the pen-up state, so is later incorrectly interpreted as 
	 * pen-down.  Hence, this method should correct for this.
	 * 
	 * @param textImg - the binary image of the ink trace.
	 * @param strokes - the list of pen strokes
	 */
	public static List<Stroke> removeRedundancy(Mat textImg, List<Stroke> strokes) {
		
		//extract only the pen-down strokes
		List<Stroke> penDownStrokes = new ArrayList<Stroke>();
		for(int i=0; i<strokes.size(); i++) {
			if(strokes.get(i).getPoints().get(0).isPenDown()) {
				penDownStrokes.add(strokes.get(i));
			}
		}
		
//		//create an image with all the pen strokes on for comparison.
//		Mat allStrokes = drawAllStrokes(penDownStrokes, textImg.size(), textImg.type());
		
		List<Double> diffs = new ArrayList<Double>();
		
		//calculate how much each stroke 'contributes' to the ink trace of the collection of strokes.
		for(int i=0; i<penDownStrokes.size(); i++) {
			ArrayList<Stroke> single = new ArrayList<Stroke>();
			single.add(penDownStrokes.get(i));
			Mat line = drawAllStrokes(single, textImg.size(), textImg.type());
			
			ArrayList<Stroke> input = new ArrayList<Stroke>();
			for(int j=0; j<penDownStrokes.size(); j++) {
				if(j!=i) {
					input.add(penDownStrokes.get(j));
				}
			}
			
			Mat strokeImg = drawAllStrokes(input, textImg.size(), textImg.type());
			Mat strokeImgDilate = ProcessImage.erode(strokeImg, 11);
			diffs.add(calcImpact(strokeImgDilate, line));	
		}
		
		//remove those ink traces that fall below a certain threshold.
		for(int i=0; i<diffs.size(); i++) {
			System.out.print("Difference " + i + ": " + diffs.get(i));
			if(diffs.get(i)<STROKE_REMOVE_THRESH) {
				System.out.print(" - Removed!\n");
				penDownStrokes.remove(i);
				diffs.remove(i);
				i--;
			} else {
				System.out.print("\n");
			}
		}
		
		return penDownStrokes;
	}
	
	/**
	 * Method to calculate the difference between two images.
	 * 
	 * @param textImg - the binary image of the ink trace.
	 * @param strokeImg - the binary image to compare to the ink trace.
	 * @return the absolute difference between the two input images.
	 */
	@SuppressWarnings("unused")
	private static double calcError(Mat textImg, Mat strokeImg) {
				
		Mat diff = new Mat();
		Core.absdiff(textImg, strokeImg, diff);
		Scalar s = Core.sumElems(diff);
		double[] val = s.val;
		double sum = 0;
		for(int i=0; i<val.length; i++) {
			sum += val[i];
		}
		return sum;
	}
	
	/**
	 * Method which calculates how much a given line 'contributes' to a given image.  If adding the line
	 * to the image changes it significantly, a larger number is returned.
	 * 
	 * @param textImg - the image of all the strokes except the line.
	 * @param line - the image of only the line.
	 * @return an indication of how much the line contributes to the stroke trace.
	 */
	private static double calcImpact(Mat textImg, Mat line) {
		
		Mat masked = new Mat();
		line.copyTo(masked, textImg);
		Mat diff2 = new Mat();
		Core.subtract(textImg, masked, diff2);
		double[] d = Core.sumElems(diff2).val;
		double sum=0;
		for(int i=0; i<d.length; i++) {
			sum += d[i];
		} 
		return sum;
	}
	
	
	/**
	 * Method to convert an object of type Stroke into a MatOfPoint.
	 * 
	 * @param in - the Stroke to convert.
	 * @return the Stroke as a MatOfPoint.
	 */
	private static MatOfPoint convertStrokeToMOP(Stroke in) {
		List<Point> lp = new ArrayList<Point>();
		List<SubStroke> lss = in.getPoints();
		
		for(int i=0; i<lss.size(); i++) {
			Coord c = lss.get(i).getStart();
			lp.add(new Point(c.getX(), c.getY()));
		}
		
		MatOfPoint line = new MatOfPoint();
		line.fromList(lp);
		return line;
	}
	
	/**
	 * Draws all the strokes in the list provided on a blank image.
	 * 
	 * @param strokes - the list of strokes to draw.
	 * @param size - the size of the image to draw.
	 * @param type - the type of the image.
	 * @return a binary image with the strokes written in black on a white background.
	 */
	private static Mat drawAllStrokes(List<Stroke> strokes, Size size, int type) {
		
		Mat out = Mat.zeros(size, type);
		
		List<MatOfPoint> lines = new ArrayList<MatOfPoint>();
		
		for(int i=0; i<strokes.size(); i++) {
			MatOfPoint mop = convertStrokeToMOP(strokes.get(i));
			lines.add(mop);
		}
		
		Core.polylines(out, lines, false, new Scalar(255,255,255), 5);
		Imgproc.threshold(out, out, 127, 255, Imgproc.THRESH_BINARY_INV);

		return out;
	}
	
	/**
	 * Implementation of the Douglas-Peucker algorithm to smooth out a list of ordered sub-strokes.
	 * 
	 * (Adapted from the pseudocode for the algorithm on the Douglas-Peucker Wikipedia page)
	 * 
	 * @param input - the list of sub-strokes to smooth out.
	 * @param epsilon - the criterion on which to decide which points to keep.  If the perpendicular distance
	 * between a point and the straight line between points of interest either side is less than this value
	 * then the point is removed.
	 * @return the smmoth list of sub-strokes.
	 */
	public static List<SubStroke> smoothStroke(List<SubStroke> input, double epsilon) {
		
		//Find the point with the maximum distance
		double dmax = -1.0;
		int index = -1;
		
		Coord firstPoint = input.get(0).getStart();
		Coord lastPoint = input.get(input.size()-1).getEnd();
		
		for(int i=1; i<input.size(); i++) {
			double d = shortestDistToSegment(input.get(i).getStart(), firstPoint, lastPoint); 
			if ( d > dmax ) {
				index = i;
				dmax = d;
			}
		}
		
		List<SubStroke> result = new ArrayList<SubStroke>();
		
		//If max distance is greater than epsilon, recursively simplify
		if(dmax > epsilon) {
        
			//Recursive call
			List<SubStroke> subResult1 = smoothStroke(input.subList(0, index), epsilon);
			List<SubStroke> subResult2 = smoothStroke(input.subList(index, input.size()), epsilon);
 
			//Build the result list
			result.addAll(subResult1);
			result.addAll(subResult2);
			
		} else {
			result.add(new SubStroke(firstPoint, lastPoint, input.get(0).isPenDown()));
		}
		
		//Return the result
		return result;
    	
	}
	
	/**
	 * Helper method to smoothStroke.  Finds the shortest (i.e. perpendicular) distance from a point to 
	 * a straight line.  The straight line is defined by two points.
	 * 
	 * @param point - the coordinates of the point to calculate the shortest distance from.
	 * @param firstLine - the coordinates of the first point on the stright line.
	 * @param lastLine - the coordinates of the last point on the straight line.
	 * @return the shortest (i.e. perpendicular) distance from the provided point to the straight line defined
	 * by the input points firstLine and lastLine.
	 */
	private static double shortestDistToSegment(Coord point, Coord firstLine, Coord lastLine) {
		
		double dx = lastLine.getX() - firstLine.getX();	//(x2 - x1)
		double dy = lastLine.getY() - firstLine.getY();	//(y2 - y1)
		
		double numerator = Math.abs(dy*point.getX() - dx*point.getY() 
				- firstLine.getX()*lastLine.getY() + lastLine.getX()*firstLine.getY());
		
		double denominator = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
		
		return numerator / denominator;
	}
	
	
	
	
//	/**
//	 * Method to print out a list of sub-strokes to an output file.
//	 * 
//	 * @param record - the list of sub-strokes
//	 * @param ref - a reference number to use in the output file name.
//	 */
//	public static void printSubStrokeRecord(List<SubStroke> record, int ref) {
//		
//		BufferedWriter out = null;
//		try {
//			out = new BufferedWriter(new FileWriter("C:\\Users\\Simon\\Desktop\\glassVids\\templates\\bPoint\\record-" + ref + ".txt"));
//			for(int i=0; i<record.size(); i++) {
//				SubStroke s = record.get(i);
//				out.write("Start: " + s.getStart() + " End: " + s.getEnd() + " PenDown: " + s.isPenDown() 
//						+ System.lineSeparator()); 
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		} finally {
//			if(out!=null) {
//				try {
//					out.close();
//				} catch(IOException e2) {
//					e2.printStackTrace();
//				}
//			}
//		}
//		
//	}
	
}
