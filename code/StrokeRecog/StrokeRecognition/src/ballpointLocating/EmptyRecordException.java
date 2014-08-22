package ballpointLocating;

/**
 * Exception for an attempt is made to access a method requiring a non-empty ballpointRecord or
 * resampledRecord and the relevant field is empty.
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-08-20
 */
public class EmptyRecordException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private String msg;
	
	public EmptyRecordException(String msg) {
		this.msg = msg;
	}
	
	public String getLocalizedMessage() {
		return "Either the ballpointRecord field or resampledRecord field is empty.\n" + msg;
	}

}
