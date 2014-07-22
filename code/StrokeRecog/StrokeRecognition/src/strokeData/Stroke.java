package strokeData;

/**
 * Class to represent a pen stroke.  The stroke is defined by the location of the pen ballpoint and whether 
 * the pen is writing (i.e. pen-down) or not writing  (pen-up).
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-07-16
 */
public class Stroke {

	private Coord location;	//the location of the pen ballpoint.
	private boolean penDown;	//whether the pen writing (pen-down) or not (pen-up)
	
	/**
	 * Constructor for Stroke objects.
	 * Sets the location and penDown boolean to the passed values.
	 * 
	 * @param location - the coordinates of the pen ballpoint.
	 * @param penDown - whether the pen is writing or not (pen-down or pen-up).
	 */
	public Stroke(Coord location, boolean penDown) {
		this.location = location;
		this.penDown = penDown;
	}
	
	/**
	 * getter for location.
	 * @return the ballpoint location for this Stroke.
	 */
	public Coord getLocation() {
		return location;
	}

	/**
	 * setter for location.
	 * @param location - the coordinates to set the location to.
	 */
	public void setLocation(Coord location) {
		this.location = location;
	}

	/**
	 * getter for penDown.
	 * @return whether the pen is writing or not.
	 */
	public boolean isPenDown() {
		return penDown;
	}

	/**
	 * setter for penDown.
	 * @param penDown - whether the pen is writing or not.
	 */
	public void setPenDown(boolean penDown) {
		this.penDown = penDown;
	}
	
	
}
