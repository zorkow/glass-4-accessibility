package videoProcessing;

/**
 * Exception for when a video input cannot be initialised for some reason.
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-08-20
 */
public class VideoInitialisationException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private String msg;
	
	public VideoInitialisationException(String msg) {
		this.msg = msg;
	}
	
	public String getLocalizedMessage() {
		return "The input video was unable to be initialised." + msg;
	}

}
