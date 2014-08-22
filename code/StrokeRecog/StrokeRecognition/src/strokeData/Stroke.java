package strokeData;

import java.util.ArrayList;
import java.util.List;

/**
 * Class which defines a pen stroke.  
 * A stroke is an ordered collection of sub-strokes.  The sub-strokes are ordered chronologically.  All
 * sub-strokes within the collection should have the same penDown value.
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-08-20
 */
public class Stroke {

	private List<SubStroke> points;	//the collection of sub-strokes that define the stroke.
	
	/**
	 * Constructor for Stroke objects.
	 * Initialises the list of sub-strokes to an empty list.
	 */
	public Stroke() {
		this.points = new ArrayList<SubStroke>();
	}
	
	/**
	 * Constructor for Stroke objects.
	 * Creates a Stroke from an existing list of sub-strokes.
	 * 
	 * @param points - the list of sub-strokes that defines the stroke.
	 */
	public Stroke(List<SubStroke> points) {
		this.points = points;
	}
	
	/**
	 * Method to add a list of sub-strokes to the end of the existing list of sub-storkes that defines the
	 * Stroke.
	 * 
	 * @param newPoints - the new sub-strokes to add to the Stroke.
	 */
	public void addPoints(List<SubStroke> newPoints) {
		points.addAll(newPoints);
	}
	
	/**
	 * Method to swap the penDown status to its opposite value for all the sub-strokes within the Stroke.
	 */
	public void changePenState() {
		for(int i=0; i<points.size(); i++) {
			points.get(i).setPenDown(!points.get(i).isPenDown());
		}
	}
	
	/**
	 * getter for points.
	 * 
	 * @return points - the ordered collection of sub-strokes that define the stroke.
	 */
	public List<SubStroke> getPoints() {
		return points;
	}
	
	/**
	 * setter for points.
	 * 
	 * @param points - the new list of sub-strokes to define the Stroke.
	 */
	public void setPoints(List<SubStroke> points) {
		this.points = points;
	}
	
}
