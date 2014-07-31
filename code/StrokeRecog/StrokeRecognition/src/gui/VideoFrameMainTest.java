package gui;

import java.awt.Dimension;

import javax.swing.JFrame;

import org.opencv.core.Core;
import org.opencv.highgui.Highgui;

/**
 * Class to test the Stroke Recognition system.
 * 
 * The system has been designed to be flexible, allowing the user to select what type of video input they
 * want to use (webcam, video file, or collection of jpg frames).  The system also allows the user to either
 * provide a .jpg template of the pen that is being used, or to use an automatic template-extraction 
 * technique (this automatic technique is not yet implemented!).
 * 
 * The flexibility is provide Class VideoFrame's startVideo() method, which is overloaded to take different 
 * arguments for different input types.  
 * 
 * Notes on different input video types:
 * - 	Webcam: For this input, provide an integer value indicated which webcam to use. 0 is the default 
 * 		webcam.
 * - 	Video file: Provide the full file path including extension of the video to use (.mp4 files definitely
 * 		work, other formats not yet tested).  (Also note you may have to adjust your system's PATH variable 
 * 		to include a path to the location of the ffmpeg.dll file for this method to work).
 * - 	Jpeg frames: If you have a collection of individual frames saved as jpeg files, provide the file path 
 * 		of the frames up to the number of the frame e.g. "C:\\frame_".  Frames should be number 1, 2, 3...etc 
 * 		(not 01, 02, 03, 012).  Also provide the number of the last frame.
 * 
 * 
 * To specify a template to use, amend the file path of the 'templateFile' variable and pass this as an 
 * additional argument to the startVideo() method.
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-07-16
 */
public class VideoFrameMainTest {

	public static void main(String[] args) {
		
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		//input variables (See JavaDoc for this Class for details)
		String templateFile = "C:\\Users\\Simon\\Desktop\\glassVids\\templates\\templateBlue4.jpg";
//		int webcamNum = 0;
		String videoFile = "C:\\Users\\Simon\\Desktop\\glassVids\\Blue-4\\blue-vid12.mp4";
//		String jpgFile = "C:\\Users\\Simon\\Desktop\\frames8\\frame_";
		
		//set up the GUI on the screen as desired.
		VideoFrame vidFrame = new VideoFrame(new Dimension(426, 320), new Dimension(100, 100));
		vidFrame.setTemplateView(VideoFrame.matToBuffImg(Highgui.imread(templateFile)));
		vidFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		vidFrame.setPreferredSize(new Dimension(900, 525));
		vidFrame.pack();
		vidFrame.setLocation(300, 10);
		vidFrame.setVisible(true);
		
		//Start the processing with the desired method:
		//1 - webcam input, user-defined template.
//		vidFrame.startVideo(webcamNum, templateFile);
		
		//2 - video file input, user-defined template.
		vidFrame.startVideo(videoFile, templateFile);
		
		//3 - collection of jpg frames input, user-defined template.
//		vidFrame.startVideo(jpgFile, 179, templateFile);
		
		
		//NOTE: automated template extraction not yet implemented!
		//4 - webcam input, automated template extraction.
//		vidFrame.startVideo(webcamNum);
		
		//5 - video file input, automated template extraction.
//		vidFrame.startVideo(videoFile);
		
		//6 - collection of jpg frames input, automated template extraction.
//		vidFrame.startVideo(jpgFile, 484);
		
		
	}
	
	
}
