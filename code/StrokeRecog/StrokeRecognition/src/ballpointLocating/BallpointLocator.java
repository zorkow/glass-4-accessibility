package ballpointLocating;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import strokeData.Coord;
import strokeData.SubStroke;
import videoProcessing.ProcessImage;

/**
 * Class to perform operations related to estimating the position of the 'ballpoint' of the pen (i.e. the 
 * very end point of the pen where it contacts the whiteboard).
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-07-30
 */
public class BallpointLocator {

	private ArrayList<Coord> ballpointRecord;	//the list of coordinates of the location of the ballpoint in sequence.
	private ArrayList<Coord> resampledRecord;	//the ballpointRecord resampled at regular intervals as defined by the 
												//step size provided to the resampleRecord() method. 
	private Coord validZoneTopLeft; //the coordinates of the top left point of the zone in which a ballpoint is
									//considered valid (keep zone as small as practicable for better accuracy in results)
	private Coord validZoneBottomRight; //the coordinates of the bottom right point of the zone in which a ballpoint is
										//considered valid (keep zone as small as practicable for better accuracy in results)
	
	private final double gaussianBlurSrcWeight = 4.5;
	private final double gaussianBlurBlurredWeight = -0.5;
	private final int dilateKSize = 2;	//the dimensions of the kernel to use in the image dilation step.
	private final int cannyLow = 150;	//the lower threshold to use in the Canny edge detector
	private final int cannyHigh = 250;	//the upper threshold to use in the Canny edge detector.
	private final int contourLengthThresh = 50;	//the length below which contours are rejected.
	private final int houghThreshold = 15;	//the threshold to use with the Hough transform (a lower value will 
											//result in more lines being returned from the Hough transform)
	private final int lineLimit = 100;	//the limit on the number of lines obtained from the Hough transform.
	
//	private int count = 0;	//just used to output part-processed images for inspection.
	
	/**
	 * Constructor for the BallpointLocator.
	 * Initialises the ballpointRecord.
	 * 
	 * @param validZoneTopLeft - the coordinates of the top left point of the zone in which a ballpoint is
	 * considered valid (keep zone as small as practicable for better accuracy in results)
	 * @param validZoneBottomRight - the coordinates of the bottom right point of the zone in which a 
	 * ballpoint is considered valid (keep zone as small as practicable for better accuracy in results) 
	 */
	public BallpointLocator(Coord validZoneTopLeft, Coord validZoneBottomRight) {
		this.validZoneTopLeft = validZoneTopLeft;
		this.validZoneBottomRight = validZoneBottomRight;
		ballpointRecord = new ArrayList<Coord>();
		resampledRecord = new ArrayList<Coord>();
	}
	
