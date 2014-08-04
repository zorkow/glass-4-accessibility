package strokeData;

/**
 * Class to represent a pen sub-stroke.  The sub-stroke is defined by two locations (points) of the pen 
 * ballpoint and whether the pen is writing (i.e. pen-down) or not writing  (pen-up).
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-07-30
 */
public class SubStroke {

	private Coord start;	//the start location of the pen sub-stroke.
	private Coord end;		//the end location of the pen sub-stroke.
	private boolean penDown;	//whether the pen writing (pen-down) or not (pen-up)
	
	/**
	 * Constructor for Stroke objects.
	 * Sets the location and penDown boolean to the passed values.
	 * 
	 * @param location - the coordinates of the pen ballpoint.
	 * @param penDown - whether the pen is writing or not (pen-down or pen-up).
	 */
	public SubStroke(Coord start, Coord end, boolean penDown) {
		this.start = start;
		this.end = end;
		this.penDown = penDown;
	}
	
	/**
	 * getter for start.
	 * @return the start ballpoint location for this sub-stroke.
	 */
	public Coord getStart() {
		return start;
	}
	
	/**
	 * getter for end.
	 * @return the end ballpoint location for this sub-stroke.
	 */
	public Coord getEnd() {
		return end;
	}

	/**
	 * setter for start.
	 * @param start - the coordinates to set the start location to.
	 */
	public void setStart(Coord start) {
		this.start = start;
	}
	
	/**
	 * setter for end.
	 * @param end - the coordinates to set the end location to.
	 */
	public void setEnd(Coord end) {
		this.end = end;
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
	
	/**
	 * Method which calculates and returns the length of the sub-stroke.
	 * @return the length of the sub-stroke.
	 */
	public double getLength() {
		return Math.sqrt(Math.pow(start.getX()-end.getX(), 2) + Math.pow(start.getY()-end.getY(), 2));
	}
	
	public double getBearing() {
		int deltaX = end.getX() - start.getX();
		int deltaY = end.getY() - start.getY();
		
		double angleInRad = Math.atan2(deltaY, deltaX);
		double angleInDeg = angleInRad * 180 / Math.PI;
		angleInDeg = (angleInDeg*-1) + 90;
		double bearing = (angleInDeg + 360) % 360;
		
		return bearing;
	}
}
