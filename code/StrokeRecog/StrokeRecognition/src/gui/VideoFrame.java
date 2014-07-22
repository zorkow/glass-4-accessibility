package gui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Observable;
import java.util.Observer;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import videoProcessing.*;

/**
 * Class to provide the GUI for the Stroke Recognition system.
 * 
 * The GUI follows the Model-View pattern (using the Observer-Observable setup) with the Model provided by
 * an object of type ProcessVideo. (ProcessVideo is an abstract class with various sub-class concrete
 * implementations for different video input methods e.g. webcam, video file, or collection of jpeg frames).
 * 
 * The GUI is basic and primarily consists of 4 JLabels, 3 of which are constantly updated by the input 
 * video.  The views are:
 * 	- 	inputView - just the image as input from the video (with the tracked position of the pen)
 * 	-	processedView - the image processed in some way (e.g. thresholding) to aid the object tracking.
 * 	- 	roiView - showing the region of interest around the tracked object.
 * 	- 	templateView - showing the template that is being tracked (image stays constant).
 * 
 * The dimensions of the GUI are provided externally to allow flexibility in setup.
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-07-16
 */
public class VideoFrame extends JFrame implements Observer {

	private static final long serialVersionUID = 1L;
	private JLabel inputView;
	private JLabel processedView;
	private Dimension mainDims;		//dimensions of the two main JLabels (inputView and processedView)
	private JLabel roiView;
	private JLabel templateView;
	private Dimension subDims;		//dimensions of the two smaller JLabels (roiView and templateView)
	private ProcessVideo input;		//the input video processing object (this is the Model in the Model-View pattern)
	

	/**
	 * Constructor for the VideoFrame.
	 * Creates the basic GUI.
	 * 
	 * @param mainDims - the dimensions of the two main input video views.
	 * @param subDims - the dimensions of the smaller region of interest view and template view.
	 */
	public VideoFrame(Dimension mainDims, Dimension subDims) {
		super("Stroke Recognition");
		this.mainDims = mainDims;
		this.subDims = subDims;
		setupFrame();	
	}
	
	/**
	 * Defines the layout of the frame, sets the dimensions of the various views and adds the components to 
	 * the frame.
	 */
	private void setupFrame() {
		
		setLayout(new FlowLayout());
		
		inputView = new JLabel();
		inputView.setSize(mainDims);
		processedView = new JLabel();
		processedView.setSize(mainDims);
		roiView = new JLabel();
		roiView.setSize(subDims);
		templateView = new JLabel();
		templateView.setSize(subDims);
		
		add(inputView);
		add(processedView);
		add(roiView);
		add(templateView);
		
	}
	
	/**
	 * Method to start processing the input video, with input from webcam and user-defined template.
	 * 
	 * @param webcamNum	- integer to define which webcam to use.  0 is the default webcam.
	 * @param templateFile - the full file path of the image to use as a template.
	 */
	public void startVideo(int webcamNum, String templateFile) {
		input = new ProcessWebcam(webcamNum, templateFile);
		process();
	}
	
	/**
	 * Method to start processing the input video, with input from video file and user-defined template.
	 * 
	 * @param videoFile - the full file path of the video file to use as input.
	 * @param templateFile - the full file path of the image to use as a template.
	 */
	public void startVideo(String videoFile, String templateFile) {
		input = new ProcessFile(videoFile, templateFile);
		process();
	}
	
	/**
	 * Method to start processing the input video, with input from collection of video frame and 
	 * user-defined template.
	 * 
	 * @param videoFile - the file path of the jpg frames up to the frame number (e.g. "C:\\video\\frame_")
	 * @param lastFrame - the integer number of the last frame in the sequence.
	 * @param templateFile - the full file path of the image to use as a template.
	 */
	public void startVideo(String videoFile, int lastFrame, String templateFile) {
		input = new ProcessJpgs(videoFile, lastFrame, templateFile);
		process();
	}
	