	/**
	 * Method to estimate the ballpoint within the matched template image.
	 * 
	 * @param src - the image in which to find the ballpoint (should be the size of the template)
	 * @return the estimated location of the ballpoint within the source image.
	 */
	public Coord findBallpoint(Mat src) {
		
//		Highgui.imwrite("C:\\Users\\Simon\\Desktop\\glassVids\\templates\\bPoint\\" + count + "-1-src.jpg", src);
		
		//first attempt to remove blur from the image, the use Canny to detect edges.
		Mat sharpen = ProcessImage.sharpenWithGaussianBlur(src, gaussianBlurSrcWeight, gaussianBlurBlurredWeight);	
		Mat detectedEdges = new Mat();
		detectedEdges = ProcessImage.cannyEdge(sharpen, cannyHigh, cannyLow);
		
		//use the detected edges to derive contours.
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Mat hierarchy = new Mat();
		Imgproc.findContours(detectedEdges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		
		//Only pick those contours which are above the threshold, then dilate them to improve their prominance.
		Mat filteredEdges = filterContours(contours, contourLengthThresh, detectedEdges.size(), detectedEdges.type());
		filteredEdges = ProcessImage.dilate(filteredEdges, dilateKSize);
		
		//Use the edges to detect lines in the image.
		Mat lines = new Mat();
		Imgproc.HoughLines(filteredEdges, lines, 1, Math.PI/180, houghThreshold);
		
		//if too many lines are found, restrict their number to the specified limit.
		if(lines.cols()>lineLimit) {
			lines = lines.submat(0, 1, 0, lineLimit);
		}
		
		//Use the processed image to estimate the ballpoint location.
		Mat edgesBGR = new Mat();
		Imgproc.cvtColor(filteredEdges, edgesBGR, Imgproc.COLOR_GRAY2BGR);
		Coord bPoint = ballpointLocate(edgesBGR, lines);
		
//		Highgui.imwrite("C:\\Users\\Simon\\Desktop\\glassVids\\templates\\bPoint\\" + count + "-2-edges.jpg", edgesBGR);
//		Mat out = new Mat();
//		edgesBGR.copyTo(out);
//		ProcessImage.drawAllLines(out, lines);
//		Highgui.imwrite("C:\\Users\\Simon\\Desktop\\glassVids\\templates\\bPoint\\" + count + "-3-hough.jpg", out);
//		count++; 
		
		//if null is returned, the ballpoint could not be located, so don't add it to the record.
		if(bPoint!=null) {
			ballpointRecord.add(bPoint);
		} 
		
		return bPoint;

	}
	
	/**
	 * Method to remove any contours that are below a certain length.  The contours that are kept are drawn
	 * on an output Mat.
	 * 
	 * @param contours - the list of contours to process.
	 * @param lengthThreshold - the length threshold under which contours are rejected.
	 * @param imgSize - the size of the output Mat.
	 * @param imgType - the type of the output Mat.
	 * @return a Mat on which the contours that are above the length threshold are drawn.
	 */
	private Mat filterContours(List<MatOfPoint> contours, int lengthThreshold, Size imgSize, int imgType) {
		List<MatOfPoint> longContours = new ArrayList<MatOfPoint>();
		for(int i=0; i<contours.size(); i++) {
			if(contours.get(i).rows()>lengthThreshold) {
				longContours.add(contours.get(i));
			}
		}
		Mat filtered = Mat.zeros(imgSize, imgType);
		for(int i=0; i<longContours.size(); i++) {
			Imgproc.drawContours(filtered, longContours, i, new Scalar(255,255,255));
		}
		
		return filtered;
	}
	
	
	/**
	 * Method which uses the Hough transform lines to estimate the ballpoint location.  The intersection
	 * points of lines that are within a specific zone are averaged to give the estimated ballpoint.
	 * 
	 * @param src - the image with the detected edges (as a BGR image)
	 * @param lines - the Hough transform lines Mat.
	 * @return the estimated coordinates of the ballpoint.
	 */
	private Coord ballpointLocate(Mat src, Mat lines) {
		
		int num = (int) lines.size().width;
		double bPointX = 0.0;
		double bPointY = 0.0;
		int count=0;
		
		//compare each line to every other line.  If they intersect within the defined valid zone, add the
		//intersection coordinated to the X and Y ballpoint variables.
		for(int i=0; i<num; i++) {
			double[] firstLine = lines.get(0, i);
			for(int j=i+1; j<num; j++) {
				double[] secondLine = lines.get(0,  j);
				Coord intersection = findIntersection(firstLine[0], firstLine[1], secondLine[0], secondLine[1]);
				if(intersection.getX()>validZoneTopLeft.getX() && intersection.getY()>validZoneTopLeft.getY()
						&& intersection.getX()<validZoneBottomRight.getX() 
						&& intersection.getY()<validZoneBottomRight.getY()) {
					bPointX += intersection.getX();
					bPointY += intersection.getY();
					count++;
				}
			}	
		}
		
		Coord bPoint = null;
		//as long as at least one intersection has been found within the valid zone, calculate the average
		//of all intersections and then adjust this such that it sits on the nearest detected edge.
		if(count>0) {
			bPointX = bPointX/count;
			bPointY = bPointY/count;
			bPoint = new Coord((int) Math.round(bPointX), (int) Math.round(bPointY));
			bPoint = findNearestPenShadow(src, bPoint);	//(may return null if an edge cannot be found nearby)
		}
		//bPoint may be null if no intersection was found or no edge was found near the average intersection location.
		if(bPoint!=null) {
			ProcessImage.drawGreenRectangle(src, bPoint, 3, 3);
		}
		
		return bPoint;
	}
	
	
	/**
	 * Finds the intersection point between two lines defined in polar coordinates by the parameters rho 
	 * and theta.
	 * 
	 * @param rho1 - the length of the radius vector of line 1.
	 * @param theta1 - the angle of the radius vector of line 1.
	 * @param rho2 - the length of the radius vector of line 2.
	 * @param theta2 - the angle of the radius vector of line 2.
	 * @return the coordinates of the intersection point of the two lines.
	 */
	private static Coord findIntersection(double rho1, double theta1, double rho2, double theta2) {
		
		double x0 = rho1*Math.cos(theta1);
		double y0 = rho1*Math.sin(theta1);
		double x1 = x0 - Math.sin(theta1);
		double y1 = y0 + Math.cos(theta1);
		
		double m1 = (y1-y0) / (x1-x0);
		double c1 = -m1 * x1 + y1;
		
		double x2 = rho2*Math.cos(theta2);
		double y2 = rho2*Math.sin(theta2);
		double x3 = x2 - Math.sin(theta2);
		double y3 = y2 + Math.cos(theta2);
		
		double m2 = (y3-y2) / (x3-x2);
		double c2 = -m2 * x3 + y3;
		
		double xIn = (c1-c2) / (m2-m1);
		double yIn = m1 * xIn + c1;
		
		return new Coord((int) xIn, (int) yIn);
		
	}
	
	/**
	 * Method to find the nearest detected edge to the estimated ballpoint position.  This is such that the
	 * estimated ballpoint can be ensured to be a point on the pen.
	 * The method searches progressively widening radius around the estimated ballpoint location until it 
	 * finds an edge.
	 * 
	 * @param src - the image with the detected edges (as a BGR image)
	 * @param bPointEst - the estimated ballpoint location based on the Hough transform lines analysis.
	 * @return a revised estimate of the ballpoint positioned on the nearest edge to the estimated location.
	 */
	private Coord findNearestPenShadow(Mat src, Coord bPointEst) {
		
		Coord adjustedBPoint = null;
		
		//first check whether the estimated ballpoint is already on an edge.
		double[] vals = src.get(bPointEst.getY(), bPointEst.getX());
		if(vals[0]==255 && vals[1]==255 &&  vals[2]==255) {
			adjustedBPoint = new Coord((bPointEst.getX()), (bPointEst.getY()));
			return adjustedBPoint;
		}
		
		//search radius increases to 20 - if an edge is not found, it is assumed that the estimate is poor
		//and is therefore ignored.
		for(int i=1; i<20; i++) {
			adjustedBPoint = searchRoundPixel(src, bPointEst, i);
			if(adjustedBPoint!=null) {
				break;
			}
		}
		
		return adjustedBPoint;
	}
	
	
	/**
	 * Method to search all the pixels a certain radius (step) from the estimated ballpoint location.  The
	 * method returns as soon as a pixel is found that contains an edge (i.e. has pixel intensity 255 for
	 * all BGR).
	 * 
	 * @param src - the image with the detected edges (as a BGR image)
	 * @param bPointEst - the estimated ballpoint location based on the Hough transform lines analysis.
	 * @param step - the distance/radius from the estimated location to search.
	 * @return the first encountered pixel with intensity 255 in all BGR space, or null if no such pixel is
	 * found.
	 */
	private Coord searchRoundPixel(Mat src, Coord bPointEst, int step) {
		
		int minX = bPointEst.getX()-step>0 ? -step : 0;
		int maxX = bPointEst.getX()+step<src.cols() ? step : src.cols()-1-bPointEst.getX();
		int minY = bPointEst.getY()-step>0 ? -step : 0;
		int maxY = bPointEst.getY()+step<src.rows() ? step : src.rows()-1-bPointEst.getY();
		
		Coord adjustedBPoint = null;
		
		//search 'columns'
		for(int i=minX; i<=maxX; i+=(maxX-minX)) {
			for(int j=minY; j<=maxY; j++) {
				
				double[] vals = src.get(bPointEst.getY()+j, bPointEst.getX()+i);
				if(vals[0]==255 && vals[1]==255 &&  vals[2]==255) {
					adjustedBPoint = new Coord((bPointEst.getX() + i), (bPointEst.getY() + j));
					return adjustedBPoint;
				}
			}
		}
		
		//search 'rows'
		for(int j=minY; j<=maxY; j+=(maxY-minY)) {
			for(int i=minX; i<=maxX; i++) {
				
				double[] vals = src.get(bPointEst.getY()+j, bPointEst.getX()+i);
				if(vals[0]==255 && vals[1]==255 &&  vals[2]==255) {
					adjustedBPoint = new Coord((bPointEst.getX() + i), (bPointEst.getY() + j));
					return adjustedBPoint;
				}
			}
		}
		
		return adjustedBPoint;
	}
	
	/**
	 * Method that can be used to 'smooth out' an input list of coordinates using an implementation of the 
	 * Douglas-Peucker algorithm.  
	 * 
	 * (The algorithm is amended from the pseudocode on the Wikipedia page for the Douglas-Peucker algorithm).
	 * 
	 * @param input - the list of coordinates to smooth out.
	 * @param epsilon - the distance criterion on which to decide whether to keep a point or not. (distance in pixels)
	 * @return a smoothed out version of the input list of coordinates based on the specified epsilon.
	 */
	public static List<Coord> DouglasPeucker(List<Coord> input, double epsilon) {
		
		//Find the point with the maximum distance
		double dmax = -1.0;
		int index = -1;
		int end = input.size();
		
		for(int i=1; i<end-1; i++) {
			double d = shortestDistToSegment(input.get(i), input.get(0), input.get(end-1)); 
			if ( d > dmax ) {
				index = i;
				dmax = d;
			}
		}
		
		List<Coord> result = new ArrayList<Coord>();
		
		//If max distance is greater than epsilon, recursively simplify
		if(dmax > epsilon) {
        
			//Recursive call
			List<Coord> subResult1 = DouglasPeucker(input.subList(0, index), epsilon);
			List<Coord> subResult2 = DouglasPeucker(input.subList(index, end), epsilon);
 
			//Build the result list
			result.addAll(subResult1);
			result.addAll(subResult2);
			
		} else {
			result.add(input.get(0));
			result.add(input.get(end-1));
		}
		
		//Return the result
		return result;
    	
	}
	
	/**
	 * Helper method to Douglas-Peucker.  Finds the shortest (i.e. perpendicular) distance from a point to 
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
	
	/**
	 * Populates the resampledRecord field variable.  
	 * The ballpointRecord is resampled at regular intervals specified by the specified 'step' value.
	 * 
	 * @param step - the distance between the resampled points.
	 */
	public void resampleRecord(int step) {
		
		List<SubStroke> resampled = new ArrayList<SubStroke>();
		
		double currentDist = 0;
		
		for(int i=0; i<ballpointRecord.size()-1; i++) {
			
			Coord c3 = ballpointRecord.get(i);
			Coord c4 = ballpointRecord.get(i+1);
			SubStroke strk = new SubStroke(c3, c4, true);
			
			double currentLength = strk.getLength();
			double currentdx = strk.getEnd().getX() - strk.getStart().getX();
			double currentdy = strk.getEnd().getY() - strk.getStart().getY();
			
			double x = strk.getStart().getX() + currentDist/currentLength * (currentdx);
			double y = strk.getStart().getY() + currentDist/currentLength * (currentdy);
			
			Coord c1 = new Coord((int) Math.round(x), (int) Math.round(y));
			
			while(currentDist<currentLength) {
				x = x + step/currentLength * (currentdx);
				y = y + step/currentLength * (currentdy);
				
				Coord c2 = new Coord((int) Math.round(x), (int) Math.round(y));
				
				resampled.add(new SubStroke(c1, c2, true));
				c1=c2;				
				currentDist += step;

			}
			
			currentDist = currentDist-currentLength;
		}
		
		for(int i=0; i<resampled.size(); i++) {
			if(i<resampled.size()-1) {
				resampledRecord.add(resampled.get(i).getStart());
			} else {
				resampledRecord.add(resampled.get(i).getEnd());
			}
		}
		
	}
	
	public ArrayList<Coord> getBallpointRecord() {
		return ballpointRecord;
	}
	
	public ArrayList<Coord> getResampledRecord() {
		return resampledRecord;
	}
	
	
	
	
//	Methods no longer used:
//	/**
//	 * Method to check that the estimated ballpoint location is not too dissimilar to those determined in 
//	 * the previous frames. The method takes the average of the previous 5 points and checks that the new
//	 * point is within a radius of 10 pixels (Euclidean distance) of this average point.
//	 * 
//	 * @param newPoint - the new point to check.
//	 */
//	private void validateCoord(Coord newPoint) {
//		
//		double avX = 0;
//		double avY = 0;
//		for(int i=ballpointRecord.size()-1; i>ballpointRecord.size()-6; i--) {
//			avX += ballpointRecord.get(i).getX();
//			avY += ballpointRecord.get(i).getY();
//		}
//		avX = avX/5;
//		avY = avY/5;
//		double dist = Math.sqrt(Math.pow((avX-newPoint.getX()), 2) + Math.pow((avY-newPoint.getY()), 2));
//		if(dist<10) {
//			ballpointRecord.add(newPoint);
//		} else {
//			ballpointRecord.add(new Coord((int) Math.round(avX), (int) Math.round(avY)));
//		}
//		
//	}
	
	
	//OLD FINDBALLPOINT METHOD:
//public Coord findBallpoint(Mat src, FilterRange fr) {
//		
//		Mat dilate = new Mat();
//		Mat detectedEdges = new Mat();
//
//		//normalise the source image, then filter the image to only leave the pen head colour, then dilate 
//		//the result to leave an intact pen head.
////		dilate = ProcessImage.normalise(src);
//		Highgui.imwrite("C:\\Users\\Simon\\Desktop\\glassVids\\templates\\bPoint\\" +count + "-1-src.jpg", src);
////		dilate = ProcessImage.filterColour(src, fr.getLow(), fr.getHigh());
////		Highgui.imwrite("C:\\Users\\Simon\\Desktop\\glassVids\\templates\\bPoint\\" + count + "-2-filter.jpg", dilate);
////		dilate = ProcessImage.dilate(dilate, dilateKSize);
//		
//		//blur the image then carry out the edge detection.
////		detectedEdges = ProcessImage.blur(dilate, blurKSize);
//		detectedEdges = ProcessImage.cannyEdge(src, cannyHigh, cannyLow);
//
//		//convert the detectEdges Mat to the BGR space
//		Mat edgesBGR = new Mat();
//		Imgproc.cvtColor(detectedEdges, edgesBGR, Imgproc.COLOR_GRAY2BGR);
//
//		//perform the Hough transform to determine the lines from the detected edges.
//		Mat lines = new Mat();
//		Imgproc.HoughLines(detectedEdges, lines, 1, Math.PI/180, houghThreshold);
//
//		//use the lines to determine the estimated ballpoint
//		Coord bPoint = ballpointLocate(edgesBGR, lines);
//		Highgui.imwrite("C:\\Users\\Simon\\Desktop\\glassVids\\templates\\bPoint\\" + count + "-2-edges.jpg", edgesBGR);
//		Mat out = new Mat();
//		edgesBGR.copyTo(out);
//		ProcessImage.drawAllLines(out, lines);
//		Highgui.imwrite("C:\\Users\\Simon\\Desktop\\glassVids\\templates\\bPoint\\" + count + "-3-hough.jpg", out);
//		count++; 
//		
//		//if null is returned, the ballpoint could not be located, so don't add it to the record.
//		if(bPoint!=null) {
//			ballpointRecord.add(bPoint);
//		} 
//		
//		return bPoint;
//	}
		
}
