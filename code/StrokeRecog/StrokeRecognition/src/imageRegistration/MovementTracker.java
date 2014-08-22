package imageRegistration;


import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
//import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import penFinding.PenLocator;
import penTracking.MatrixOps;
import strokeData.Coord;
import videoProcessing.ProcessImage;

/**
 * Class that can be used for video frame registration (assuming Affine transformations between frames).  
 * Each successive frame is passed to the trackMovement() method.  When this method first detects text
 * within the image, this frame becomes the reference frame.  The trackMovement() method returns the 
 * transformation from the current frame back to the reference frame.
 * The class also provides methods to use affine transformations, e.g. warp a frame by an affine 
 * transformation, convert a coordinate using an affine transformation, or invert an affine transformation
 * matrix to get the reverse transformation.
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-08-20
 */
public class MovementTracker {

	private int windowSize;				//the size of the zones to use in the registration process.
	private Mat prevFrameGray;			//the previously supplied frame.
	private List<Zone> prevZones;		//the valid registration zones identified in the previously supplied 
										//frame (includes text zones and whiteboard zones).
	private List<Zone> prevTextZones;	//the text zones identified in the previously supplied frame.
	private Mat fullTransform;			//the affine transformation from the reference frame to the last 
										//processed frame.
	
	//any registration points found within this percentage of the height or width of the frame are ignored
	//(i.e. those points close to the border are ignored).
	private static final double FILTER_FACTOR = 0.05;		
	
	//the minimum number of text zones that must be identified for the image to be classified as containing text.
	private static final int TEXT_ZONE_MIN = 3;
	
	//the minimum number of feature points required for the affine transformation process to be used (instead
	//of the translation-only approach). (Note: require a min of 3 to calculate an affine transformation.) 
	private static final int FEATURE_POINT_MIN = 3;
	
	//if the error on a transformation between two frames is greater than this value, the transformation is
	//ignored.
	private static final double TRANS_ERROR_THRESH = 1;
	
	//the magnitude of the search area dimension to extend beyond the template in the translation-only
	//transformation estimation.
	private static final int SEARCH_DIM = 25;
	
	//the maximum number of tracking points to find (set to a high number and control number of points 
	//using TRACING_THRESH.
	private static final int MAX_TRACKING_POINTS = 100;	
	
	//refers to the quality of the corners found in the image used for tracking (refer to 
	//OpenCV Imgproc.goodFeaturesToTrack() method).
	private static final double TRACKING_THRESH = 0.2;
	
	//the minimum distance between tracking points.
	private static final int MIN_TRACKING_SPACING = 20;
	
	/**
	 * Constructor for MovementTracker objects.
	 * 
	 * @param windowSize - the dimension of the square zones to use when examining an image for text.
	 */
	public MovementTracker(int windowSize) {
		this.windowSize = windowSize;
		fullTransform = getNoChangeTrans();
		prevZones = new ArrayList<Zone>();
		prevTextZones = new ArrayList<Zone>();
		prevFrameGray = new Mat();
	}
	
