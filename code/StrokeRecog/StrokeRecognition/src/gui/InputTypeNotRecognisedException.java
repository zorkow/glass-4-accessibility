package gui;

public class InputTypeNotRecognisedException extends IllegalArgumentException {

	private static final long serialVersionUID = 1L;
	private int inputType;
	
	public InputTypeNotRecognisedException(int inputType) {
		super();
		this.inputType = inputType;
	}
	
	public String getLocalizedMessage() {
		return "The input type " + inputType + " is not known. (0 - Video file, 1 - Webcam)";
	}

}
