package penFinding;

import strokeData.*;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.imgproc.Imgproc;

/**
 * Class to facilitate finding of a pen tip template within a larger image.
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-08-20
 */
public class PenLocator {
	
	private Mat template;	//the template to find.
	
	private int matchMethod;	//the method to use in the template-matching
									
	//the number of levels to use in the image pyramid version of the template matching method.
	private final int PYRAMID_LEVELS = 3;	
	
	//the distance either side of the best match location to look in higher levels of the pyramid (i.e. 
	//the search area is (2*pyramidDelta)*(2*pyramidDelta)).
	private final int PYRAMID_DELTA = 75;	
	
	/**
	 * Constructor for PenLocator objects with user-defined template. 
	 * Initialises the template to the passed Mat.
	 * 
	 * @param temp - the template to use in the matching.
	 */
	public PenLocator(Mat temp) {
		this.template = temp;
		this.matchMethod = Imgproc.TM_CCORR_NORMED; //TM_CCORR_NORMED = Normalised Cross-correlation
	}
	
	/**
	 * Constructor for PenLocator objects with automatic template extraction.
	 * (Automatic template extraction NOT YET IMPLEMENTED.)
	 */
	public PenLocator() {
		this.matchMethod = Imgproc.TM_CCORR_NORMED; //TM_CCORR_NORMED = Normalised Cross-correlation
		//determine template in some way.
		System.out.println("Automatic template extraction is not yet implemented.");
	}
	
	/**
	 * Method to find the best matching location of the template within the source image.  The method simply
	 * moves the template through the source image and calculates the 'fitness' of the match at each location.
	 * The location with the highest fitness is returned along with the fitness value.
	 * 
	 * (Note: this algorithm is amended from the one on the OpenCV template-matching tutorial page)
	 * 
	 * @param src - the image in which to search for the template.
	 * @return A TempMatchOutput object containing the coordinates of the best match location and the value
	 * of the fitness of the match.
	 */
	public TempMatchOutput findTemplateSimple(Mat src) {
		
		//check template is smaller than src in both dimensions
		if(src.size().height < template.size().height || src.size().width < template.size().width) {
			throw new IllegalArgumentException("The source image is smaller than the template.");
		}
		
		//Match the template.
		Mat result = new Mat();
		Imgproc.matchTemplate(src, template, result, matchMethod);
//		Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());	//do not normalise as want absolute match-fitness result.

		//Localising the best match with minMaxLoc
		MinMaxLocResult mmr = Core.minMaxLoc(result);
		TempMatchOutput out = null;
		
		//For SQDIFF and SQDIFF_NORMED, the best matches are lower values. 
		//For all the other methods, the higher the better
		if(matchMethod == Imgproc.TM_SQDIFF || matchMethod == Imgproc.TM_SQDIFF ) { 
			out = new TempMatchOutput(new Coord((int) mmr.minLoc.x, (int) mmr.minLoc.y), mmr.minVal);
		} else {
			out = new TempMatchOutput(new Coord((int) mmr.maxLoc.x, (int) mmr.maxLoc.y), mmr.maxVal);
		}
		
        return out;
        
	}
	