	/**
	 * Method to start processing the input video, with input from webcam and automatic template extraction.
	 * (NOTE: AUTOMATIC TEMPLATE EXTRACTION NOT IMPLEMENTED YET)
	 * 
	 * @param webcamNum - integer to define which webcam to use.  0 is the default webcam.
	 */
	public void startVideo(int webcamNum) {
		input = new ProcessWebcam(webcamNum);
		process();
	}
	
	/**
	 * Method to start processing the input video, with input from video file and automatic template 
	 * extraction.
	 * (NOTE: AUTOMATIC TEMPLATE EXTRACTION NOT IMPLEMENTED YET)
	 * 
	 * @param videoFile - the full file path of the video file to use as input.
	 */
	public void startVideo(String videoFile) {
		input = new ProcessFile(videoFile);
		process();
	}
	
	/**
	 * Method to start processing the input video, with input from collection of video frame and 
	 * automatic template extraction.
	 * (NOTE: AUTOMATIC TEMPLATE EXTRACTION NOT IMPLEMENTED YET)
	 * 
	 * @param videoFile - the file path of the jpg frames up to the frame number (e.g. "C:\\video\\frame_")
	 * @param lastFrame - the integer number of the last frame in the sequence.
	 */
	public void startVideo(String videoFile, int lastFrame) {
		input = new ProcessJpgs(videoFile, lastFrame);
		process();
	}
	
	/**
	 * Setup the Model-View connection and start processing the video.
	 */
	private void process() {
		input.addObserver(this);
		input.startProcessing();
	}
	
	/**
	 * Implementation of the Observer interface method.
	 * Updates the 3 variable views with the next images from the ProcessVideo object.
	 */
	@Override
	public void update(Observable obs, Object obj) {
		
		if(obs instanceof ProcessVideo) {
			ProcessVideo pv = (ProcessVideo) obs;
			
			//Get the updated images from the ProcessVideo object.
			Mat frame = pv.getImg();
			Mat roi = pv.getROI();
			Mat filteredFrame = pv.getFilteredImg();
			
			Mat frameResize = new Mat();
			Mat roiResize = new Mat();
	    	Mat filteredFrameResize = new Mat();
	    	
	    	//Resize the images to fit the GUI dimensions.
	    	Imgproc.resize(frame, frameResize, new Size(mainDims.getWidth(), mainDims.getHeight()));
	    	Imgproc.resize(roi, roiResize, new Size(subDims.getWidth(), subDims.getHeight()));
	    	Imgproc.resize(filteredFrame, filteredFrameResize, new Size(mainDims.getWidth(), mainDims.getHeight()));
	    	
	    	//Convert each image to a Buffered image.
	    	BufferedImage biFrame = matToBuffImg(frameResize);
	    	BufferedImage biROI = matToBuffImg(roiResize);
	    	BufferedImage biFilteredFrame = matToBuffImg(filteredFrameResize);
	    	
	    	//Update the views with the new images.
			inputView.setIcon(new ImageIcon(biFrame));
			roiView.setIcon(new ImageIcon(biROI));
			processedView.setIcon(new ImageIcon(biFilteredFrame));
			
		} else {
			throw new IllegalArgumentException("Unrecognised object trying to update GUI.");
		}
		
	}
	
	/**
	 * Method to convert an OpenCV Mat object to a BufferedImage.
	 * 
	 * @param img - the Mat object to convert.
	 * @return the converted Mat object as a BufferedImage. 
	 */
	public static BufferedImage matToBuffImg(Mat img) {
		
		MatOfByte bytemat = new MatOfByte();
    	Highgui.imencode(".jpg", img, bytemat);
    	byte[] bytes = bytemat.toArray();
    	InputStream in = new ByteArrayInputStream(bytes);
    	
    	BufferedImage output = null;
    	try {
    		output = ImageIO.read(in);
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    	
		return output;
		
	}
	
	/**
	 * Method to change the template view to a new BufferedImage.
	 * 
	 * @param template - the BufferedImage to set the template to.
	 */
	public void setTemplateView(BufferedImage template) {
		templateView.setIcon(new ImageIcon(template));
	}
	
}