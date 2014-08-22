package imageRegistration;

import strokeData.Coord;

/**
 * Class to define a rectangular area within an image.
 * The rectangle is defined by the location of its top-left coordinate in the image, along with its height 
 * and width.
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-08-20
 */
public class Rect {

	private int width;	//the width of the rectangle.
	private int height;	//the height of the rectangle.
	private Coord location;	//the top-left coordinate of the rectangle within an image.
	
	/**
	 * Constructor for Rect objects.
	 * 
	 * @param width - the width of the rectangle.
	 * @param height - the height of the rectangle.
	 * @param location - the top-left coordinate of the rectangle within an image.
	 */
	public Rect(int width, int height, Coord location) {
		this.width = width;
		this.height = height;
		this.location = location;
	}
	
	/**
	 * Copy Constructor for Rect objects.
	 * 
	 * @param r - the Rect object to copy.
	 */
	public Rect(Rect r) {
		this.width = r.getWidth();
		this.height = r.getHeight();
		this.location = new Coord(r.getLocation().getX(), r.getLocation().getY());
	}

	/**
	 * Method to trim all sides of a Rect by the provided dimension delta.  
	 * The Rect object's height and width will both be reduced by 2*delta.
	 * 
	 * @param delta - the trim dimension.
	 */
	public void trimRect(int delta) {
		
		setLocation(new Coord(getLocation().getX()+delta, getLocation().getY()+delta));
		setHeight(getHeight()-delta*2);
		setWidth(getWidth()-delta*2);
		
	}
	
	/**
	 * Method to calculate the area of the rectangle.
	 * 
	 * @return the area of the rectangle.
	 */
	public int getArea() {
		return height*width;
	}
	
	/**
	 * getter for width.
	 * 
	 * @return width - the width of the rectangle.
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * setter for width.
	 * 
	 * @param width - the width to use for the rectangle.
	 */
	public void setWidth(int width) {
		this.width = width;
	}

	/**
	 * getter for height.
	 * 
	 * @return height - the height of the rectangle.
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * setter for height.
	 * 
	 * @param height - the height to use for the rectangle.
	 */
	public void setHeight(int height) {
		this.height = height;
	}

	/**
	 * getter for location.
	 * 
	 * @return the coordinates of the top-left corner of the rectangle.
	 */
	public Coord getLocation() {
		return location;
	}

	/**
	 * setter for location.
	 * 
	 * @param location - the coordinates to set the top-left corner of the rectangle to.
	 */
	public void setLocation(Coord location) {
		this.location = location;
	}
	
}
