package penTracking;

/**
 * Class containing numerous basic matrix operations that can be carried out on either 1D or 2D arrays of 
 * doubles.
 * 
 * @author Simon Dicken (Student ID: 1378818)
 * @version 2014-07-16
 */
public class MatrixOps {

	/**
	 * Multiply two 2D matrices together.
	 * 
	 * @param a - the first 2D matrix.
	 * @param b - the second 2D matrix.
	 * @return a 2D array representing a.b.
	 */
	public static double[][] matrixMult(double[][] a, double[][] b) {
		
		if(a[0].length != b.length) {
			throw new IllegalArgumentException("Inner matrix dimensions do not match.");
		}
		
		double[][] ans = new double[a.length][b[0].length];
		
		for(int i=0; i<ans.length; i++) {
			for(int j=0; j<ans[0].length; j++) {
				ans[i][j] = 0;
				for(int k=0; k<a[0].length; k++) {
					ans[i][j] += a[i][k] * b[k][j];
				}
			}
		}
		
		return ans;
	}
	
	public static double[] matrixMult(double[] a, double[][] b) {
		
		if(a.length != b.length) {
			throw new IllegalArgumentException("Inner matrix dimensions do not match.");
		}
		
		double[] ans = new double[b[0].length];
		
		for(int i=0; i<ans.length; i++) {
			
			ans[i] = 0;
			for(int k=0; k<b[0].length; k++) {
				ans[i] += a[k] * b[k][i];
			}
			
		}
		
		return ans;
	}
	
	public static double[] matrixMult(double[][] a, double[] b) {
		
		if(a[0].length != b.length) {
			throw new IllegalArgumentException("Inner matrix dimensions do not match.");
		}
		
		double[] ans = new double[a.length];
		
		for(int i=0; i<ans.length; i++) {
			
			ans[i] = 0;
			for(int k=0; k<a[0].length; k++) {
				ans[i] += a[i][k] * b[k];
			}
			
		}
		
		return ans;
	}
	
	public static double[][] matrixTranspose(double[][] a) {
		double[][] ans = new double[a[0].length][a.length];
		
		for(int i=0; i<a.length; i++) {
			for(int j=0; j<a[0].length; j++) {
				ans[j][i] = a[i][j];
			}
		}
		
		return ans;
	}
	
	public static double[][] matrixAddition(double[][] a, double[][] b) {
		
		if((a.length != b.length) || (a[0].length != b[0].length)) {
			throw new IllegalArgumentException("Matrix dimensions do not match.");
		}
		
		double[][] ans = new double[a.length][a[0].length];
		
		for(int i=0; i<a.length; i++) {
			for(int j=0; j<a[0].length; j++) {
				ans[i][j] = a[i][j] + b[i][j];
			}
		}
		
		return ans;
	}
	
	public static double[] matrixAddition(double[] a, double[] b) {
		
		if(a.length != b.length) {
			throw new IllegalArgumentException("Matrix dimensions do not match.");
		}
		
		double[] ans = new double[a.length];
		
		for(int i=0; i<a.length; i++) {
			ans[i] = a[i] + b[i];
		}
		
		return ans;
	}
	
	public static double[][] matrixScalarMult(double[][] a, double lambda) {
		
		double[][] ans = new double[a.length][a[0].length];
				
		for(int i=0; i<a.length; i++) {
			for(int j=0; j<a[0].length; j++) {
				ans[i][j] = a[i][j] * lambda;
			}
		}
		
		return ans;
	}
	
	public static double[] matrixScalarMult(double[] a, double lambda) {
		
		double[] ans = new double[a.length];
				
		for(int i=0; i<a.length; i++) {
				ans[i] = a[i] * lambda;
		}
		
		return ans;
	}
	
	public static double[][] matrix2dInverse(double[][] a) {
		
		if(a.length!=2 || a[0].length!=2) {
			throw new IllegalArgumentException("Matrix must have dimensions 2x2");
		}
		
		double[][] ans = new double[a.length][a[0].length];
		
		//determinant = a*d - b*c
		double det = (a[0][0] * a[1][1] - a[0][1] * a[1][0]);
		
		//swap a and d. negate b and c. multiply all by 1/det.
		ans[0][0] = (1/det) * a[1][1];
		ans[1][1] = (1/det) * a[0][0];
		ans[0][1] = (-1/det) * a[0][1];
		ans[1][0] = (-1/det) * a[1][0];
		
		return ans;
	}
	
}
