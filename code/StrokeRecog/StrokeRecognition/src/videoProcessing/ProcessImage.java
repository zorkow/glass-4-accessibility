package videoProcessing;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import strokeData.Coord;

/**
 * Class containing a collection of static methods for processing images stored as OpenCV Mat objects.
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-07-30
 */
public class ProcessImage {
	
	//constants for the lower and upper values for the colour black in the HSV space:
	public static final Scalar BLACK_LOW_HSV = new Scalar(0,0,0);
	public static final Scalar BLACK_HIGH_HSV = new Scalar(255,75,50);
	
	/**
	 * Method to process an image for the template-matching operation.  The method sharpens the image and
	 * removes background noise.
	 * 
	 * @param src - input image.
	 * @return a copy of the src image which has been processed for use in the template-matching operation.
	 */
	public static Mat filterImage(Mat src) {
		Mat stripped = new Mat();
		src.copyTo(stripped);
		stripped = ProcessImage.sharpenWithGaussianBlur(src, 2, 0.5);
		return stripped;
	}
	
	/**
	 * Method to filter out colours from a src image using the defined ranges.  Pixels falling within the 
	 * range are returned as 1, all other pixels are 0.  
	 * 
	 * @param src - input image in RGB format.
	 * @param low - Scalar of HSV values representing the low threshold.
	 * @param high - Scalar of HSV values representing the high threshold.
	 * @return a copy of the src image converted to HSV and filtered through the range defined by low and 
	 * high. Pixels that fall within the range are returned as 1, all other pixels are 0.
	 */
	public static Mat filterColour(Mat src, Scalar low, Scalar high) {
		Mat stripped = new Mat();
		src.copyTo(stripped);
		Core.inRange(stripped, low, high, stripped);
		stripped = dilate(stripped, 9);
		return stripped;
	}
	
	/**
	 * Method to apply an adaptive threshold based on  a weighted sum (cross-correlation with a Gaussian 
	 * window) of the blockSize x blockSize neighbourhood of (x, y) minus constant.
	 * The image is blurred prior to thresholding with a kernel if dimension blurSize.
	 * 
	 * @param src - the image to apply the adaptive threshold to.
	 * @param blurSize - the dimension of the kernel to use in the blurring operation.
	 * @param blockSize - the size of the block in the threshold operation.
	 * @param constant - the constant to adjust the threshold by.
	 * @return the src image with the adaptive threshold applied.
	 */
	public static Mat adaptiveThreshold(Mat src, int blurSize, int blockSize, double constant) {
		
		Mat dst = ProcessImage.blur(src, blurSize);
		Imgproc.cvtColor(dst, dst, Imgproc.COLOR_BGR2GRAY);
		Imgproc.adaptiveThreshold(dst, dst, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, 
				Imgproc.THRESH_BINARY, blockSize, constant);
		
		return dst;
	}
	
	
	/**
	 * Method to draw a green rectangle on the input image at the specified coordinates (topLeft) with the 
	 * specified dimensions (dx by dy).
	 * 
	 * @param src - the image to draw the rectangle on.
	 * @param topLeft - the coordinates of the top left corner of the rectangle.
	 * @param dx - the width of the rectangle.
	 * @param dy - the height of the rectangle.
	 */
	public static void drawGreenRect(Mat src, Point topLeft, int dx, int dy) {
		Core.rectangle(src, topLeft, new Point(topLeft.x + dx, topLeft.y + dy), new Scalar(0, 255, 0));
	}
	
	
	/**
	 * Method to draw a green rectangle on the input image at the specified coordinates (centre) with the 
	 * specified dimensions (2*halfWidth by 2*halfHeight).
	 * 
	 * @param src - the image to draw the rectangle on.
	 * @param centre - the coordinates of the centre of the rectangle.
	 * @param halfWidth - half the width of the rectangle.
	 * @param halfHeight - half the height of the rectangle.
	 */
	public static void drawGreenRectangle(Mat src, Coord centre, int halfWidth, int halfHeight) {	
		Core.rectangle(src, new Point(centre.getX()-halfWidth,  centre.getY()-halfHeight), 
				new Point(centre.getX()+halfWidth, centre.getY()+halfHeight), new Scalar(0,255,0));
	}
	
	/**
	 * Method to draw a red line on the input image between the specified start and end coordinates.
	 * 
	 * @param src - the image to draw the red line on.
	 * @param start - the coordinates of the start of the line.
	 * @param end - the coordinates of the end of the line.
	 */
	public static void drawRedLine(Mat src, Coord start, Coord end) {
		Point s = new Point(start.getX(), start.getY());
		Point e = new Point(end.getX(), end.getY());
		Core.line(src, s, e, new Scalar(0, 0, 255), 2);
	}
	
