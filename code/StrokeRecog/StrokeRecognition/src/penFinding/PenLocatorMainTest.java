package penFinding;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import strokeData.TempMatchOutput;
import videoProcessing.ProcessImage;

/**
 * A short test class for the PenLocator class.  Checks that the template matching is working as expected.
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-07-16
 */
public class PenLocatorMainTest {
	
	public static void main(String[] args) {
		
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		Mat template = Highgui.imread("C:\\Users\\Simon\\Desktop\\frames3\\template.jpg");
		template = ProcessImage.filterColour(template, ProcessImage.BLACK_LOW_HSV, ProcessImage.BLACK_HIGH_HSV);
		
		Mat src = Highgui.imread("C:\\Users\\Simon\\Desktop\\frames3\\frame_111.jpg");
		src = ProcessImage.filterColour(src, ProcessImage.BLACK_LOW_HSV, ProcessImage.BLACK_HIGH_HSV);
		
		PenLocator pl = new PenLocator(template);
		TempMatchOutput out = pl.findTemplate(src);
		System.out.println("X = " + out.getBestMatch().getX() + ", Y = " + out.getBestMatch().getY());
		System.out.println("Error = " + out.getError());
		
		Imgproc.cvtColor(src, src, Imgproc.COLOR_GRAY2BGR);
		ProcessImage.drawGreenRect(src, new Point(out.getBestMatch().getX(), 
				out.getBestMatch().getY()), pl.getTemplate().cols(), pl.getTemplate().rows());
		Highgui.imwrite("C:\\Users\\Simon\\Desktop\\frames3\\test.jpg", src);
		
	}

}
