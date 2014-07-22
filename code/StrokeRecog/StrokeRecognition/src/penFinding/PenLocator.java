package penFinding;

import strokeData.*;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.imgproc.Imgproc;

/**
 * 
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-07-16
 */
public class PenLocator {
	
	private Mat template;
	
	private static final int MATCH_METHOD = Imgproc.TM_CCORR_NORMED;
	
	public PenLocator(Mat temp) {
		this.template = temp;
	}
	
	public PenLocator() {
		//determine template in some way.
		System.out.println("Automatic template extraction is not yet implemented.");
	}
	
	/**
	 * (Note: amended from the algorithm on the OpenCV template matching tutorial page)
	 * 
	 * @param main
	 * @param template
	 * @return
	 */
	public TempMatchOutput findTemplate(Mat src) {
		
		//check template is smaller than src in both dimensions
		if(src.size().height < template.size().height || src.size().width < template.size().width) {
			System.out.println("Template bigger than src image.");
			//THROW EXCEPTION.
		}
		
		//Match the template and normalise the result
		Mat result = new Mat();
		Imgproc.matchTemplate(src, template, result, MATCH_METHOD);
		Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());

		//Localizing the best match with minMaxLoc
		MinMaxLocResult mmr = Core.minMaxLoc(result);
		Point matchLoc = mmr.maxLoc;

		//For SQDIFF and SQDIFF_NORMED, the best matches are lower values. 
		//For all the other methods, the higher the better
//		if(matchMethod == Imgproc.TM_SQDIFF || matchMethod == Imgproc.TM_SQDIFF ) { 
//			matchLoc = mmr.minLoc;
//		} else {
//			matchLoc = mmr.maxLoc;
//		}
		
		//calculate how well the template is matched.
		Mat match = src.submat((int) matchLoc.y, (int) (matchLoc.y+template.rows()), (int) matchLoc.x, (int) (matchLoc.x+template.cols()));
		long error = calcDiff(match, template);
		
        return new TempMatchOutput(new Coord((int) matchLoc.x, (int) matchLoc.y), error);
        
	}
	
	
	private static long calcDiff(Mat img1, Mat img2) {
		
		if(img1.size().height != img2.size().height || img1.size().width != img2.size().width) {
			System.out.println("Images do not have same dimensions.");
			//THROW EXCEPTION.
		}
		
		long sum=0;
		Mat diff = new Mat();
		Core.absdiff(img1, img2, diff);
		Scalar sumAll = Core.sumElems(diff);
		for(int k=0; k<3; k++) {
			sum += sumAll.val[k];
		}
		
		return sum;
		
	}
	
	
	public Mat getTemplate() {
		return template;
	}
	
	
}