	/**
	 * Method to draw a line of a specified colour on the input image between the specified start and 
	 * end coordinates.
	 * 
	 * @param src - the image to draw the line on.
	 * @param start - the coordinates of the start of the line.
	 * @param end - the coordinates of the end of the line.
	 * @param colour - the colour of the line specified as an RGB(?) scalar.
	 */
	public static void drawColouredLine(Mat src, Coord start, Coord end, Scalar colour) {
		Point s = new Point(start.getX(), start.getY());
		Point e = new Point(end.getX(), end.getY());
		Core.line(src, s, e, colour, 3);
	}
	
	/**
	 * Method to normalise an image between values of 0 and 255.
	 * 
	 * @param src - the image to normalise.
	 * @return a copy of the source image that has been normalised between 0 and 255.
	 */
	public static Mat normalise(Mat src) {
		Mat dst = new Mat();
		Core.normalize(src, dst, 0, 255, Core.NORM_MINMAX);
		return dst;
	}
	
	/**
	 * Converts a BGR image to a grayscale image.
	 * 
	 * @param src - the BGR image to convert
	 * @return a copy of the source image that has been converted to grayscale.
	 */
	public static Mat convertToGray(Mat src) {
		Mat dst = new Mat();
		Imgproc.cvtColor(src, dst, Imgproc.COLOR_BGR2GRAY);
		return dst;
	}
	
