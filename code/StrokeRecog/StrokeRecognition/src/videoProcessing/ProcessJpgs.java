package videoProcessing;

import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;

/**
 * Concrete implementation of the ProcessVideo superclass.
 * This implementation allows the input to be a collection of Jpeg images saved in the same format with 
 * sequentially numbered frames.  (Frames should be numbered 1, 2, 3, etc. not 01, 02, 045 etc.)
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-07-16
 */
public class ProcessJpgs extends ProcessVideo {

	private int frameNum;	//the current frame number.
	private int lastFrame;	//the final frame number.
	private String videoFile;	//the file path up to but not including the number of the frame  
								//(e.g. "C:\\video\\frame_")
	
	/**
	 * Constructor for ProcessJpgs objects with automatic template extraction.
	 * 
	 * @param videoFile - the full file path including extension of the video file to use as input.
	 * @param lastFrame - the number of the final frame in the sequence of jpgs.
	 */
	public ProcessJpgs(String videoFile, int lastFrame) {
		super();
		setupVideoFile(videoFile, lastFrame);
	}
	
	/**
	 * Constructor for ProcessJpgs objects with a user-defined template.
	 * 
	 * @param videoFile - the full file path including extension of the video file to use as input.
	 * @param lastFrame - the number of the final frame in the sequence of jpgs.
	 * @param template - the full file path including extension of the image file to use as template.
	 */
	public ProcessJpgs(String videoFile, int lastFrame, String template) {
		super(template);
		setupVideoFile(videoFile, lastFrame);
	}
	
	/**
	 * Method to initialise the field variables.
	 * 
	 * @param videoFile - the full file path including extension of the video file to use as input.
	 * @param lastFrame - the number of the final frame in the sequence of jpgs.
	 */
	private void setupVideoFile(String videoFile, int lastFrame) {
		this.videoFile = videoFile;
		frameNum = 1;
		this.lastFrame = lastFrame;
	}

	@Override
	public Mat getFrame() {
		Mat frame = Highgui.imread(videoFile + frameNum + ".jpg");
		frameNum++;
		return frame;
	}

	@Override
	public boolean frameAvailable() {
		return (frameNum<=lastFrame);
	}
	
	@Override
	public int getFrameNum() {
		return frameNum;
	}
	
}
