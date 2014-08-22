package strokeData;

/**
 * A class which represents a standard 2D coordinate.
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-08-20
 */
public class Coord {

	private int x;	//the x coordinate
	private int y;	//the y coordinate
	
	/**
	 * Constructor for Coords.
	 * Sets x and y to the passed values.
	 * 
	 * @param x - the x coordinate.
	 * @param y - the y coordinate.
	 */
	public Coord(int x, int y) {
		this.x=x;
		this.y=y;
	}

	/**
	 * Method to find the Euclidean distance between this Coord and another Coord.
	 * 
	 * @param other - the other Coord to calculate the Euclidean distance to.
	 * @return the Euclidean distance between this coordinate and the provided other coordinate.
	 */
	public int findDist(Coord other) {
		double dist = Math.sqrt(Math.pow(getX()-other.getX(), 2) + Math.pow(getY()-other.getY(), 2));
		return (int) Math.round(dist);
	}
	
	/**
	 * getter for the x coordinate.
	 * @return the x coordinate.
	 */
	public int getX() {
		return x;
	}

	/**
	 * setter for the x coordinate
	 * @param x - the value to set the x coordinate to.
	 */
	public void setX(int x) {
		this.x = x;
	}

	/**
	 * getter for the y coordinate.
	 * @return the y coordinate.
	 */
	public int getY() {
		return y;
	}

	/**
	 * setter for the y coordinate
	 * @param y - the value to set the y coordinate to.
	 */
	public void setY(int y) {
		this.y = y;
	}
	
	/**
	 * Method to add a specified value to the x coordinate.
	 * 
	 * @param deltaX - the value to add to the x coordinate.
	 */
	public void addToX(int deltaX) {
		this.x += deltaX;
	}
	
	/**
	 * Method to add a specified value to the y coordinate.
	 * 
	 * @param deltaY - the value to add to the y coordinate.
	 */
	public void addToY(int deltaY) {
		this.y += deltaY;
	}

	@Override
	public String toString() {
		return "X = " + getX() + ", Y = " + getY();
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof Coord) {
			Coord c = (Coord) o;
			return (getX()==c.getX() && getY()==c.getY());	
		} else {
			return false;
		}
	}
}