	/**
	 * Performs a image dilation operation on the source image.
	 * 
	 * @param src - the image to dilate.
	 * @param kSize the dimensions of the kernel to use in the dilation operation.
	 * @return a copy of the source image with the dilation operation applied.
	 */
	public static Mat dilate(Mat src, int kSize) {
		Mat dst = new Mat();
		Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(kSize, kSize));
		Imgproc.dilate(src, dst, element);
		return dst;
	}
	
	/**
	 * Performs a image erode operation on the source image.
	 * 
	 * @param src - the image to erode.
	 * @param kSize the dimensions of the kernel to use in the erode operation.
	 * @return a copy of the source image with the erode operation applied.
	 */
	public static Mat erode(Mat src, int kSize) {
		Mat dst = new Mat();
		Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(kSize, kSize));
		Imgproc.erode(src, dst, element);
		return dst;
	}

	/**
	 * Applies a blur to the source image.
	 * 
	 * @param src - the image to blur.
	 * @param ksize - the dimensions of the kernel to use.
	 * @return a copy of the source image with the blur applied.
	 */
	public static Mat blur(Mat src, int ksize) {
		Mat dst = new Mat();
		Imgproc.blur(src, dst, new Size(ksize,ksize));
		return dst;
	}
	
	/**
	 * Method to carry out Canny edge detection on a source image.
	 * 
	 * @param src - the image to apply the edge detection to.
	 * @param highThresh - used to find initial strong edge segments.
	 * @param lowThresh - used to determine how to link edges.
	 * @return the output edge map as a Mat (same size and type as src)
	 */
	public static Mat cannyEdge(Mat src, int highThresh, int lowThresh) {
		Mat dst = new Mat();
//		double highThresh = Imgproc.threshold(src, new Mat(), 250, 255, Imgproc.THRESH_BINARY);
//		double lowThresh = highThresh/2;
		Imgproc.Canny(src, dst, lowThresh, highThresh);
		return dst;
	}
	
	/**
	 * Uses the output of a Hough line transform, lines, to draw all the lines on a source image.
	 * The lines are drawn in varying shades of red.
	 * 
	 * @param src - the image to draw the lines on.
	 * @param lines - the Hough transform lines Mat.
	 */
	public static void drawAllLines(Mat src, Mat lines) {
		
		int num = (int) lines.size().width;
		for(int i=0; i<num; i++) {
			double[] out = lines.get(0, i);
			double rho = out[0];
			double theta = out[1];
			int val = (int) (i*255.0)/num;
			println(rho, theta, src, val);
		}
		
	}
	
	/**
	 * Uses the output of the Hough line transform, lines. Draws all the lines in the specified 'lines'
	 * Mat which have an angle with another line between the specified angle 1 and angle 2.  
	 * (For example, if two lines are provided which are at angles 50 and 70 degrees, and angle1 and 
	 * angle2 are set to 10degrees and 30degrees respectively, the two lines will both be drawn.)
	 * 
	 * @param src - the image to draw the lines on.
	 * @param lines - the Hough transform lines Mat.
	 * @param angle1 - the lower angle threshold.
	 * @param angle2 - the upper angle threshold.
	 */
	public static void drawLinesBetweenAngles(Mat src, Mat lines, double angle1, double angle2) {
		
		int num = (int) lines.size().width;
		for(int i=0; i<num; i++) {
			double[] firstLine = lines.get(0, i);
			for(int j=i+1; j<num; j++) {
				double[] secondLine = lines.get(0,  j);
				double angleBetween = Math.abs((firstLine[1] - secondLine[1]) * 180/Math.PI);
				System.out.println("Angle: " + angleBetween);
				if(angleBetween > angle1 && angleBetween < angle2) {
					println(firstLine[0], firstLine[1], src, 0);
					println(secondLine[0], secondLine[1], src, 0);
				}
			}	
		}
		
	}

	
	/**
	 * Method to print a line on a Mat.  The line is defined by rho and theta.  The line is a shade of 
	 * red determined by val (0 - full red, 255 - white).
	 * 
	 * @param rho - the polar line parameter rho.
	 * @param theta - the polar line parameter theta.
	 * @param out - the Mat on which to draw the line.
	 * @param val - used to adjust the shade of red drawn by the line (0 - full red, 255 - white).
	 */
	private static void println(double rho, double theta, Mat out, int val) {
		double a = Math.cos(theta);
		double b = Math.sin(theta);
		double x0 = a*rho;
		double y0 = b*rho;
		Point p1 = new Point(x0+1000*(-b), y0+1000*(a));
		Point p2 = new Point(x0-1000*(-b), y0-1000*(a));
		Core.line(out, p1, p2, new Scalar(val,val,255));
		
//		Maybe think about doing it this way:
//		x= -10:10;
//		y = (rho - x* cos(theta) )/ sin(theta);
	}
	
	/**
	 * Performs a 2D filter on the source image using the specified kernel.
	 * 
	 * @param src - the image to filter.
	 * @param kernel - the kernel to use in the filtering process.
	 * @return a copy of the src image with the filter applied.
	 */
	public static Mat convolveImage(Mat src, Mat kernel) {
		
		Mat dst = new Mat();
		Imgproc.filter2D(src, dst, -1, kernel);
		return dst;
		
	}
	
	/**
	 * Method to create a 3x3 kernel that can be used in conjunction with a 2D filter (ref convolveImage() )
	 * to sharpen an image.
	 * 
	 * @return the 3x3 kernel for image sharpening.
	 */
	public static Mat getSharpenKernel() {
		
		Mat kernel = Mat.zeros(new Size(3,3), 1);
		double[] val1 = {-1.0};
		double[] val2 = {5.0};
		kernel.put(0, 1, val1);
		kernel.put(1, 0, val1);
		kernel.put(2, 1, val1);
		kernel.put(1, 2, val1);
		kernel.put(1, 1, val2);
		
		return kernel;
	}
		
	/**
	 * Method that attempts to sharpen an image using the Gaussian Blur technique.  The method blurs the 
	 * source image and adds this to the source image using the specified weights.
	 * 
	 * @param src - the source image to sharpen.
	 * @param alpha - weighting for the source image.
	 * @param beta - weighting for the Gaussian blurred image.
	 * @return a copy of the source image with the sharpening applied.
	 */
	public static Mat sharpenWithGaussianBlur(Mat src, double alpha, double beta) {
		
		Mat gaussBlur = new Mat(); 
		Mat dst = new Mat();
		Imgproc.GaussianBlur(src, gaussBlur, new Size(0, 0), 5);
		Core.addWeighted(src, alpha, gaussBlur, beta, 0, dst);
		
		return dst;
	}
	
	
	/**
	 * Method which processes the source image using an adaptive threshold, dilating and eroding operations
	 * to extract only the ink trace as a binary image.  The source should contain only background and text.
	 * 
	 * @param src - image to find the ink trace in. Should only contain background and text.
	 * @param dilateVal - the dimensions of the kernel to use in the dilate operation. 
	 * @param erodeVal - the dimensions of the kernel to use in the erode operation.
	 * @return a binary image containing only the ink trace.
	 */
	public static Mat extractInkTrace(Mat src, int dilateVal, int erodeVal) {
		Mat thresh = ProcessImage.adaptiveThreshold(src, 3, 3, 2);
		thresh = ProcessImage.dilate(thresh, dilateVal);
		thresh = ProcessImage.erode(thresh, erodeVal);
		return thresh;
	}

	
}