	/**
	 * Method to track the movement between frames.  The method receives the current frame and works out
	 * the transformation between it and the previous frame.  It then uses this transformation to calculate
	 * the transformation between the current frame and the reference frame. 
	 * The reference frame is identified as the first frame to be passed to the method which is found to 
	 * contain text.
	 * 
	 * @param currentFrame - the frame to register.
	 * @return the transformation from the current frame to the reference frame.
	 */
	public Mat trackMovement(Mat currentFrame, int num) {
		
		Mat currentFrameGray = ProcessImage.convertToGray(currentFrame);
		
		//zone the current image into blocks of dimension windowSize and classify them as either whiteboard,
		//text or 'other' (hand/pen).
		List<Zone> currentZones = Zone.classifyZones(currentFrameGray, windowSize);
		currentZones = Zone.removeZoneOfType(currentZones, 2);
		List<Zone> currentTextZones = Zone.getZoneOfType(currentZones, 1);
		boolean poorTransform = false;
		
		//if text has been detected in consecutive frames, it is likely that this truly is text, so 
		//register the frames
		if(prevTextZones.size()>=TEXT_ZONE_MIN && currentTextZones.size()>=TEXT_ZONE_MIN) {

			//we create the rectangular ROI we will use to register the two frames.  The ROI is formed by
			//finding a large text area among the text zones, and then expanding this zone as far as possible 
			//to the top and left of the image.  This should give us a satisfactorily large zone to register with.
			Rect registerZone = Zone.findLargestRect(prevTextZones);
			registerZone = Zone.expandRect(registerZone, prevZones);
			registerZone.trimRect(10);	//cut the edges in case the pen/hand is just encroaching on the image.
			
			int rowStart = registerZone.getLocation().getY();
			int rowEnd = rowStart + registerZone.getHeight();
			int colStart = registerZone.getLocation().getX();
			int colEnd = colStart + registerZone.getWidth();
			
			Mat subPrevFrameGray = prevFrameGray.submat(rowStart, rowEnd, colStart, colEnd);
			Mat subCurrentFrameGray = currentFrameGray.submat(rowStart, rowEnd, colStart, colEnd);
			
			//find the features to track in the previous image.  The method goodFeaturesToTrack() should find
			//strong reliable features only (e.g. the end points of ink traces).
			MatOfPoint trackPoints = new MatOfPoint();
			Imgproc.goodFeaturesToTrack(subPrevFrameGray, trackPoints, MAX_TRACKING_POINTS, 
					TRACKING_THRESH, MIN_TRACKING_SPACING);
			
			//the tracking points found at the edge of the image may not be present in the next frame, so remove
			//these from the list.
			trackPoints = filterOutPtsNrBoundary(trackPoints, registerZone);
			
			Mat transform = new Mat();
			
			//if we have enough tracking points, calculate the affine transformation, otherwise, calculate the
			//translation transformation.
			if(trackPoints.rows()>=FEATURE_POINT_MIN) {
				
				MatOfPoint2f prevPoints = new MatOfPoint2f(trackPoints.toArray());
				MatOfPoint2f nextPoints = new MatOfPoint2f();
				MatOfByte status = new MatOfByte();
				MatOfFloat err = new MatOfFloat();
				
				//find the tracking points in the current frame using the Lucas-Kanade optical flow technique.
				Video.calcOpticalFlowPyrLK(subPrevFrameGray, subCurrentFrameGray, prevPoints, nextPoints, status, err);
//				Mat out1 = drawPoints(subPrevFrameGray, prevPoints);
//				Mat out2 = drawPoints(subCurrentFrameGray, nextPoints);
//				Highgui.imwrite("C:\\Users\\Simon\\Desktop\\glassVids\\dump\\points_" + num + "-1.jpg", out1);
//				Highgui.imwrite("C:\\Users\\Simon\\Desktop\\glassVids\\dump\\points_" + num + "-2.jpg", out2);
				
				if(num>470) {
					System.out.println("");
				}
				
				//check that the estimated nextPoints are within the image boundary. If not, they are removed.
				verifyPoints(prevPoints, nextPoints, registerZone);
				
				if(prevPoints.rows()>=FEATURE_POINT_MIN) {
				//use the two sets of points to calculate the affine transformation between frames.
				transform = Video.estimateRigidTransform(prevPoints, nextPoints, false);
				
				//estimate the transformation error. If it is too large, ignore the transform, and assume no
				//movement between frames.
				double transError = Double.MAX_VALUE;
				if(transform.rows()!=0) {
					transError = calcTransError(prevPoints, nextPoints, transform);
				}
				System.out.println("Transform error = " + transError);
				if(transError>TRANS_ERROR_THRESH) {
					transform = getNoChangeTrans();
					poorTransform = true;
				}
				} else {
					transform = registerImgTranslation(prevFrameGray, currentFrameGray, registerZone);
				}
				
			} else {
				//calculate the translation-only transformation.
				transform = registerImgTranslation(prevFrameGray, currentFrameGray, registerZone);
			}
			//update the transformation from the reference frame to the current frame.
			updateFullTransform(transform);
			
		} 
		
		//as long as there wasn't a poor affine transformation, make the current frame the previous frame.
		if(!poorTransform) {
			currentFrameGray.copyTo(prevFrameGray);
			prevZones = currentZones;
			prevTextZones = currentTextZones;
		}
		
		//we want to return the transformation from the current frame back to the reference frame (this
		//is the inverse of the full transform from reference to current frame.
		return MovementTracker.invertAffine(fullTransform);
	}
	
