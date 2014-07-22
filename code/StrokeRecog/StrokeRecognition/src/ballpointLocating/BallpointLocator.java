package ballpointLocating;

import java.util.ArrayList;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import strokeData.Coord;
import videoProcessing.ProcessImage;

/**
 * Class to perform operations related to estimating the position of the 'ballpoint' of the pen (i.e. the 
 * very end point of the pen where it contacts the whiteboard).
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-07-16
 */
public class BallpointLocator {

	private ArrayList<Coord> ballpointRecord;	//the list of coordinates of the location of the ballpoint in sequence.
	private Coord validZoneTopLeft; //the coordinates of the top left point of the zone in which a ballpoint is
									//considered valid (keep zone as small as practicable for better accuracy in results)
	private Coord validZoneBottomRight; //the coordinates of the bottom right point of the zone in which a ballpoint is
										//considered valid (keep zone as small as practicable for better accuracy in results)
	
	
	private final Scalar filterLow = new Scalar(0,0,0);		//the lower bound of the filter (in HSV colour space)
	private final Scalar filterHigh = new Scalar(255,75,75); //the upper bound of the filter (in HSV colour space)
	private final int dilateKSize = 3;	//the dimensions of the kernel to use in the image dilation step.
	private final int blurKSize = 3;	//the dimensions of the kernel to use in the image blur step.
	private final int cannyLow = 125;	//the lower threshold to use in the Canny edge detector
	private final int cannyHigh = 250;	//the upper threshold to use in the Canny edge detector.
	private final int houghThreshold = 10;	//the threshold to use with the Hough transform (a lower value will 
											//result in more lines being returned from the Hough transform)
	
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
	}
	
	/**
	 * Method to estimate the ballpoint within the matched template image.
	 * 
	 * @param src - the image in which to find the ballpoint (should be the size of the template)
	 * @return the estimated location of the ballpoint within the source image.
	 */
	public Coord findBallpoint(Mat src) {
		
		Mat dilate = new Mat();
		Mat detectedEdges = new Mat();

		//normalise the source image, then filter the image to only leave the pen head colour, then dilate 
		//the result to leave an intact pen head.
		dilate = ProcessImage.normalise(src);
		dilate = ProcessImage.filterColour(dilate, filterLow, filterHigh);
		dilate = ProcessImage.dilate(dilate, dilateKSize);
		
		//blur the image then carry out the edge detection.
		detectedEdges = ProcessImage.blur(dilate, blurKSize);
		detectedEdges = ProcessImage.cannyEdge(detectedEdges, cannyHigh, cannyLow);

		//convert the detectEdges Mat to the BGR space
		Mat edgesBGR = new Mat();
		Imgproc.cvtColor(detectedEdges, edgesBGR, Imgproc.COLOR_GRAY2BGR);

		//perform the Hough transform to determine the lines from the detected edges.
		Mat lines = new Mat();
		Imgproc.HoughLines(detectedEdges, lines, 1, Math.PI/180, houghThreshold);

		//use the lines to determine the estimated ballpoint
		Coord bPoint = ballpointLocate(edgesBGR, lines);
		
		//if null is returned, the ballpoint could not be located, so don't add it to the record.
		if(bPoint!=null) {
			ballpointRecord.add(bPoint);
		} 
		
		return bPoint;
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
			Highgui.imwrite("C:\\Users\\Simon\\Desktop\\frames4\\ballpoint-" + ballpointRecord.size() + ".jpg", src);
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
		
		//search radius increases to 10 - if an edge is not found, it is assumed that the estimate is poor
		//and is therefore ignored.
		for(int i=1; i<10; i++) {
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
	
		
}
