package videoProcessing;

import org.opencv.core.Mat;
import org.opencv.highgui.VideoCapture;

/**
 * Concrete implementation of the ProcessVideo superclass.
 * This implementation allows the input to be a video file.
 * 
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-07-16
 */
public class ProcessFile extends ProcessVideo {

	private int frameNum;	//the current frame number.
	private int lastFrame;		//the final frame number
	private VideoCapture vid;	//the object through which the video file is accessed.
	
	/**
	 * Constructor for ProcessFile objects with automatic template extraction.
	 * 
	 * @param videoFile - the full file path including extension of the video file to use as input.
	 */
	public ProcessFile(String videoFile) {
		super();
		setupVideoFile(videoFile);
	}
	
	/**
	 * Constructor for ProcessFile objects with a user-defined template.
	 * 
	 * @param videoFile - the full file path including extension of the video file to use as input.
	 * @param template - the full file path including extension of the image file to use as template.
	 */
	public ProcessFile(String videoFile, String template) {
		super(template);
		setupVideoFile(videoFile);
	}
	
	/**
	 * Method to initialise the field variables.
	 * 
	 * @param videoFile - the full file path including extension of the video file to use as input.
	 */
	private void setupVideoFile(String videoFile) {
		vid = new VideoCapture(videoFile);
		if(!vid.isOpened()) {
			throw new VideoInitialisationException("Could not read from specified file.");
		}
		frameNum = 1;
		lastFrame = (int) vid.get(7);	//video property code 7 is the frame count.
	}

	@Override
	public Mat getFrame() {
		Mat frame = new Mat();
		vid.read(frame);
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