	/**
	 * Helper method to remove points near to the boundary of a registration zone.  
	 * The method uses the constant FILTER_FACTOR to define the distance from the boundary.
	 * 
	 * @param trackPoints - the list of all tracking points.
	 * @param registerZone - the rectangular zone used for frame registration
	 * @return - the filtered tracking points, with those near the boundary removed.
	 */
	private MatOfPoint filterOutPtsNrBoundary(MatOfPoint trackPoints, Rect registerZone) {
		
		MatOfPoint filtered = new MatOfPoint();
		
		for(int i=0; i<trackPoints.rows(); i++) {
			double[] d = trackPoints.get(i, 0);
			if(d[0] < (1-FILTER_FACTOR)*registerZone.getWidth() && d[0] > FILTER_FACTOR*registerZone.getWidth()
					&& d[1] < (1-FILTER_FACTOR)*registerZone.getHeight() && d[1] > FILTER_FACTOR*registerZone.getHeight()) {
				Point p = new Point(d);
				MatOfPoint row = new MatOfPoint();
				row.fromArray(p);
				filtered.push_back(row);
			}
		}
		
		return filtered;
	}
	
	/**
	 * Method to check that the estimated positions of the tracking points in the current frame (nextPoints)
	 * are within the registration zone.  
	 * 
	 * @param prevPoints
	 * @param nextPoints
	 * @param registerZone
	 */
	private void verifyPoints(MatOfPoint2f prevPoints, MatOfPoint2f nextPoints, Rect registerZone) {
		
		MatOfPoint2f prevPointsRevised = new MatOfPoint2f();
		MatOfPoint2f nextPointsRevised = new MatOfPoint2f();
		List<Point> prevRevisedInput = new ArrayList<Point>();
		List<Point> nextRevisedInput = new ArrayList<Point>();
		
		for(int i=0; i<nextPoints.rows(); i++) {
			double[] d = nextPoints.get(i, 0);
			if(d[0]>0 && d[0]<registerZone.getWidth() && d[1]>0 && d[1]<registerZone.getHeight()) {
				double[] d2 = prevPoints.get(i, 0);
				prevRevisedInput.add(new Point(d2[0], d2[1]));
				nextRevisedInput.add(new Point(d[0], d[1]));
			}
		}
		
		prevPointsRevised.fromList(prevRevisedInput);
		nextPointsRevised.fromList(nextRevisedInput);
		
		prevPointsRevised.copyTo(prevPoints);
		nextPointsRevised.copyTo(nextPoints);
	}
	
	/**
	 * Helper method to update the fullTransform which described the relationship from the reference 
	 * frame to the previous frame, to describe the relationship from the reference from to the current
	 * frame.
	 * 
	 * @param transform - the transformation from the previous frame to the current frame.
	 */
	private void updateFullTransform(Mat transform) {
		
		double[][] full = new double[3][3];
		double[][] trans = new double[3][3];
		
		//first we convert the full transform into a 3x3 array (adding 0, 0, 1 as the bottom row).
		for(int i=0; i<fullTransform.rows(); i++) {
			for(int j=0; j<fullTransform.cols(); j++) {
				full[i][j] = fullTransform.get(i, j)[0];
			}
		}
		full[2][0] = 0; full[2][1]=0; full[2][2] = 1;

		//next we similarly convert the transform from previous to current frame into a 3x3 array
		for(int i=0; i<transform.rows(); i++) {
			for(int j=0; j<transform.cols(); j++) {
				trans[i][j] = transform.get(i, j)[0];
			}
		}
		trans[2][0] = 0; trans[2][1]=0; trans[2][2] = 1;

		//we multiply the matrices together.
		double[][] output = MatrixOps.matrixMult(trans, full);

		//finally we put the result into the fullTransform field variable.
		for(int i=0;i<fullTransform.rows(); i++) {
			for(int j=0; j<fullTransform.cols(); j++) {
				fullTransform.put(i,j, new double[] {output[i][j]});
			}
		}
		
	}
	
