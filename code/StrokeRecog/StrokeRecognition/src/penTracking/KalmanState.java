package penTracking;

/**
 * Class to capture a specific Kalman State.
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-07-16
 */
public class KalmanState {
	
	private double[] state;	//the state which is represented by the x and y location and x and y velocities.
	private double[][] p;	//the error covariance.
	
	/**
	 * Constructor for KalmanState.
	 * 
	 * @param state - the 'state' of the object, represented by its x and y location and x and y velocities.
	 * @param p - the error covariance.
	 */
	public KalmanState(double[] state, double[][] p) {
		this.state = state;
		this.p = p;
	}

	
	public double[] getState() {
		return state;
	}

	public void setState(double[] state) {
		this.state = state;
	}

	public double[][] getP() {
		return p;
	}

	public void setP(double[][] p) {
		this.p = p;
	}
	
	
}
