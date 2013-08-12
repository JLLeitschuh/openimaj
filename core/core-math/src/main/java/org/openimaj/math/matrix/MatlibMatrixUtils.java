package org.openimaj.math.matrix;


import ch.akuhn.matrix.DenseMatrix;
import ch.akuhn.matrix.Matrix;
import ch.akuhn.matrix.SparseMatrix;
import ch.akuhn.matrix.Vector;
import ch.akuhn.matrix.Vector.Entry;

import com.jmatio.types.MLDouble;

/**
 * Some helpful operations on {@link Matrix} instances
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class MatlibMatrixUtils {

	private static final double EPS = 1e-8;

	/**
	 * @param mat
	 * @return uses {@link SparseMatrix#density()}
	 */
	public static double sparcity(SparseMatrix mat) {
		return 1 - mat.density();
	}

	/**
	 * Bring each element to the power d
	 * @param degree
	 * @param d
	 * @return the input
	 */
	public static <T extends Matrix>  T powInplace(T degree, double d) {
		int rowN = 0;
		for (Vector ent : degree.rows()) {
			for (Entry row : ent.entries()) {
				degree.put(rowN, row.index, Math.pow(row.value, d));
			}
			rowN++;
		}
		return degree;
	}

	/**
	 * @param D
	 * @param A
	 * @return R = D . A
	 */
	public static SparseMatrix times(DiagonalMatrix D, SparseMatrix A) {
		SparseMatrix mat = new SparseMatrix(A.rowCount(), A.columnCount());
		double[] Dvals = D.getVals();
		int rowIndex = 0;
		for (Vector row : A.rows()) {
			for (Entry ent : row.entries()) {
				mat.put(rowIndex, ent.index, ent.value * Dvals[rowIndex]);
			}
			rowIndex++;
		}
		return mat;
	}

	/**
	 * @param D
	 * @param A
	 * @return R =  A . D
	 */
	public static SparseMatrix times(SparseMatrix A,DiagonalMatrix D) {
		SparseMatrix mat = new SparseMatrix(A.rowCount(), A.columnCount());
		int rowIndex = 0;
		double[] Dvals = D.getVals();
		for (Vector row : A.rows()) {
			for (Entry ent : row.entries()) {
				mat.put(rowIndex, ent.index, ent.value * Dvals[ent.index]);
			}
			rowIndex++;
		}
		return mat;
	}

	/**
	 * A = A + B
	 * @param A
	 * @param B
	 * @return A
	 */
	public static SparseMatrix plusInplace(SparseMatrix A, SparseMatrix B) {
		for (int i = 0; i < A.rowCount(); i++) {
			A.addToRow(i, B.row(i));
		}
		return A;
	}

	/**
	 * @param D
	 * @param A
	 * @return D - A, the same matrix A
	 *
	 */
	public static <T extends Matrix> T minusInplace(DiagonalMatrix D, T A) {
		double[] Dval = D.getVals();
		for (int i = 0; i < Dval.length; i++) {
			A.put(i, i, Dval[i] - A.get(i, i));
		}
		return A;
	}

	/**
	 * @param D
	 * @param A
	 * @return the same matrix A
	 *
	 */
	public static <T extends Matrix> T plusInplace(DiagonalMatrix D, T A) {
		double[] Dval = D.getVals();
		for (int i = 0; i < Dval.length; i++) {
			A.put(i, i, A.get(i, i) + Dval[i]);
		}
		return A;
	}

	/**
	 * Y = A . Bt
	 * @param A
	 * @param B
	 * @param Y
	 * @return Y
	 */
	public static <T extends Matrix> T dotProductTranspose(Matrix A, Matrix B, T Y) {
		int mA = A.rowCount();
		int nB = B.rowCount();

		for (int i = 0; i < mA; i++) {
			for (int j = 0; j < nB; j++) {
				double dot = A.row(i).dot(B.row(j));
				if(Math.abs(dot)>EPS) Y.put(i, j, dot);
			}
		}
		return Y;
	}

	/**
	 * A = A . s
	 * @param A
	 * @param s
	 * @return A
	 */
	public static SparseMatrix scaleInplace(SparseMatrix A, double s) {
		for (Vector row: A.rows()) {
			row.timesEquals(s);
		}
		return A;
	}

	/**
	 * @param laplacian
	 * @return returns a dense jama matrix
	 */
	public static Jama.Matrix toJama(Matrix laplacian) {
		Jama.Matrix ret = new Jama.Matrix(laplacian.asArray());
		return ret;
	}

	/**
	 * @param vector
	 * @return the vector as a column in a matrix
	 */
	public static Jama.Matrix toColJama(Vector vector) {
		double[] vec = new double[vector.size()];
		vector.storeOn(vec, 0);
		Jama.Matrix ret = new Jama.Matrix(vec.length, 1);
		for (int i = 0; i < vec.length; i++) {
			ret.set(i, 0, vec[i]);
		}

		return ret;
	}

	/**
	 * @param vector
	 * @return the vector as a row in a matrix
	 */
	public static Jama.Matrix toRowJama(Vector vector) {
		double[] vec = new double[vector.size()];
		vector.storeOn(vec, 0);
		Jama.Matrix ret = new Jama.Matrix(1,vec.length);
		for (int i = 0; i < vec.length; i++) {
			ret.set(0, i, vec[i]);
		}

		return ret;
	}

	/**
	 * @param sol
	 * @return Dense matrix from a {@link Jama.Matrix}
	 */
	public static Matrix fromJama(Jama.Matrix sol) {
		DenseMatrix mat = new DenseMatrix(sol.getRowDimension(), sol.getColumnDimension());
		for (int i = 0; i < mat.rowCount(); i++) {
			for (int j = 0; j < mat.columnCount(); j++) {
				mat.put(i, j, sol.get(i, j));
			}
		}
		return mat;
	}

	/**
	 * Extract the submatrix of the same type of mat 
	 * 
	 * @param mat
	 * @param rowstart
	 * @param rowend
	 * @param colstart
	 * @param colend
	 * @return new instance
	 */
	public static <T extends Matrix> T subMatrix(T mat, int rowstart, int rowend, int colstart, int colend) {
		@SuppressWarnings("unchecked")
		T ret = (T) mat.newInstance(rowend - rowstart, colend - colstart);

		for (int i = 0; i < ret.rowCount(); i++) {
			Vector row = mat.row(i + rowstart);
			for (Entry ent : row.entries()) {
				if(ent.index >= colstart && ent.index < colend){
					ret.put(i, ent.index-colstart, ent.value);
				}
			}
		}

		return ret;
	}

	/**
	 * @param m
	 * @return a {@link MLDouble} for matlab
	 */
	public static MLDouble asMatlab(Matrix m){
		double[][] retArr = new double[m.rowCount()][m.columnCount()];
		for (int i = 0; i < retArr.length; i++) {
			for (int j = 0; j < retArr[i].length; j++) {
				retArr[i][j] = m.get(i, j);
			}
		}
		MLDouble ret = new MLDouble("out", retArr);
		return ret;
	}

	/**
	 * Calculate all 3, used by {@link #min(Matrix)}, {@link #max(Matrix)} and {@link #mean(Matrix)}
	 * @param mat
	 * @return the min, max and mean of the provided matrix
	 */
	public static double[] minmaxmean(Matrix mat) {
		double min = Double.MAX_VALUE, max = - Double.MAX_VALUE, mean = 0;
		double size = mat.rowCount() * mat.columnCount();
		for (Vector v : mat.rows()) {
			for (Entry ent : v.entries()) {
				min = Math.min(min, ent.value);
				max = Math.max(max, ent.value);
				mean += ent.value / size;
			}
		}
		return new double[]{min,max,mean};
	}
	
	/**
	 * uses the first value returned by {@link #minmaxmean(Matrix)}
	 * @param mat
	 * @return the min
	 */
	public static double min(Matrix mat) {
		return minmaxmean(mat)[0];
	}
	
	/**
	 * uses the second value returned by {@link #minmaxmean(Matrix)}
	 * @param mat
	 * @return the min
	 */
	public static double max(Matrix mat) {
		return minmaxmean(mat)[1];
	}
	
	/**
	 * uses the third value returned by {@link #minmaxmean(Matrix)}
	 * @param mat
	 * @return the min
	 */
	public static double mean(Matrix mat) {
		return minmaxmean(mat)[2];
	}

	/**
	 * @param l
	 * @param v
	 * @return performs l - v returning a matrix of type T
	 */
	public static<T extends Matrix> T minus(T l, double v) {
		@SuppressWarnings("unchecked")
		T ret = (T) l.newInstance(l.rowCount(), l.columnCount());
		int r = 0;
		for (Vector vec : l.rows()) {
			for (Entry ent : vec.entries()) {
				ret.put(r, ent.index, ent.value - v);
			}
			r++;
		}
		return ret;
	}
	
	/**
	 * @param v
	 * @param l
	 * @return performs v - l returning a matrix of type T
	 */
	public static<T extends Matrix> T minus(double v, T l) {
		@SuppressWarnings("unchecked")
		T ret = (T) l.newInstance(l.rowCount(), l.columnCount());
		for (int i = 0; i < l.rowCount(); i++) {
			for (int j = 0; j < l.columnCount(); j++) {
				ret.put(i,j, v - l.get(i,j));
			}
		}
		
		return ret;
	}
	
	
	
}