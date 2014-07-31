package videoProcessing;

import org.opencv.core.Mat;
import org.opencv.highgui.VideoCapture;

/**
 * Concrete implementation of the ProcessVideo superclass.
 * This implementation allows the input from a webcam.
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-07-30
 */
public class ProcessWebcam extends ProcessVideo {

	private VideoCapture camera;	//the object through which access to the webcam is obtained.
	private int frameNum;
	
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
	}

	@Override
	public Mat getFrame() {
		Mat frame = new Mat();
		camera.read(frame);
		return frame;
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