	/**
	 * Helper method to estimate how good the transformation is.  
	 * This is calculated by finding the sum of the Euclidean distances between the transformed tracking 
	 * points and the actual locations of the points in the next frame.   
	 * The error is normalised by dividing by the number of tracking points.
	 * 
	 * @param prevPoints - the tracking points in the previous frame.
	 * @param nextPoints - the estimated positions of the tracking points in the current frame.
	 * @param trans - the estimate affine transformation between the two frames.
	 * @return an estimate of the transformation error.
	 */
	private static double calcTransError(MatOfPoint2f prevPoints, MatOfPoint2f nextPoints, Mat trans) {
		
		double sum=0;
		
		for(int i=0; i<nextPoints.rows(); i++) {
			
			double x = trans.get(0, 0)[0]*prevPoints.get(i, 0)[0] + trans.get(0, 1)[0]*prevPoints.get(i, 0)[1] + trans.get(0, 2)[0];
			double y = trans.get(1, 0)[0]*prevPoints.get(i, 0)[0] + trans.get(1, 1)[0]*prevPoints.get(i, 0)[1] + trans.get(1, 2)[0];
			
			sum += Math.sqrt(Math.pow(nextPoints.get(i, 0)[0]-x, 2) + Math.pow(nextPoints.get(i, 0)[1]-y, 2));
		}
		
		return sum / nextPoints.rows();
	}
	
	
	/**
	 * Method to get an affine transformation matrix which describes no change between frames.
	 * 
	 * @return the 2x3 affine transformation matrix which describes no change.
	 */
	private static Mat getNoChangeTrans() {
		
		Mat noChange = Mat.eye(2, 3, CvType.CV_64FC1);
		
		return noChange;
	}
	
	/**
	 * Method to estimate the translation between two frames using template matching.  The template is 
	 * defined by a rectangle within the first frame and the search area in the second frame is defined 
	 * as the same area, but expanded by an area defined by the constant SEARCH_DIM.
	 * 
	 * @param oldFrame - the frame to extract the template from.
	 * @param frame - the frame within which to look for the template.
	 * @param r - the rectangle which defines the template.
	 * @return the estimated translation of the region defined by r between oldFrame and frame.
	 */
	private static Mat registerImgTranslation(Mat oldFrame, Mat frame, Rect r) {
		
		int x = r.getLocation().getX();
		int y = r.getLocation().getY();
		int width = r.getWidth();
		int height = r.getHeight();
		
		//we correct the template area if it is too close to the top left corner of the image (otherwise
		//we may not accurately find negative translations).
		if(x<SEARCH_DIM) {
			width = width+x-SEARCH_DIM;
			x = SEARCH_DIM; 
		} 
		if (y<SEARCH_DIM) {
			height = height+y-SEARCH_DIM;
			y=SEARCH_DIM;
		}
		
		//defined the template and search ROI.
		Mat imgObject = oldFrame.submat(y, y+r.getHeight(), x, x+r.getWidth());
		int rowStart = y-SEARCH_DIM;
		int rowEnd = y+r.getHeight()+SEARCH_DIM<frame.rows() ? y+r.getHeight()+SEARCH_DIM : frame.rows();
		int colStart = x-SEARCH_DIM;
		int colEnd = x+r.getWidth()+SEARCH_DIM<frame.cols() ? x+r.getWidth()+SEARCH_DIM : frame.cols();
		Mat imgScene = frame.submat(rowStart, rowEnd, colStart, colEnd);
		
		//find the template in the frame.
		PenLocator pl = new PenLocator(imgObject);
		Coord c = pl.findTemplateSimple(imgScene).getBestMatch();
		c.addToX(colStart-x);
		c.addToY(rowStart-y);
		
		//convert the x and y translations into the format of an affine translation.
		Mat trans = new Mat(new Size(3,2), CvType.CV_64FC1);
		trans.put(0, 0, new double[] {1});
		trans.put(0, 1, new double[] {0});
		trans.put(0, 2, new double[] {c.getX()});
		trans.put(1, 0, new double[] {0});
		trans.put(1, 1, new double[] {1});
		trans.put(1, 2, new double[] {c.getY()});
		
		return trans;
	}
	
	
	/**
	 * Method to convert a given coordinate into a new coordinate system using a given affine transformation 
	 * matrix.
	 * 
	 * @param c - the coordinate to convert.
	 * @param trans - the affine transformation matrix to use to convert the coordinate.
	 * @return the coordinate converted into the new coordinate system.
	 */
	public static Coord convertAffine(Coord c, Mat trans) {
		
		double x = c.getX() * trans.get(0, 0)[0] + c.getY() * trans.get(0, 1)[0] + trans.get(0, 2)[0];
		double y = c.getX() * trans.get(1, 0)[0] + c.getY() * trans.get(1, 1)[0] + trans.get(1, 2)[0];

		return new Coord((int) Math.round(x), (int) Math.round(y));
		
	}
	