	/**
	 * Method to find the best matching location of the template within the source image.  As opposed to the 
	 * 'simple' method, this method creates an image pyramid in order to speed up the template matching in 
	 * larger src images.  The src and template are compressed to smaller images where it is quicker to find
	 * the template, then only a small region around the best match location is searched in the higher levels
	 * of the pyramid.
	 * 
	 * @param src - the image in which to search for the template.
	 * @return A TempMatchOutput object containing the coordinates of the best match location and the value
	 * of the fitness of the match.
	 */
	public TempMatchOutput findTemplatePyramid(Mat src) {
		
		//check template is smaller than src in both dimensions
		if(src.size().height < template.size().height || src.size().width < template.size().width) {
			throw new IllegalArgumentException("The source image is smaller than the template.");
		}
		
		Mat[] srcPyramid = getImagePyramid(src, PYRAMID_LEVELS);
		Mat[] tempPyramid = getImagePyramid(template, PYRAMID_LEVELS);
		
		int rowStart = 0; int rowEnd = 0; int colStart = 0; int colEnd = 0;
		Coord minUL = new Coord(0,0);
		double fitness = 0;
		
		//work through the pyramid levels from coarse image to fine image.
		for(int i=PYRAMID_LEVELS-1; i>=0; i--) {
			
			//Match the template
			Mat result = new Mat();
			Imgproc.matchTemplate(srcPyramid[i], tempPyramid[i], result, matchMethod);
//			Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());	//do not normalise as want absolute match-fitness result.

			//Localizing the best match with minMaxLoc
			MinMaxLocResult mmr = Core.minMaxLoc(result);
			Point matchLoc = new Point();
			
			//For SQDIFF and SQDIFF_NORMED, the best matches are lower values. 
			//For all the other methods, the higher the better
			if(matchMethod == Imgproc.TM_SQDIFF || matchMethod == Imgproc.TM_SQDIFF ) { 
				matchLoc = new Point(mmr.minLoc.x, mmr.minLoc.y);
				fitness = mmr.minVal;
			} else {
				matchLoc = new Point(mmr.maxLoc.x, mmr.maxLoc.y);
				fitness = mmr.maxVal;
			}
			
			//minUL gives the location of the best matching location in the next level up.
			minUL = new Coord((int) (2*(matchLoc.x+colStart)), (int) (2*(matchLoc.y+rowStart)));	
			
			if(i>0) {
				//define the zone in which to search for the template in the next level up.
				colStart = (minUL.getX()-PYRAMID_DELTA>0) ? minUL.getX()-PYRAMID_DELTA : 0;
				colEnd = (minUL.getX()+PYRAMID_DELTA+tempPyramid[i-1].cols()<srcPyramid[i-1].cols()) ? minUL.getX()+PYRAMID_DELTA+tempPyramid[i-1].cols() : srcPyramid[i-1].cols();
				rowStart = (minUL.getY()-PYRAMID_DELTA>0) ? minUL.getY()-PYRAMID_DELTA : 0;
				rowEnd = (minUL.getY()+PYRAMID_DELTA+tempPyramid[i-1].rows()<srcPyramid[i-1].rows()) ? minUL.getY()+PYRAMID_DELTA+tempPyramid[i-1].rows() : srcPyramid[i-1].rows();
				
				srcPyramid[i-1] = srcPyramid[i-1].submat(rowStart, rowEnd, colStart, colEnd).clone(); 
			}
			
		}
		
		Coord ans = new Coord(minUL.getX()/2, minUL.getY()/2);	//the location of the best match in the src image.
		
        return new TempMatchOutput(ans, fitness);
        
	}
	
	/**
	 * Method to create an array of Mats which form an image pyramid of the original image.  Each level down
	 * the pyramid contains an image half the resolution of the level above.
	 * 
	 * @param original - the image to create the image pyramid from.
	 * @param layers - the number of levels to have in the pyramid. The first level (0) will be the original 
	 * image.
	 * @return The array of Mat objects representing the image pyramid.
	 */
	public Mat[] getImagePyramid(Mat original, int layers) {
		
		//initialise each Mat in the array to the right size.
		Mat[] pyramid = new Mat[layers];
		for(int i=0; i<layers; i++) {
			pyramid[i] = new Mat();
		}
		//first copy original image into layer 0;
		original.copyTo(pyramid[0]);
		
		//next create each layer at half resolution of previous layer
		for(int i=1; i<layers; i++) {
			Imgproc.pyrDown(pyramid[i-1], pyramid[i]);
		}
		
		return pyramid;
	}

	
	/**
	 * getter for template.
	 * 
	 * @return the template used as a Mat.
	 */
	public Mat getTemplate() {
		return template;
	}
	
	
//	Method no longer used.
//	private static long calcDiff(Mat img1, Mat img2) {
//		
//		if(img1.size().height != img2.size().height || img1.size().width != img2.size().width) {
//			System.out.println("Images do not have same dimensions.");
//			//THROW EXCEPTION.
//		}
//		
//		long sum=0;
//		Mat diff = new Mat();
//		Core.absdiff(img1, img2, diff);
//		Scalar sumAll = Core.sumElems(diff);
//		for(int k=0; k<3; k++) {
//			sum += sumAll.val[k];
//		}
//		
//		return sum;
//		
//	}
	
}
