package postProcessing;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import strokeData.Coord;
import strokeData.Stroke;
import strokeData.SubStroke;

/**
 * Class containing a number of methods for post-processing the collection of Strokes after they have been
 * obtained.
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-07-30
 */
public class StrokePostProcess {
	
	private static final int STROKE_REMOVE_THRESH = 60000;
	
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
		
		//create an image with all the pen strokes on for comparison.
		Mat allStrokes = drawAllStrokes(penDownStrokes, textImg.size(), textImg.type());
		List<Double> diffs = new ArrayList<Double>();
		
		//calculate how much each stroke 'contributes' to the ink trace of the collection of strokes.
		for(int i=0; i<penDownStrokes.size(); i++) {
			ArrayList<Stroke> input = new ArrayList<Stroke>();
			for(int j=0; j<penDownStrokes.size(); j++) {
				if(j!=i) {
					input.add(penDownStrokes.get(j));
				}
			}
			diffs.add(calcError(allStrokes, input));	
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
	 * Method to calculate the difference between an input image of an ink trace and the collection of 
	 * strokes that form that ink trace.
	 * 
	 * @param textImg - the binary image of the ink trace.
	 * @param strokes - the list of strokes to compare to the ink trace.
	 * @return the difference between the input image and an image created from drawing the list of strokes.
	 */
	private static double calcError(Mat textImg, List<Stroke> strokes) {
		
		Mat strokeImg = drawAllStrokes(strokes, textImg.size(), textImg.type());
		
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