	/**
	 * Method to invert a 2x3 affine transformation matrix to give the reverse transformation.
	 * 
	 * @param trans - the 2x3 affine transformation matrix to invert.
	 * @return the 2x3 inverted affine transformation matrix.
	 */
	public static Mat invertAffine(Mat trans) {
		
		//we create a temporary matrix to hold a 3x3 version of the transformation matrix.
		//we add (0, 0, 1) to the end of the temporary transformation matrix to give a 3x3 matrix.
		Mat temp = new Mat(0,0,trans.type());
		Mat lastRow = new Mat(1,3,trans.type());
		lastRow.put(0, 0, new double[] {0}); lastRow.put(0, 1, new double[] {0}); lastRow.put(0, 2, new double[] {1});
		
		temp.push_back(trans);
		temp.push_back(lastRow);
		
		//we invert the matrix.
		Mat inverse = new Mat();
		Core.invert(temp, inverse);
		
		//we store the output in a new 2x3 matrix and return it.
		Mat out = new Mat(2,3,trans.type());
		for(int i=0; i<trans.rows(); i++) {
			for(int j=0; j<trans.cols(); j++) {
				out.put(i, j, new double[] {inverse.get(i, j)[0]});
			}
		}
		
		return out;
	}
	
	
	/**
	 * Method to warp a frame using a 2x3 affine transformation matrix. 
	 * 
	 * @param src - the frame to warp/
	 * @param trans - the 2x3 affine transformation matrix.
	 * @return a Mat containing the warped frame.
	 */
	public static Mat transformFrame(Mat src, Mat trans) {
		Mat warp = new Mat();
		Size s = new Size(src.cols(), src.rows());
//		Imgproc.warpAffine(src, warp, trans, s);
		Imgproc.warpAffine(src, warp, trans, s, Imgproc.INTER_LINEAR, Imgproc.BORDER_CONSTANT, 
				new Scalar(245,245,245));
		return warp;
	}
	
	
	/**
	 * getter for fullTransform.
	 * 
	 * @return fullTransform - the affine transformation between the reference frame and the last 
	 * processed frame.
	 */
	public Mat getFullTransform() {
		return fullTransform;
	}
	
	/**
	 * Method to draw out a set of points as small green rectangles on a given image. 
	 * 
	 * @param img - the image to draw the points on.
	 * @param points - the points to draw on the image (each point is stored on a new row in column 0 as an
	 * array of doubles - i.e. can be used directly with MatOfPoint or MatOfPoint2f).
	 * @return - a copy of the provided image with the points drawn on as small green rectangles.
	 */
	@SuppressWarnings("unused")
	private static Mat drawPoints(Mat img, Mat points) {
		
		Mat copy = new Mat();
		img.copyTo(copy);
		
		for(int i=0; i<points.rows(); i++) {
			double[] point = points.get(i, 0);
			Coord centre = new Coord((int) Math.round(point[0]), (int) Math.round(point[1]));
			ProcessImage.drawGreenRectangle(copy, centre, 4, 4);
		}
		
		return copy;
	}
	
}
