package penTracking;

import strokeData.Coord;

/**
 * Class for implementing a Kalman Filter that can be used to help with object tracking.
 * The filter implemented in the current version assumes a constant acceleration (may be advisable to amend
 * this to take a variable acceleration which better models the movement of a pen).
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-07-16
 */
public class KalmanFilter {
	
	private KalmanState ksPredict;	//the state representing the predicted position of the object.
	private KalmanState ksCorrected;	//the state representing the actual position of the object.
	private double timestep;	//the filter timestep.
	private double acceln;		//the assumed constant acceleration.
	private double accelNoiseMag; //variability in acceleration (stdev of acceleration)
	
	/**
	 * Constructor for a KalmanFilter.
	 * 
	 * @param initialPos - the starting position of the object.
	 * @param timestep - the filter timestep.
	 * @param acceln - the assumed constant acceleration.
	 * @param accelNoiseMag	- the variability in the acceleration (stdev of acceleration).
	 */
	public KalmanFilter(Coord initialPos, double timestep, double acceln, double accelNoiseMag) {
		
		this.timestep = timestep;
		this.acceln = acceln;
		this.accelNoiseMag = accelNoiseMag;
		
		double[] initialState = {initialPos.getX(), initialPos.getY(), 0, 0};  	
		
	  	double subTerm1 = Math.pow(timestep, 4)/4;
	  	double subTerm2 = Math.pow(timestep, 3)/2;
	  	double subTerm3 = Math.pow(timestep, 2);
	  	double[][] eX = {{subTerm1, 0, subTerm2, 0}, {0, subTerm1, 0, subTerm2}, 
	  			{subTerm2, 0, subTerm3, 0}, {0, subTerm2, 0, subTerm3}};
	  	eX = MatrixOps.matrixScalarMult(eX, Math.pow(accelNoiseMag, 2));
	  	
	  	ksCorrected = new KalmanState(initialState, eX);
	}
	
	
	
	/**
	 * Method to predict the next location of the object using the Kalman filter.
	 * 
	 * (loosely based on the Matlab code found here: 
	 * http://studentdavestutorials.weebly.com/kalman-filter-with-matlab-code.html)
	 * 
	 * @return the predicted location of the object at the next timestep.
	 */
	public Coord kalmanFilterPredict() {
		
		
		double[] qEst = new double[4];	//estimate of location
		
		double subTerm1 = Math.pow(timestep, 4)/4;
		double subTerm2 = Math.pow(timestep, 3)/2;
		double subTerm3 = Math.pow(timestep, 2);
		
		double[][] eX = {{subTerm1, 0, subTerm2, 0}, {0, subTerm1, 0, subTerm2}, 
				{subTerm2, 0, subTerm3, 0}, {0, subTerm2, 0, subTerm3}};
		eX = MatrixOps.matrixScalarMult(eX, Math.pow(accelNoiseMag, 2));
		
		double[][] p = new double[4][4];	//error covariance, p.
		
		double[][] a = {{1, 0, timestep, 0}, {0, 1, 0, timestep}, {0, 0, 1, 0}, {0, 0, 0, 1}};
		double[][] aTrans = MatrixOps.matrixTranspose(a);
		double[] b = {subTerm3/2, subTerm3/2, timestep, timestep};
		
		
		//qEst(t) = A*q(t-1) + B*u;
		qEst = MatrixOps.matrixMult(ksCorrected.getState(), a);
		double[] bTemp = MatrixOps.matrixScalarMult(b, acceln);
		qEst = MatrixOps.matrixAddition(qEst, bTemp);
		
		//P(t) = A*P(t-1)*A' + eX;
		p = MatrixOps.matrixMult(a, ksCorrected.getP());
		p = MatrixOps.matrixMult(p, aTrans);
		p = MatrixOps.matrixAddition(p, eX);
		
		ksPredict = new KalmanState(qEst, p);
		return new Coord((int) Math.round(qEst[0]), (int) Math.round(qEst[1]));
		
	}
	
	/**
	 * Method to update the KalmanFilter with the actual measured location of the object.
	 * 
	 * @param measurement - the obtained actual location of the object.
	 */
	public void kalmanFilterMeasure(Coord measurement) {
		
		double[] measure = {measurement.getX(), measurement.getY()};
		
		double measureNoiseX = 1.0;	//measurement noise in x direction.
		double measureNoiseY = 1.0;	//measurement noise in y direction.
		double[][] eZ = {{measureNoiseX, 0}, {0, measureNoiseY}}; //measurement covariance matrix.
		
		double[][] c = {{1, 0, 0, 0}, {0, 1, 0, 0}};
		double[][] cTrans = MatrixOps.matrixTranspose(c);
		
		//K = pPredict*C' * inv(C*pPredict*C'+eZ);
		double[][] subK1 = MatrixOps.matrixMult(ksPredict.getP(), cTrans);
		double[][] subK2 = MatrixOps.matrixMult(c, ksPredict.getP());
		subK2 = MatrixOps.matrixMult(subK2, cTrans);
		subK2 = MatrixOps.matrixAddition(subK2, eZ);
		subK2 = MatrixOps.matrix2dInverse(subK2);
		double[][] k = MatrixOps.matrixMult(subK1, subK2);
		
		double[] q = new double[4];
		//qEstCorrected = qEstPredict + K * (measurement - C * qEstPredict);
		double[] temp1 = MatrixOps.matrixMult(c, ksPredict.getState());
		temp1 = MatrixOps.matrixAddition(measure, MatrixOps.matrixScalarMult(temp1, -1));
		temp1 = MatrixOps.matrixMult(k, temp1);
		q = MatrixOps.matrixAddition(ksPredict.getState(), temp1);
		
		//pCorrected =  (eye(4)-K*C)*pPredict;
		double[][] eye4 = {{1,0,0,0}, {0,1,0,0}, {0,0,1,0}, {0,0,0,1}};
		double[][] p = new double[4][4];
		double[][] temp2 = MatrixOps.matrixMult(k, c);
		temp2 = MatrixOps.matrixScalarMult(temp2, -1);
		temp2 = MatrixOps.matrixAddition(eye4, temp2);
		p = MatrixOps.matrixMult(temp2, ksPredict.getP());
		
		ksCorrected = new KalmanState(q, p);
		
	}
	

}
