package videoProcessing;

import org.opencv.core.Mat;
import org.opencv.highgui.VideoCapture;

/**
 * Concrete implementation of the ProcessVideo superclass.
 * This implementation allows the input from a webcam.
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-08-20
 */
public class ProcessWebcam extends ProcessVideo {

	private VideoCapture camera;	//the object through which access to the webcam is obtained.
	private int frameNum;		//the number of the next frame to get.
	private Mat currentFrame;	//a copy of the current frame as a Mat object
	
	/**
	 * Constructor for ProcessWebcam objects with automatic template extraction.
	 * 
	 * @param webcamNum - the integer specifying which webcam to use. 0 is the default webcam.
	 */
	public ProcessWebcam(int webcamNum) {
		super();
		setupCamera(webcamNum);
	}
	
	/**
	 * Constructor for ProcessWebcam objects with a user-defined template.
	 * 
	 * @param webcamNum - the integer specifying which webcam to use. 0 is the default webcam.
	 * @param template - the full file path including extension of the image file to use as template.
	 */
	public ProcessWebcam(int webcamNum, String template) {
		super(template);
		setupCamera(webcamNum);
	}
	
	/**
	 * Method to initialise the camera.
	 * 
	 * @param webcamNum - the integer specifying which webcam to use. 0 is the default webcam.
	 * @throws VideoInitialisationException - if the webcam input cannot be initialised.
	 */
	private void setupCamera(int webcamNum) {
		camera = new VideoCapture(webcamNum);
	    if(!camera.isOpened()){
	        throw new VideoInitialisationException("Specified webcam number could not be found.");
	    }
	    frameNum=1;
	    currentFrame = new Mat();
	}

	@Override
	public Mat getFrame() {
		Mat frame = new Mat();
		camera.read(frame);
		currentFrame = frame;
		return frame;
	}

	@Override
	public Mat getCurrentFrame() {
		return currentFrame;
	}
	
	@Override
	public boolean frameAvailable() {
		return camera.isOpened();
	}

	@Override
	public int getFrameNum() {
		return frameNum;
	}

	@Override
	public void releaseResources() {
		camera.release();
	}
	
	
}
