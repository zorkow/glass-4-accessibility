package strokeData;

/**
 * Class to capture the result of a template-matching operation.  The result is captured by the coordinates
 * of the location within the source image that best matches the template image, along with the fitness of 
 * the match at that location.  
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-07-30
 */
public class TempMatchOutput {

	private Coord bestMatch;	//the location of the best match of the template in the source image
	private double fitness;		//how well the template matches at the bestMatch location.
	
	/**
	 * Constructor for TempMatchOutputs.
	 * Sets bestMatch and error to the passed values.
	 * 
	 * @param bestMatch - the coordinates of the best match of the template in the source image.
	 * @param error - the error of the template match at the bestMatch location.
	 */
	public TempMatchOutput(Coord bestMatch, double fitness) {
		this.bestMatch = bestMatch;
		this.fitness = fitness;
	}
	
	/**
	 * getter for bestMatch.
	 * @return the coordinates representing the location of the best match of the template within the source 
	 * image.
	 */
	public Coord getBestMatch() {
		return bestMatch;
	}
	
	/**
	 * setter for bestMatch.
	 * @param bestMatch - the coordinates of the best match of the template in the source image.
	 */
	public void setBestMatch(Coord bestMatch) {
		this.bestMatch = bestMatch;
	}
	
	/**
	 * getter for error.
	 * @return error - the error of the template match at the bestMatch location.
	 */
	public double getFitness() {
		return fitness;
	}
	
	/**
	 * setter for error.
	 * @param error - the error of the template match at the bestMatch location.
	 */
	public void setFitness(double fitness) {
		this.fitness = fitness;
	}
	
}
