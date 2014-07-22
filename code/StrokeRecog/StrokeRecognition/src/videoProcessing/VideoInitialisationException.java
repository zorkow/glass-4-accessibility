package videoProcessing;

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
