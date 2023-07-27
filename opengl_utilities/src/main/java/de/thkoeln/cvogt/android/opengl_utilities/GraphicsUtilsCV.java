// This work is provided under GPLv3, the GNU General Public License 3
//   http://www.gnu.org/licenses/gpl-3.0.html

// Prof. Dr. Carsten Vogt
// Technische Hochschule Köln, Germany
// Fakultät für Informations-, Medien- und Elektrotechnik
// carsten.vogt@th-koeln.de
// 27.1.2023

// For algorithms concerning points / lines / planes see: http://paulbourke.net/geometry/pointlineplane/

package de.thkoeln.cvogt.android.opengl_utilities;

import android.graphics.Point;
import android.opengl.Matrix;
import android.util.Log;

/**
 * Class with some utility methods for 2D and 3D graphics programming in general.
 * The class GLUtilsCV contains utility methods for the OpenGL-related classes and objects defined in this package.
 * @see GLUtilsCV
 */

public class GraphicsUtilsCV {

    /** Method to calculate the distance between two points in 2D space.
     *
     * @param p1 First point
     * @param p2 Second point
     * @return Distance between the points
     */

    public static double distance2D(Point p1, Point p2) {
        return distance2D(p1.x,p1.y,p2.x,p2.y);
    }

    /** Method to calculate the distance between two points in 2D space.
     *
     * @param p1x First point: x position
     * @param p1y First point: y position
     * @param p2x Second point: x position
     * @param p2y Second point: y position
     * @return Distance between the points
     */

    public static double distance2D(double p1x, double p1y, double p2x, double p2y) {
        return Math.sqrt((p2x-p1x)*(p2x-p1x)+(p2y-p1y)*(p2y-p1y));
    }

    /** Method to calculate the distance between two points in 3D space.
     *
     * @param p1 The first point: Array of length 3 with the (x,y,z) coordinates (in this order).
     * @param p2 The second point: Array of length 3 with the (x,y,z) coordinates (in this order).
     * @return The distance between the points or -1 if the parameters are not valid
     * (i.e. one of them is null or has a length other than 3).
     */

    public static float distance3D(float[] p1, float[] p2) {
        if (p1==null||p2==null||p1.length!=3||p2.length!=3) return -1;
        return vectorLength(vectorFromTo3D(p1,p2));
    }

    /** Method to calculate the distance between a single point and a line in 3D space.
     *
     * @param singlePoint The single point: Array of length 3 with the (x,y,z) coordinates (in this order).
     * @param linePoint Some point on the line: Array of length 3 with the (x,y,z) coordinates (in this order).
     * @param lineVector Some vector on the line: Array of length 3 with the (x,y,z) coordinates (in this order).
     * @return The distance between the single point and the line or -1 if the parameters are not valid
     * (i.e. one of them is null or has a length other than 3 or if the line vector is the null vector).
     */

    public static float distancePointToLine3D(float[] singlePoint, float[] linePoint, float[] lineVector) {
        if (singlePoint==null||singlePoint.length!=3||linePoint==null||linePoint.length!=3||lineVector==null||lineVector.length!=3) return -1;
        if (isZero(vectorLength(lineVector))) return -1;
        // Calculation as described at https://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line > "Another vectro formulation"
        float[] vectorLinePointToSinglePoint = new float[3];
        for (int i=0;i<3;i++)
            vectorLinePointToSinglePoint[i] = singlePoint[i]-linePoint[i];
        double distance_numerator = vectorLength(crossProduct3D(vectorLinePointToSinglePoint,lineVector));
        double distance_denominator = vectorLength(lineVector);
        return (float) (distance_numerator/distance_denominator);
    }

    /** Method to calculate the length of a vector in 2D or 3D space.
     *
     * @param vector The vector.
     * @return The length of the vector or -1 if the parameter is not valid
     * (i.e. is null or has a length other than 2 or 3).
     */

    public static float vectorLength(float[] vector) {
        if (vector==null||vector.length<2||vector.length>3) return -1;
        if (vector.length==2)
            return (float)Math.sqrt(vector[0]*vector[0]+vector[1]*vector[1]);
          else
            return (float)Math.sqrt(vector[0]*vector[0]+vector[1]*vector[1]+vector[2]*vector[2]);
    }

    /** Method to determine the vector between two points in 3D space.
     *
     * @param point1 The origin of the vector: Array of length 3 with the (x,y,z) coordinates (in this order).
     * @param point2 The target of the vector: Array of length 3 with the (x,y,z) coordinates (in this order).
     * @return The vector from the origin to the target or null if one of the parameters is not valid
     * (i.e. is null or has a length other than 3).
     */

    public static float[] vectorFromTo3D(float[] point1, float[] point2) {
        if (point1==null||point1.length!=3||point2==null||point2.length!=3) return null;
        float[] result = new float[3];
        result[0] = point2[0]-point1[0];
        result[1] = point2[1]-point1[1];
        result[2] = point2[2]-point1[2];
        return result;
    }

    /**
     * Method to determine if two vectors are linearly dependent.
     * @param vector1 The first vector.
     * @param vector2 The second vector.
     * @return false if the vectors are not linearly dependent or one of the parameters is not valid; true otherwise
     */

    public static boolean linearlyDependent(float[] vector1, float[] vector2) {
        if (vector1==null||vector2==null||vector1.length!=vector2.length) return false;
        if (isZero(vectorLength(vector1))||isZero(vectorLength(vector2))) return false;
        float normedDotProduct = dotProduct3D(vector1,vector2)/(vectorLength(vector1)*vectorLength(vector2));
        return valuesEqual(normedDotProduct,1);
    }

    /** Method to determine, in 2D space, the coefficients a and b of the linear function f(x)=a*x+b
     * whose graph goes through two given points.
     *
     * @param p1 The first point: Array of length 2 with the (x,y) coordinates in its positions 0 and 1.
     * @param p2 The second point: Array of length 2 with the (x,y) coordinates in its positions 0 and 1.
     * @return An array of length 2 with coefficient a in position 0 and coefficient b in position 1
     *         or null if the parameters are not valid
     *         (i.e. if one of them is null or has a length other than 2 or if both x coordinates are equal).
     */

    public static float[] linearFunctionCoeffsFromPoints2D(float[] p1, float[] p2) {
        if (p1==null||p1.length!=2||p2==null||p2.length!=2||p1[0]==p2[0]) return null;
        float[] result = new float[2];
        // coefficient a
        result[0] = (p2[1]-p1[1])/(p2[0]-p1[0]);
        // coefficient b
        result[1] = p2[1]-result[0]*p2[0];
        return result;
    }

    /** Method to determine, in 2D space, the coordinates of the intersection
     * of two linear functions f1(x)=a1*x+b1 and f2(x)=a2*x+b2.
     *
     * @param a1 The first coefficient of the first function.
     * @param b1 The second coefficient of the first function.
     * @param a2 The first coefficient of the second function.
     * @param b2 The second coefficient of the second function.
     * @return An array of length 2 with the x coordinate of the intersection in position 0 and the y coordinate in position 1
     *         or null if the functions do not intersect, i.e. are parallel, or are identical.
     */

    public static float[] intersectionLinearFunctions2D(float a1, float b1, float a2, float b2) {
        if (a1==a2) return null;
        float[] result = new float[2];
        // x coordinate
        result[0] = (b1-b2)/(a2-a1);
        // y coordinate
        result[1] = a1*result[0]+b1;
        return result;
    }

    /**
     * Method to determine, in 3D space, the intersection of two lines
     * or (if the lines do not intersect) the shortest line segment between two lines.
     * Each of the two lines is defined by two points on this line.
     * <P>
     * The code is based on the theory explained in http://paulbourke.net/geometry/pointlineplane/, "The shortest line between two lines in 3D".
     *
     * @param p11 The first point of the first line.
     * @param p12 The second point of the first line.
     * @param p21 The first point of the second line.
     * @param p22 The second point of the second line.
     * @return If the lines are identical or parallel: null.
     *         If the lines intersect: An array of length 3 with the x, y, and z coordinates of the intersection.
     *         Otherwise: An array of length 6 specifying the shortest line segment between the two lines
     *                    - positions 0-2: start point coordinates (x,y,z) and positions 3-5: end point coordinates (x,y,z).
     */

    public static float[] lineIntersectionOrShortestConnection3D(float[] p11, float[] p12, float[] p21, float[] p22) {

        float[] dirvec1 = new float[3];  // direction vector of the first line
        float[] dirvec2 = new float[3];  // direction vector of the second line
        float[] vec12 = new float[3];    // vector between some point on the first line and some point of the second line

        for (int i=0;i<3;i++) {          // initialization of the direction vectors
            dirvec1[i] = p12[i] - p11[i];
            dirvec2[i] = p22[i] - p21[i];
            vec12[i]   = p11[i] - p21[i];
        }

        float epsilon = 0.0001f;   // return null if one of the direction vectors is null, i.e. defines no line
        if (vectorLength(dirvec1)<epsilon||vectorLength(dirvec2)<epsilon) return null;

        // calculate the result as explained in
        // http://paulbourke.net/geometry/pointlineplane/, "The shortest line between two lines in 3D"
        // (detailed explanation not repeated here)

        float dot_vec12_dirvec1 = dotProduct3D(vec12,dirvec1);
        float dot_vec12_dirvec2 = dotProduct3D(vec12,dirvec2);
        float dot_dirvec1_dirvec2 = dotProduct3D(dirvec1,dirvec2);
        float lengthsquare_dirvec1 = dotProduct3D(dirvec1,dirvec1);
        float lengthsquare_dirvec2 = dotProduct3D(dirvec2,dirvec2);

        float a = dot_vec12_dirvec2 * dot_dirvec1_dirvec2 - dot_vec12_dirvec1 * lengthsquare_dirvec2;
        float b = lengthsquare_dirvec1 * lengthsquare_dirvec2 - dot_dirvec1_dirvec2 * dot_dirvec1_dirvec2;
        if (Math.abs(b) < epsilon) return null;

        float lambda1 = a / b;
        float lambda2 = (dot_vec12_dirvec2 + dot_dirvec1_dirvec2 * lambda1) / lengthsquare_dirvec2;

        float[] segmentStart = new float[3];
        float[] segmentEnd = new float[3];

        for (int i=0;i<3;i++) {
            segmentStart[i] = p11[i] + lambda1 * dirvec1[i];  // start point of the line se
            segmentEnd[i] = p21[i] + lambda2 * dirvec2[i];
        }

        // if both points of the segment are equal: return this point as the intersection of the two vectors

        if (valuesEqual(segmentStart[0], segmentEnd[0])&&valuesEqual(segmentStart[1], segmentEnd[1])&&valuesEqual(segmentStart[2], segmentEnd[2]))
            return segmentStart;

        // otherwise return both points, i.e. the specification of the shortest segment

        return new float[] { segmentStart[0], segmentStart[1], segmentStart[2], segmentEnd[0], segmentEnd[1], segmentEnd[2]};

    }

    /** Method that returns a random point in 3D space.
     * @param minX The minimum value for the x coordinate (inclusive).
     * @param maxX The maximum value for the x coordinate (exclusive).
     * @param minY The minimum value for the y coordinate (inclusive).
     * @param maxY The maximum value for the y coordinate (exclusive).
     * @param minZ The minimum value for the z coordinate (inclusive).
     * @param maxZ The maximum value for the z coordinate (exclusive).
     * @return The random point (float array of length 3 with the x, y, and z coordinates)
     * or null if one of the three parameter pairs defines no valid interval.
     */

    public static float[] randomPoint3D(float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
        if (minX>maxX||minY>maxY||minZ>maxZ) return null;
        float[] result = new float[3];
        result[0] = minX + (float)((maxX-minX)*Math.random());
        result[1] = minY + (float)((maxY-minY)*Math.random());
        result[2] = minZ + (float)((maxZ-minZ)*Math.random());
        return result;
    }

    /** Method to map multiple 2D points to 3D space.
     * The x and y coordinates will be copied, the z coordinate will be set to 0.
     * @param points2D The points in 2D space.
     * @return The points in 3D space (second dimension: index 0 - x coordinate, 1 - y coordinate, 2 - z coordinate = 0)
     * or null if points2D is null.
     */

    public static float[][] points2Dto3D(Point[] points2D) {
        if (points2D==null) return null;
        float[][] result = new float[points2D.length][3];
        for (int i=0; i<points2D.length; i++) {
            result[i][0] = points2D[i].x;
            result[i][1] = points2D[i].y;
            result[i][2] = 0;
        }
        return result;
    }

    /** Method to get the homogeneous coordinate representation of a point in 3D space.
     * @param coordinates The x, y, and z coordinates of the point (must be an array of length 3).
     * @return The homogeneous coordinate representation of the point,
     * i.e. a vector of length 4 with copies of the values of the parameter array in the first three positions
     * and a 1 in the last position.
     * Null if the parameter is not valid.
     */

    public static float[] homogeneousCoordsForPoint3D(float[] coordinates) {
        if (coordinates==null||coordinates.length!=3) return null;
        float[] result = new float[4];
        for (int i=0; i<3; i++) result[i] = coordinates[i];
        result[3] = 1;
        return result;
    }

    /** Method to get the homogeneous coordinate representation of a vector in 3D space.
     * @param coordinates The x, y, and z coordinates of the vector (must be an array of length 3).
     * @return The homogeneous coordinate representation of the vector,
     * i.e. a vector of length 4 with copies of the values of the parameter array in the first three positions
     * and a 0 in the last position.
     * Null if the parameter is not valid.
     */

    public static float[] homogeneousCoordsForVector3D(float[] coordinates) {
        if (coordinates==null||coordinates.length!=3) return null;
        float[] result = new float[4];
        for (int i=0; i<3; i++) result[i] = coordinates[i];
        result[3] = 0;
        return result;
    }

    /** Method to get the x, y, and z coordinates from the homogeneous representation of a point or vector in 3D space.
     * @param homogeneous The homogeneous representation (must be an array of length 4).
     * @return The 3D coordinates of the point (array of length 3 with x in array position 0, y in pos. 1, z in pos. 2).
     * Null if the parameter is not valid.
     */

    public static float[] coordsFromHomogeneous3D(float[] homogeneous) {
        if (homogeneous==null||homogeneous.length!=4) return null;
        float[] result = new float[3];
        for (int i=0; i<3; i++) result[i] = homogeneous[i];
        return result;
    }

    /** Method to calculate the midpoint between two points in 3D space.
     * @param p1 The first point: Array of length 3 with the (x,y,z) coordinates (in this order).
     * @param p2 The second point: Array of length 3 with the (x,y,z) coordinates (in this order).
     * @return The midpoint between the two points or null if one of the parameters is not valid
     * (i.e. is null or has a length other than 3).
     */

    public static float[] midpoint3D(float[] p1, float[] p2) {
        if (p1==null||p1.length!=3||p2==null||p2.length!=3) return null;
        float[] result = new float[3];
        for (int i=0;i<3;i++) {
            result[i] = p1[i] + (p2[i] - p1[i]) / 2;
            // Log.v("GLDEMO",i+": "+p1[i]+" "+p2[i]+" "+result[i]);
        }
        return result;
    }

    /** Method to calculate the center of a set of points in 3D space.
     * @param points A two-dimensional array with the x, y, and z coordinate of point i in its ith position.
     * @return The center point whose x coordinate is calculated as the arithmetic mean of the minimum and the maximum
     * of the x coordinates of all points and whose y and z coordinates are calculated correspondingly.
     */

    public static float[] center3D(float[] ... points) {
        float[] minCoords, maxCoords;
        minCoords = points[0].clone();
        maxCoords = points[0].clone();
        for (float[] point : points)
            for (int i=0; i<3; i++) {
                if (minCoords[i]>point[i]) minCoords[i]=point[i];
                if (maxCoords[i]<point[i]) maxCoords[i]=point[i];
            }
        float[] center = new float[3];
        for (int i=0; i<3; i++)
            center[i] = (minCoords[i]+maxCoords[i])/2;
        return center;
    }

    /** Method to calculate a number of points in 3D space that lie equidistantly between two end points.
     * @param endpoint1 The first end point: Array of length 3 with the (x,y,z) coordinates (in this order).
     * @param endpoint2 The second end point: Array of length 3 with the (x,y,z) coordinates (in this order).
     * @param numberOfPoints The number of points to be calculated (>1).
     * @return The points as a two-dimensional array of size 'numberOfPoints' in the first dimension
     * and 3 in the second dimension (specifying the x, y, and z coordinates).
     * The first point (index 0) is endpoint1, the last point (index numberOfPoints-1) is endpoint2.
     * Null is returned if one of the end point parameters is not valid (i.e. null or not of length 3)
     * or numberOfPoints is not greater than 1.
     */

    public static float[][] pointsInLine3D(float[] endpoint1, float[] endpoint2, int numberOfPoints) {
        if (endpoint1==null||endpoint1.length!=3||endpoint2==null||endpoint2.length!=3||numberOfPoints<2) return null;
        float[][] result = new float[numberOfPoints][3];
        for (int i=0; i<numberOfPoints; i++)
            if (i<numberOfPoints-1)
                for (int j=0; j<3; j++) result[i][j] = endpoint1[j]+(endpoint2[j]-endpoint1[j])/(numberOfPoints-1)*i;
            else
                result[i] = endpoint2.clone();
        return result;
    }

    /** Method to normalize a vector in 3D space.
     *
     * @param vector The vector to normalize.
     * @return true if the operation was successful.
     * false if the parameter is not valid (i.e. is null or has a length other than 3) or if the length of the vector is zero or very close to zero.
     */

    public static boolean normalizeVector3D(float[] vector) {
        if (vector==null||vector.length!=3) return false;
        float length = vectorLength(vector);
        if (length<1E-9f) return false;
        vector[0] /= length;
        vector[1] /= length;
        vector[2] /= length;
        return true;
    }

    /** Method to get a normalized copy of a vector in 3D space.
     *
     * @param vector The vector to normalize.
     * @return A normalized copy of the vector, i.e. a vector of length 1 with the same direction.
     * null if the parameter is not valid (i.e. is null or has a length other than 3) or if the length of the vector is zero or very close to zero.
     */

    public static float[] getNormalizedVectorCopy3D(float[] vector) {
        if (vector==null||vector.length!=3) return null;
        float[] result = new float[3];
        float length = vectorLength(vector);
        if (length<1E-9f) return null;
        result[0] = vector[0]/length;
        result[1] = vector[1]/length;
        result[2] = vector[2]/length;
        return result;
    }

    /** Method to calculate the dot product of two vectors in 3D space.
     * @param vec1 The first vector.
     * @param vec2 The second vector.
     * @return The dot product or -1 if one of the parameters is not valid
     * (i.e. is null or has a length other than 3).
     */

    public static float dotProduct3D(float[] vec1, float[] vec2) {
        if (vec1==null||vec1.length!=3||vec2==null||vec2.length!=3) return -1;
        return vec1[0]*vec2[0]+vec1[1]*vec2[1]+vec1[2]*vec2[2];
    }

    /** Method to calculate the cross product of two vectors in 3D space,
     *  i.e. a vector that is perpendicular to the plane spanned by the two vectors.
     * @param vec1 The first vector.
     * @param vec2 The second vector.
     * @return The cross product or null if one of the parameters is not valid
     * (i.e. is null or has a length other than 3).
     */

    public static float[] crossProduct3D(float[] vec1, float[] vec2) {
        if (vec1==null||vec1.length!=3||vec2==null||vec2.length!=3) return null;
        float[] result = new float[3];
        result[0] = vec1[1]*vec2[2]-vec1[2]*vec2[1];
        result[1] = vec1[2]*vec2[0]-vec1[0]*vec2[2];
        result[2] = vec1[0]*vec2[1]-vec1[1]*vec2[0];
        return result;
    }

    /** Method to check if two double values are nearly equal,
     * i.e. if their difference is smaller than 1E-6.
     * @param f1 The first value.
     * @param f2 The second value.
     * @return true iff they are nearly equal.
     */

    public static boolean valuesEqual(double f1, double f2) {
        return valuesEqual(f1,f2,1E-6);
    }

    /** Method to check if two double values are nearly equal,
     * i.e. if their difference is smaller than the parameter 'epsilon'.
     * @param f1 The first value.
     * @param f2 The second value.
     * @param f2 The difference threshold.
     * @return true iff their difference is smaller than epsilon.
     */

    public static boolean valuesEqual(double f1, double f2, double epsilon) {
        return Math.abs(f1-f2)<epsilon;
    }

    /** Method to check if a value is nearly zero,
     * i.e. if its absolute value than 1E-6.
     * @param value The value.
     * @return true iff value is nearly zero.
     */

    public static boolean isZero(double value) {
        return Math.abs(value)<1E-6;
    }

    /** Method to check if a matrix is an identity matrix,
     * i.e. a square matrix with ones in its diagonal and zeros elsewhere.
     * @param matrix The matrix.
     * @return true iff the matrix is an identity matrix
     */

    public static boolean isIdentity(float[][] matrix) {
        if (matrix==null||matrix.length!=matrix[0].length) return false;
        for (int i=0; i<matrix.length; i++)
            if (!valuesEqual(matrix[i][i],1.0)) return false;
        for (int i=0; i<matrix.length; i++)
            for (int j=0; j<matrix[i].length; j++)
                if (i!=j && !valuesEqual(matrix[i][j],0.0)) return false;
        return true;
    }

    /** Method to check if a matrix is an 4x4 rotation matrix.
     * @param matrix The matrix.
     * @return true iff the matrix is a rotation matrix.
     */

    public static boolean is4x4RotationMatrix(float[][] matrix) {
        if (matrix==null||matrix.length!=4) return false;
        for (int i=0; i<matrix.length; i++)
            if (matrix[i].length!=4) return false;
        // for the following see https://math.stackexchange.com/questions/261617/find-the-rotation-axis-and-angle-of-a-matrix/1886992
        float[] m1 = arrayFromMatrix(matrix);
        float[] m2 = new float[16];
        Matrix.transposeM(m2,0,m1,0);
        float[] m3 = new float[16];
        Matrix.multiplyMM(m3,0,m2,0,m1,0);
        // hier eigentlich noch prüfen, ob die Determinante = 1 ist
        // (wenn -1, dann Spiegelung)
        return isIdentity(matrixFromArray(m3,4,4));
    }

    /** Method to transform a two-dimensional matrix into a one-dimensional array,
     * copying its columns one after the other
     * (needed e.g. to transform a scaling, rotation or transformation matrix
     * into the one-dimensional format required by OpenGL - a float array of length 16).
     * @param matrix The matrix to be transformed.
     * @return The corresponding array; null if the parameter is not valid.
     */

    public static float[] arrayFromMatrix(float[][] matrix) {
        if (matrix==null) return null;
        float[] array = new float[matrix.length*matrix[0].length];
        for (int i=0;i<array.length;i++)
            array[i] = matrix[i%matrix.length][i/matrix.length];
        return array;
    }

    /** Method to transform a one-dimensional array into a two-dimensional matrix,
     * i.e. to copy its lines one after the other to a one-dimensional array
     * (needed e.g. to transform the one-dimensional matrix format of OpenGL
     * into a two-dimensional Java array).
     * @param array The array to be transformed.
     * @param lines The number of lines of the resulting matrix.
     * @param columns The number of columns of the resulting matrix.
     * @return The corresponding array; null if one of the parameters is not valid.
     */

    public static float[][] matrixFromArray(float[] array, int lines, int columns) {
        if (array==null||lines<1||columns<1||array.length!=lines*columns) return null;
        if (array==null||lines<1||columns<1||array.length!=lines*columns) return null;
        float[][] matrix = new float[lines][columns];
        for (int i=0;i<array.length;i++)
            matrix[i%lines][i/lines] = array[i];
        return matrix;
    }

    /**
     * Method to calculate a rotation matrix from three Euler angles (= cardan angles)
     * Code taken and adapted from: https://www.euclideanspace.com/maths/geometry/rotations/conversions/eulerToMatrix/index.htm
     * Note: Euler rotations are not commutative! Current rotation order: X > Z > Y (THIS ORDER IS YET TO BE ADAPTED: X > Y > Z)
     * ALSO TO BE ADAPTED: POSITIVE / NEGATIVE ROTATION ANGLE (?) BUT: IS CURRENTLY THE SAME AS WITH THE OTHER TWO APPROACHES
     * @param eulerX The Euler angle in the x dimension.
     * @param eulerY The Euler angle in the y dimension.
     * @param eulerZ The Euler angle in the z dimension.
     * @return The 4x4 rotation matrix as a one-dimensional array of length 16.
     */

    public static float[] rotationMatrixFromEulerAngles(float eulerX, float eulerY, float eulerZ) {

        // Matrix.setRotateEulerM(rotationMatrix,0,eulerX,eulerY,eulerZ);
        // THE IMPLEMENTATION OF THE METHOD Matrix.setRotateEulerM() IS BUGGY (AS OF 18.9.22), DOES NOT ROTATE CORRECTLY AROUND THE Y AXIS.

        float cosX = (float) Math.cos(Math.PI*eulerX/180.0);
        float sinX = (float) Math.sin(Math.PI*eulerX/180.0);
        float cosY = (float) Math.cos(Math.PI*eulerY/180.0);
        float sinY = (float) Math.sin(Math.PI*eulerY/180.0);
        float cosZ = (float) Math.cos(Math.PI*eulerZ/180.0);
        float sinZ = (float) Math.sin(Math.PI*eulerZ/180.0);

        float[] rotationMatrix = new float[16];

        rotationMatrix[0] = cosY * cosZ;
        rotationMatrix[1] = sinY*sinX - cosY*sinZ*cosX;
        rotationMatrix[2] = cosY*sinZ*sinX + sinY*cosX;
        rotationMatrix[3] = 0.0f;
        rotationMatrix[4] = sinZ;
        rotationMatrix[5] = cosZ*cosX;
        rotationMatrix[6] = -cosZ*sinX;
        rotationMatrix[7] = 0.0f;
        rotationMatrix[8] = -sinY*cosZ;
        rotationMatrix[9] = sinY*sinZ*cosX + cosY*sinX;
        rotationMatrix[10] = -sinY*sinZ*sinX + cosY*cosX;
        rotationMatrix[11] = rotationMatrix[12] = rotationMatrix[13] = rotationMatrix[14] = 0.0f;
        rotationMatrix[15] = 1.0f;

        return rotationMatrix;

    }

    // PROPOSED SOLUTION IN https://issuetracker.google.com/issues/36923403, Aug 17, 2011 10:20PM
    // (but this is not what is needed here: seems to be rotation in the world coordinate space, not in the model coordinate space!)
    /*
    float sinX = (float) Math.sin(Math.PI*eulerX/180.0);
    float sinY = (float) Math.sin(Math.PI*eulerY/180.0);
    float sinZ = (float) Math.sin(Math.PI*eulerZ/180.0);
    float cosX = (float) Math.cos(Math.PI*eulerX/180.0);
    float cosY = (float) Math.cos(Math.PI*eulerY/180.0);
    float cosZ = (float) Math.cos(Math.PI*eulerZ/180.0);
    float cosXsinY = cosX * sinY;
    float sinXsinY = sinX * sinY;
    rotationMatrix[0] = cosY * cosZ;
    rotationMatrix[1] = -cosY * sinZ;
    rotationMatrix[2] = sinY;
    rotationMatrix[3] = 0.0f;
    rotationMatrix[4] = sinXsinY * cosZ + cosX * sinZ;
    rotationMatrix[5] = -sinXsinY * sinZ + cosX * cosZ;
    rotationMatrix[6] = -sinX * cosY;
    rotationMatrix[7] = 0.0f;
    rotationMatrix[8] = -cosXsinY * cosZ + sinX * sinZ;
    rotationMatrix[9] = cosXsinY * sinZ + sinX * cosZ;
    rotationMatrix[10] = cosX * cosY;
    rotationMatrix[11] = 0.0f;
    rotationMatrix[12] = 0.0f;
    rotationMatrix[13] = 0.0f;
    rotationMatrix[14] = 0.0f;
    rotationMatrix[15] = 1.0f; */

    /**
     * Given are a rotation axis and a rotation angle around this axis in 3d space.
     * Returned is the angle for a rotation around the x axis that is to be applied
     * together with corresponding rotations around the y axis and z axis
     * (see methods eulerAngleY() and eulerAngleZ()) to get the same rotation.
     * @param axis The rotation axis (as a vector with origin (0,0,0)).
     * @param angle The rotation angle (degrees).
     * @return The rotation angle around the x axis (degrees).
     */

    public static float eulerAngleX(float[] axis, float angle) {
        // calculation according to https://gamedev.stackexchange.com/questions/50963/how-to-extract-euler-angles-from-transformation-matrix
        float[] rotMatrix = new float[16];
        Matrix.setIdentityM(rotMatrix,0);
        Matrix.rotateM(rotMatrix,0,angle,axis[0],axis[1],axis[2]);
        /*
        Log.v("DEMO2","RotMatrix X:");
        for (int i=0; i<4; i++) {
            String out = "";
            for (int j=0; j<4; j++)
                out += rotMatrix[i*4+j]+" ";
            Log.v("DEMO2",out);
        }
         */
        return (float) -Math.toDegrees(Math.atan2(-rotMatrix[6],rotMatrix[5]));
    }

    /**
     * Given are a rotation axis and a rotation angle around this axis in 3d space.
     * Returned is the angle for a rotation around the y axis that is to be applied
     * together with corresponding rotations around the x axis and z axis
     * (see methods eulerAngleX() and eulerAngleZ()) to get the same rotation.
     * @param axis The rotation axis (as a vector with origin (0,0,0)).
     * @param angle The rotation angle (degrees).
     * @return The rotation angle around the y axis (degrees).
     */

    public static float eulerAngleY(float[] axis, float angle) {
        // calculation according to https://gamedev.stackexchange.com/questions/50963/how-to-extract-euler-angles-from-transformation-matrix
        float[] rotMatrix = new float[16];
        Matrix.setIdentityM(rotMatrix,0);
        Matrix.rotateM(rotMatrix,0,angle,axis[0],axis[1],axis[2]);
        return (float) -Math.toDegrees(Math.atan2(-rotMatrix[8],rotMatrix[0]));
    }

    /**
     * Given are a rotation axis and a rotation angle around this axis in 3d space.
     * Returned is the angle for a rotation around the z axis that is to be applied
     * together with corresponding rotations around the x axis and y axis
     * (see methods eulerAngleX() and eulerAngleY()) to get the same rotation.
     * @param axis The rotation axis (as a vector with origin (0,0,0)).
     * @param angle The rotation angle (degrees).
     * @return The rotation angle around the z axis (degrees).
     */

    public static float eulerAngleZ(float[] axis, float angle) {
        // calculation according to https://gamedev.stackexchange.com/questions/50963/how-to-extract-euler-angles-from-transformation-matrix
        float[] rotMatrix = new float[16];
        Matrix.setIdentityM(rotMatrix,0);
        Matrix.rotateM(rotMatrix,0,angle,axis[0],axis[1],axis[2]);
        return (float) -Math.toDegrees(Math.asin(rotMatrix[4]));
    }

    /**
     * Given is a 4x4 matrix specifying a rotation in 3d space.
     * Returned is the corresponding rotation angle.
     * @param rotMatrix The rotation matrix.
     * @return The rotation angle (degrees)
     * or -1000 if rotMatrix is not a valid rotation matrix.
     */

    public static float rotAngleFrom4x4RotationMatrix(float[][] rotMatrix) {
        if (!is4x4RotationMatrix(rotMatrix)) return -1000;
        // get the rotation axis
        float[] rotAxis = rotAxisFrom4x4RotationMatrix(rotMatrix);
        // find a vector v that is perpendicular to the rotation axis
        float[] v = new float[3];
        if (!isZero(rotAxis[0])) { v[0]=-(rotAxis[1]+rotAxis[2])/rotAxis[0]; v[1]=v[2]=1; }
        else if (!isZero(rotAxis[1])) { v[0]=v[2]=1; v[1]=-rotAxis[2]/rotAxis[1]; }
        else { v[0]=v[1]=1; v[2]=0; }
        // rotate v by the rotation matrix
        float[] rotV = new float[3];
        rotV[0] = rotMatrix[0][0]*v[0]+rotMatrix[0][1]*v[1]+rotMatrix[0][2]*v[2];
        rotV[1] = rotMatrix[1][0]*v[0]+rotMatrix[1][1]*v[1]+rotMatrix[1][2]*v[2];
        rotV[2] = rotMatrix[2][0]*v[0]+rotMatrix[2][1]*v[1]+rotMatrix[2][2]*v[2];
        // calculate the angle between v and rotV
        float normedDotProduct = GraphicsUtilsCV.dotProduct3D(v,rotV)/(vectorLength(v)*vectorLength(rotV));
        if (normedDotProduct>1) normedDotProduct=1; // to handle rounding errors
        if (normedDotProduct<-1) normedDotProduct=-1;
        return (float) Math.toDegrees(Math.acos(normedDotProduct));
    }

    /**
     * Given is a 4x4 matrix specifying a rotation in 3d space.
     * Returned is the corresponding rotation axis.
     * @param rotMatrix The rotation matrix.
     * @return A float array of length 3 with the x, y, and z coordinates of the rotation axis in positions 0-2
     * or null if rotMatrix is not a valid rotation matrix.
     */

    public static float[] rotAxisFrom4x4RotationMatrix(float[][] rotMatrix) {
        // if (!is4x4RotationMatrix(rotMatrix)) return null;
        // calculation according to https://www.euclideanspace.com/maths/geometry/rotations/conversions/matrixToAngle/index.htm
        // and https://en.wikipedia.org/wiki/Rotation_matrix#Conversion_from_rotation_matrix_to_axis%E2%80%93angle
        float[] result = new float[3];
        if (isIdentity(rotMatrix)) {
            // identity matrix > no rotation > return some arbitrary axis
            result[0] = 1;
            result[1] = result[2] = 0;
            return result;
        }
        float epsilon = 1E-12f;
        if (valuesEqual(rotMatrix[0][1],rotMatrix[1][0],epsilon)&&valuesEqual(rotMatrix[0][2],rotMatrix[2][0],epsilon)&&valuesEqual(rotMatrix[2][1],rotMatrix[1][2],epsilon)) {
            // Matrix is symmetric > rotation angle is n*180 degrees.
            // The following code is based on https://www.euclideanspace.com/maths/geometry/rotations/conversions/matrixToAngle/index.htm
		    float diag_x = (rotMatrix[0][0]+1)/2;
            float diag_y = (rotMatrix[1][1]+1)/2;
            float diag_z = (rotMatrix[2][2]+1)/2;
            float matr_01 = (rotMatrix[0][1]+rotMatrix[1][0])/4;
            float matr_02 = (rotMatrix[0][2]+rotMatrix[2][0])/4;
            float matr_12 = (rotMatrix[1][2]+rotMatrix[2][1])/4;
			if ((diag_x>diag_y)&&(diag_x>diag_z)) {
				if (diag_x<epsilon) {
					result[0] = 0;
					result[1] = (float) -Math.sqrt(0.5); // 0.7071;
					result[2] = (float) -Math.sqrt(0.5);
				} else {
					result[0] = (float) -Math.sqrt(diag_x);
					result[1] = -matr_01/result[0];
					result[2] = -matr_02/result[0];
				}
			} else if (diag_y>diag_z) {
				if (diag_y<epsilon) {
					result[0] = (float) -Math.sqrt(0.5);
					result[1] = 0;
					result[2] = (float) -Math.sqrt(0.5);
				} else {
					result[1] = (float) -Math.sqrt(diag_y);
					result[0] = -matr_01/result[1];
					result[2] = -matr_12/result[1];
				}
			} else {
				if (diag_z<epsilon) {
					result[0] = (float) -Math.sqrt(0.5);
					result[1] = (float) -Math.sqrt(0.5);
					result[2] = 0;
				} else {
					result[2] = (float) -Math.sqrt(diag_z);
					result[0] = -matr_02/result[2];
					result[1] = -matr_12/result[2];
				}
			}
            // for (int i=0;i<4;i++)
            //    result[i] = Math.abs(result[i]);
            return result;
        }
        result[0] = rotMatrix[1][2]-rotMatrix[2][1];
        result[1] = rotMatrix[2][0]-rotMatrix[0][2];
        result[2] = rotMatrix[0][1]-rotMatrix[1][0];
        float norm = vectorLength(result);
        for (int i=0;i<3;i++)
            result[i] /= norm;
        return result;
    }

    /** Method to calculate a number of points in 2D space lying equidistantly on a circle around a center.
     * The first point will have the coordinates (0,radius), the following points will be calculated in counter-clockwise order.
     * @param centerX Center of the circle - X coordinate
     * @param centerY Center of the circle - Y coordinate
     * @param radius Radius of the circle
     * @param numberOfPoints Number of points to be placed on the circle
     * @return An array with the x-y coordinates of the points (x coordinate of point i in position [i][0], y coordinate in position [i][1])
     */

    public static float[][] pointsOnCircle2D(float centerX, float centerY, float radius, int numberOfPoints) {
        float[][] result = new float[numberOfPoints][2];
        for (int i=0;i<numberOfPoints;i++) {
            result[i][0] = -(float)(centerX+radius*Math.sin(2*Math.PI/numberOfPoints*i));
            result[i][1] = -(float)(centerY-radius*Math.cos(2*Math.PI/numberOfPoints*i));
            // Log.v("GLDEMO", result[i][0]+" "+result[i][1]);
        }
        return result;
    }

    /** Method to calculate a number of points in 3D space lying equidistantly on a circle.
     * @param center Center of the circle (array of length 3 - x, y, and z coordinate). Must be an array of length 3.
     * @param radius Radius of the circle. Must be greater than zero.
     * @param perpendicularVector Vector that is perpendicular to the plane in which the circle shall lie
     *                            and thus defines the orientation of the 2D circle in 3D space.
     *                            If null the circle will lie in the x-y plane, i.e. will not be rotated.
     *                            If not null it must be an array of length 3.
     * @param numberOfPoints Number of points to be placed on the circle. Must be greater than 1.
     * @return An array specifying the coordinates of the points (second dimension: 0 - x coordinate, 1 - y coordinate, 2 - z coordinate).
     * Null if one of the parameters is not valid.
     */

    public static float[][] pointsOnCircle3D(float[] center, float radius, float[] perpendicularVector, int numberOfPoints) {
        if (center==null||center.length!=3||radius<=0||numberOfPoints<2) return null;
        float[][] result = new float[numberOfPoints][3];
        // 1.) Construct a circle with the given radius around the origin (0,0,0) in the x-y plane
        for (int i=0;i<numberOfPoints;i++) {
            result[i][0] = (float)(radius*Math.sin(2* Math.PI/numberOfPoints*i));
            result[i][1] = (float)(radius*Math.cos(2* Math.PI/numberOfPoints*i));
            result[i][2] = 0;
        }
        // 2.) Rotate the circle to align it in 3D space
        if (perpendicularVector!=null&&GraphicsUtilsCV.vectorLength(perpendicularVector)!=0) {
            if (perpendicularVector.length!=3) return null;
            if (!(GraphicsUtilsCV.valuesEqual(perpendicularVector[0],0)
                    && GraphicsUtilsCV.valuesEqual(perpendicularVector[1],0))) {
                float[] z_axis = {0, 0, 1};
                float[] rotAxis = GraphicsUtilsCV.crossProduct3D(z_axis, perpendicularVector);
                float rotAngle = (float) Math.toDegrees(Math.acos(GraphicsUtilsCV.dotProduct3D(z_axis, GraphicsUtilsCV.getNormalizedVectorCopy3D(perpendicularVector))));
                float[] rotMatrix = new float[16];
                Matrix.setIdentityM(rotMatrix, 0);
                Matrix.rotateM(rotMatrix, 0, rotAngle, rotAxis[0], rotAxis[1], rotAxis[2]);
                for (int i = 0; i < numberOfPoints; i++) {
                    float[] point = GraphicsUtilsCV.homogeneousCoordsForPoint3D(result[i]);
                    Matrix.multiplyMV(point, 0, rotMatrix, 0, point, 0);
                    result[i] = GraphicsUtilsCV.coordsFromHomogeneous3D(point);
                }
            }
        }
        // 3.) Translate the circle to its center
        for (int i=0;i<numberOfPoints;i++)
            for (int j=0; j<3; j++)
                result[i][j] += center[j];
        return result;
    }

    /** Method to calculate a number of points in 2D space lying equidistantly on an ellipse around a center.
     * The ellipse is implemented by a circle that is compressed vertically by a specified compression factor
     * and then rotated by a specified angle around its center.
     * @param centerX Center of the ellipse - X coordinate
     * @param centerY Center of the ellipse - Y coordinate
     * @param radius Radius of the ellipse
     * @param comprFactor; // Vertical compression of the circle (0<=comprFactor<=1), yielding the ellipse
     * @param rotAngle; // Rotation angle of the resulting ellipse (in radians)
     * @param numberOfPoints Number of points to be placed on the circle
     * @return An array with 'numberOfPoints' Point objects specifying the coordinates of the points
     */

    public static Point[] pointsOnEllipse(int centerX, int centerY, int radius, double comprFactor, double rotAngle, int numberOfPoints) {
        Point[] result = new Point[numberOfPoints];
        Point centerEll = new Point(centerX,centerY);
        Point startPoint = new Point(centerX+radius,centerY);
        for (int i=0;i<numberOfPoints;i++) {
            Point ptOnCircle = GraphicsUtilsCV.rotateAroundPoint(startPoint,centerEll,i*2* Math.PI/numberOfPoints);
            int compressedY = centerY + (int)(comprFactor*(centerY-ptOnCircle.y));
            Point ptOnEllipseUnrotated = new Point(ptOnCircle.x,compressedY);
            result[i] = GraphicsUtilsCV.rotateAroundPoint(ptOnEllipseUnrotated,centerEll,rotAngle);
        }
        return result;
    }

    /**
     * Method to rotate a point in 2D space around a center.
     *
     * @param toRotateX Point to be rotated: x coordinate
     * @param toRotateY Point to be rotated: y coordinate
     * @param centerX Center of the rotation: x coordinate
     * @param centerY Center of the rotation: y coordinate
     * @param angle Rotation angle (in radians)
     * @return The point after the rotation
     */

    public static Point rotateAroundPoint(int toRotateX, int toRotateY, int centerX, int centerY, double angle) {
        int normX = toRotateX - centerX;
        int normY = toRotateY - centerY;
        int rotX = (int) (normX * Math.cos(angle) - normY * Math.sin(angle)) + centerX;
        int rotY = (int) (normX * Math.sin(angle) + normY * Math.cos(angle)) + centerY;
        return new Point(rotX,rotY);
    }

    /** Method to rotate a point in 2D space around a center.
     *
     * @param toRotate Point to be rotated
     * @param center Center of the rotation
     * @param angle Rotation angle (in radians)
     * @return The point after the rotation
     */

    public static Point rotateAroundPoint(Point toRotate, Point center, double angle) {
        return rotateAroundPoint(toRotate.x,toRotate.y,center.x,center.y,angle);
    }

    /**
     * Method to rotate a point by some angle around some axis in 3D space.
     * @param point The point to rotate.
     * @param axisPoint1 The first point defining the rotation axis.
     * @param axisPoint2 The second point defining the rotation axis.
     * @param angle The rotation angle (degrees).
     * @return A new point resulting from the rotation (the point passed as a parameter will remain unchanged)
     * or null if one of the parameters is not valid.
     */

    public static float[] rotateAroundAxis(float[] point, float[] axisPoint1, float[] axisPoint2, float angle) {
        if (point==null||point.length!=3||axisPoint1==null||axisPoint1.length!=3||axisPoint2==null||axisPoint2.length!=3) return null;
        float[] rotMatrix = new float[16];
        Matrix.setIdentityM(rotMatrix,0);
        Matrix.rotateM(rotMatrix,0,angle,axisPoint2[0]-axisPoint1[0],axisPoint2[1]-axisPoint1[1],axisPoint2[2]-axisPoint1[2]);
        float[] temp = new float[4];
        temp[0] = point[0]-axisPoint1[0];
        temp[1] = point[1]-axisPoint1[1];
        temp[2] = point[2]-axisPoint1[2];
        temp[3] = 1;
        Matrix.multiplyMV(temp,0,rotMatrix,0,temp,0);
        float[] result = new float[3];
        for (int i=0;i<3;i++) result[i]=temp[i]+axisPoint1[i];
        return result;
    }

    /** Method to calculate points on a quadratic Bezier curve in 3D space.
     *
     * @param t The Bezier parameter.
     * @param start The start point of the Bezier curve (array of length 3 with the x, y, and z coordinate).
     * @param contr The control point of the Bezier curve (array of length 3 with the x, y, and z coordinate).
     * @param end The end point of the Bezier curve (array of length 3 with the x, y, and z coordinate).
     * @return The point on the Bezier curve for parameter t.
     */

    public static float[] bezier(float t, float[] start, float[] contr, float[] end) {
        return bezier(t,start,contr,null,end);
    }

    /** Method to calculate points on a quadratic or cubic Bezier curve in 3D space.
     *
     * @param t The Bezier parameter.
     * @param start The start point of the Bezier curve (array of length 3 with the x, y, and z coordinate).
     * @param contr1 The first control point of the Bezier curve (array of length 3 with the x, y, and z coordinate).
     * @param contr2 The second control point of the Bezier curve (array of length 3 with the x, y, and z coordinate).
     *               If null the Bezier curve is quadratic.
     * @param end The end point of the Bezier curve (array of length 3 with the x, y, and z coordinate).
     * @return The point on the Bezier curve for parameter t.
     */

    public static float[] bezier(float t, float[] start, float[] contr1, float[] contr2, float[] end) {
        float [] result = new float[3];
        if (contr2 == null) {
            result[0] = ((start[0] - 2 * contr1[0] + end[0]) * t * t + (-2 * start[0] + 2 * contr1[0]) * t + start[0]);
            result[1] = ((start[1] - 2 * contr1[1] + end[1]) * t * t + (-2 * start[1] + 2 * contr1[1]) * t + start[1]);
            result[2] = ((start[2] - 2 * contr1[2] + end[2]) * t * t + (-2 * start[2] + 2 * contr1[2]) * t + start[2]);
        }
        else {
            result[0] = (1 - t) * (1 - t) * (1 - t) * start[0] + 3 * (1 - t) * (1 - t) * t * contr1[0] + 3 * (1 - t) * t * t * contr2[0] + t * t * t * end[0];
            result[1] = (1 - t) * (1 - t) * (1 - t) * start[1] + 3 * (1 - t) * (1 - t) * t * contr1[1] + 3 * (1 - t) * t * t * contr2[1] + t * t * t * end[1];
            result[2] = (1 - t) * (1 - t) * (1 - t) * start[2] + 3 * (1 - t) * (1 - t) * t * contr1[2] + 3 * (1 - t) * t * t * contr2[2] + t * t * t * end[2];
        }
        /*
                static float[] prevPoint = {0,0,0};
                static long prevTime = 0;
        long currTime = System.nanoTime();
        long diff = currTime-prevTime;
        // Log.v("GLDEMO",t+" "+distance(prevPoint,result)/diff*1000000000);
        prevPoint = result;
        prevTime = currTime;
        */
        return result;
    }

    /** Method to calculate the approximate length a quadratic or cubic Bezier curve in 3D space.
     * The calculation is done by determining a number of points on the curve and summing the distances between the points
     *
     * @param start The start point of the Bezier curve (array of length 3 with the x, y, and z coordinate).
     * @param contr1 The first control point of the Bezier curve (array of length 3 with the x, y, and z coordinate).
     * @param contr2 The second control point of the Bezier curve (array of length 3 with the x, y, and z coordinate).
     *               If null the Bezier curve is quadratic.
     * @param end The end point of the Bezier curve (array of length 3 with the x, y, and z coordinate).
     * @return The point on the Bezier curve for parameter t.
     */

    public static float bezierLength(float[] start, float[] contr1, float[] contr2, float[] end) {
        final float stepSize = 0.01f;
        float length = 0, t = stepSize;
        float[] prevPoint = start;
        while (t<1+stepSize) {
            float[] currPoint;
            if (t<1)
                currPoint = bezier(t,start,contr1,contr2,end);
            else
                currPoint = end;
            length += distance3D(currPoint,prevPoint);
            // Log.v("GLDEMO",t+" "+distance(currPoint,prevPoint));
            prevPoint = currPoint;
            t += stepSize;
        }
        return length;
    }

    /** Color constant */
    public static final float[] white = { 1.0f, 1.0f, 1.0f, 1.0f };
    /** Color constant */
    public static final float[] black = { 0.0f, 0.0f, 0.0f, 1.0f };
    /** Color constant */
    public static final float[] yellow = { 1.0f, 1.0f, 0.0f, 1.0f };
    /** Color constant */
    public static final float[] lightyellow = { 1.0f, 1.0f, 0.5f, 1.0f };
    /** Color constant */
    public static final float[] orange = { 1.0f, 0.647f, 0.0f, 1.0f };
    /** Color constant */
    public static final float[] red = { 1.0f, 0.0f, 0.0f, 1.0f };
    /** Color constant */
    public static final float[] lightred = { 1.0f, 0.28f, 0.3f, 1.0f };
    /** Color constant */
    public static final float[] blue = { 0.0f, 0.0f, 1.0f, 1.0f };
    /** Color constant */
    public static final float[] lightblue = { 0.678f, 0.847f, 0.902f, 1.0f };
    /** Color constant */
    public static final float[] green = { 0.0f, 1.0f, 0.0f, 1.0f };
    /** Color constant */
    public static final float[] lightgreen = { 0.565f, 0.933f, 0.565f, 1.0f };
    /** Color constant */
    public static final float[] darkgreen = { 0.0f, 0.3922f, 0.0f, 1.0f };
    /** Color constant */
    public static final float[] cyan = { 0.0f, 1.0f, 1.0f, 1.0f };
    /** Color constant */
    public static final float[] magenta = { 1.0f, 0.0f, 1.0f, 1.0f };
    /** Color constant */
    public static final float[] purple = { 0.5785f, 0.4392f, 0.8588f, 1.0f };

    /**
     * Method that returns the RGBA representation of a grey color
     * @param brightness The brightness of the color as a percentage of white (0% = black, 100% = full white).
     *                   The percentage can be defined either a.) in the interval [0,1] (i.e. 0<=brightness<=1)
     *                   or b.) in the interval (1,100], i.e. 1<brightness<=100.
     * @return A float array - either a.) { brightness, brightness, brightness, 1 } or b.) { brightness/100, brightness/100, brightness/100, 1 }
     */

    public static float[] grey(double brightness) {
        if (brightness<=0) return new float[] { 0f, 0f, 0f, 1f };
        if (brightness>=100) return new float[] { 1f, 1f, 1f, 1f };
        if (brightness>1) brightness /= 100;
        return new float[] { (float) brightness, (float) brightness, (float) brightness, 1f };
    }

    /**
     * Method that returns the RGBA representation of a warm grey color, i.e. a grey with 6% yellow added.
     * @param brightness The brightness of the grey color as a percentage of white (0% = black, 100% = full white).
     *                   The percentage can be defined either a.) in the interval [0,1] (i.e. 0<=brightness<=1)
     *                   or b.) in the interval (1,100], i.e. 1<brightness<=100.
     * @return A float array - either a.) { brightness, brightness, brightness, 1 } or b.) { brightness/100, brightness/100, brightness/100, 1 }
     */

    public static float[] warmgrey(double brightness) {
        if (brightness<=0) return new float[] { 0f, 0f, 0f, 1f };
        if (brightness>=100) return new float[] { 1f, 1f, 1f, 1f };
        if (brightness>1) brightness /= 100;
        float redgreen = (float)(brightness+0.06f);
        if (redgreen>1) redgreen = 1;
        return new float[] { redgreen, redgreen, (float) brightness, 1f };
    }

    /**
     * Method that returns the RGBA representation of a cold grey color, i.e. a grey with 6% blue added.
     * @param brightness The brightness of the grey color as a percentage of white (0% = black, 100% = full white).
     *                   The percentage can be defined either a.) in the interval [0,1] (i.e. 0<=brightness<=1)
     *                   or b.) in the interval (1,100], i.e. 1<brightness<=100.
     * @return A float array - either a.) { brightness, brightness, brightness, 1 } or b.) { brightness/100, brightness/100, brightness/100, 1 }
     */

    public static float[] coldgrey(double brightness) {
        if (brightness<=0) return new float[] { 0f, 0f, 0f, 1f };
        if (brightness>=100) return new float[] { 1f, 1f, 1f, 1f };
        if (brightness>1) brightness /= 100;
        float blue = (float)(brightness+0.06f);
        if (blue>1) blue = 1;
        return new float[] { (float) brightness, (float) brightness, blue, 1f };
    }

    /**  TO BE EXTENDED (3D points, NON-INTERSECTING LINES)  > CALL METHOD shortestSegmentBetweenTwoLines()
     * Auxiliary method to find the control point for a quadratic Bezier curve to smoothly connect two vectors.
     * @param v1_start The start point of the first vector (x,y,z coordinates).
     * @param v1_end The end point of the first vector (x,y,z coordinates).
     * @param v2_start The start point of the second vector (x,y,z coordinates).
     * @param v2_end The end point of the second vector (x,y,z coordinates).
     * @return The control point (x,y,z coordinates).
     */

    public static float[] bezierControlPointToConnectTwoVectors(float[] v1_start, float[] v1_end, float[] v2_start, float[] v2_end) {
        float[] coeffsA = GraphicsUtilsCV.linearFunctionCoeffsFromPoints2D(v1_start,v1_end);
        float[] coeffsB = GraphicsUtilsCV.linearFunctionCoeffsFromPoints2D(v2_start,v2_end);
        float[] intersec = GraphicsUtilsCV.intersectionLinearFunctions2D(coeffsA[0],coeffsA[1],coeffsB[0],coeffsB[1]);
        float[] result = new float[3];
        result[0]=intersec[0];
        result[1]=intersec[1];
        result[2] = 0;
        return result;
    }

    /**
     * Value providers generate a sequence of values that can be used to animate graphical objects.
     * The values are stored and returned in float arrays and can hence be e.g. coordinates in 2D or 3D space or RGBA color values or single float values.
     */

    public static abstract class ValueProvider {

        /**
         * Any kind of information about the provider.
         * Code that uses objects of this class may freely define and use values for this attribute;
         * there are no predefined values with specific semantics.
         */

        private final Object info;

        ValueProvider() {
            this(null);
        }

        ValueProvider(Object info) {
            this.info = info;
        }

        public Object getInfo() { return this.info; }

        /**
         * The method returns the current values of the provider without modifying them
         * @return The current values
         */

        public abstract float[] getCurrentValues();

        /**
         * The method updates the current values of the provider and returns these updated values
         * @return The updated current values
         */

        public abstract float[] getNextValues();

    }

   /**
     * Class for value providers that sweep (to and fro) an interval of float values.
     */

    public static class ValueProviderSweepInterval extends ValueProvider {

        /** The current value of the provider */
        private float currentValue;

        /** The lower bound of the interval */
        private final float minValue;

        /** The upper bound of the interval */
        private final float maxValue;

        /** The step size for the sweep */
        private float stepSize;

        /**
         * @param info any kind of information about the provider
         * @param startValue start value for the provider
         * @param minValue lower bound of the interval
         * @param maxValue upper bound of the interval
         * @param stepSize step size for the sweep
         */

        public ValueProviderSweepInterval(Object info, float startValue, float minValue, float maxValue, float stepSize) {
            super(info);
            this.currentValue = startValue;
            this.maxValue = maxValue;
            this.minValue = minValue;
            this.stepSize = stepSize;
        }

        /**
         * @return array of length 1 with the current value within the interval
         */

        public float[] getCurrentValues() {
            return new float[] { currentValue };
        }

        /**
         * Updates the current value by incrementing or decrementing it by the step size
         * @return array of length 1 with the updated current value within the interval
         */

        public float[] getNextValues() {
            if (currentValue+stepSize>maxValue||currentValue+stepSize<minValue) stepSize=-stepSize;
            currentValue += stepSize;
            return getCurrentValues();
        }

    }

    /**
     * Class for value providers that sweep (to and fro) multiple intervals of float values
     * and return arrays with the current values within these intervals
     */

    public static class ValueProviderSweepIntervals extends ValueProvider {

        /** The current values of the provider */
        private final float[] currentValues;

        /** The lower bounds of the intervals */
        private final float[] minValues;

        /** The upper bounds of the intervals */
        private final float[] maxValues;

        /** The step sizes for the sweeps */
        private final float[] stepSizes;

        /**
         * @param info any kind of information about the provider
         * @param startValues start values for the provider
         * @param minValues lower bounds of the intervals
         * @param maxValues upper bounds of the intervals
         * @param stepSizes step sizes for the sweeps
         */

        public ValueProviderSweepIntervals(Object info, float[] startValues, float[] minValues, float[] maxValues, float[] stepSizes) {
            super(info);
            this.currentValues = startValues.clone();
            this.maxValues = maxValues;
            this.minValues = minValues;
            this.stepSizes = stepSizes;
        }

        /**
         * @return array with the current values within the intervals
         */

        public float[] getCurrentValues() {
            return currentValues.clone();
        }

        /**
         * Updates the current values by incrementing or decrementing them by the step sizes
         * @return array with the updated current values within the intervals
         */

        public float[] getNextValues() {
            for (int i=0;i<currentValues.length;i++) {
                if (currentValues[i] + stepSizes[i] > maxValues[i] || currentValues[i] + stepSizes[i] < minValues[i])
                    stepSizes[i] = -stepSizes[i];
                currentValues[i] += stepSizes[i];
            }
            return getCurrentValues();
        }

    }

    /**
     * Class for value providers that rotate a number of points in 3D.
     * The current implementation is limited to the rotation of points around the z axis.
     */

    public static class ValueProviderRotation extends ValueProvider {

        /** The points to rotate */
        private final float[] pointsToRotate;

        /** The step size for incrementing the angle */
        private final float angleStep;

        /** The current rotation angle */
        private float currentAngle;

        /** The current rotated points */
        private final float[] currentValues;

        /**
         * @param info any kind of information about the provider
         * @param pointsToRotate Array defining a number of points in 3D space that shall be rotated. It stores the x, y, and z coordinates of the ith point at its positions 3*i, 3*i+1, and 3*i+2
         * @param angleStep step size for incrementing the angle
         */

        public ValueProviderRotation(Object info, float[] pointsToRotate, float angleStep) {
            super(info);
            this.pointsToRotate = pointsToRotate.clone();
            this.angleStep = angleStep;
            this.currentAngle = 0;
            this.currentValues = new float[pointsToRotate.length];
        }

        /**
         * Calculates the coordinates of the base points rotated by the current rotation angle.
         * @return array with the x, y, and z coordinates of the rotated ith point at its positions 3*i, 3*i+1, and 3*i+2
         */

        public float[] getCurrentValues() {
            double sin = Math.sin(currentAngle*Math.PI/180f), cos = Math.cos(currentAngle*Math.PI/180f);
            for (int i = 0; i<pointsToRotate.length/3; i++) {
                currentValues[3*i] = (float) (pointsToRotate[3*i] * cos - pointsToRotate[3*i+1] * sin);
                currentValues[3*i+1] = (float) (pointsToRotate[3*i] * sin + pointsToRotate[3*i+1] * cos);
                currentValues[3*i+2] = pointsToRotate[3*i+2];
            }
            // Log.v("GLDEMO",currentAngle+"  "+sin+" "+cos+" "+currentValues[0]+" "+currentValues[1]+" "+currentValues[2]);
            return currentValues.clone();
        }

        /**
         * Increments the angle by the angle step size and calculates the coordinates of the points rotated by the updated rotation angle.
         * @return array with the updated x, y, and z coordinates of the rotated ith point at its positions 3*i, 3*i+1, and 3*i+2
         */

        public float[] getNextValues() {
            currentAngle = (currentAngle+angleStep)%360;
            return getCurrentValues();
        }

    }

    /**
     * Class for value providers that sweep (to and fro) the range between two colors
     * and return arrays with the current color values.
     * <BR>
     * A value provider is initialized with a 'startColor' and an 'endColor',
     * both float arrays of length 4 with element values between 0.0f and 1.0f being the RGBA representation of a color.
     * The provider will traverse (to and fro) the four intervals for the R, G, B, and A values defined by these two colors.
     * The step size of this traversal is specified as a percentage of the interval size
     * such that the absolute step size is larger for larger intervals and smaller for smaller ones.
     * Hence all four interval values will arrive simultaneously at the end color and the start color.
     * <BR>
     * The length of the value array returned by the methods of such a provider is either 4 (containing one set of RGBA values)
     * or a multiple of four (containing the same RGBA quadruple multiple times, one after the other).
     * The latter allows a caller to assign the color value directly to a sequence of pixels (or the like).
     */

    public static class ValueProviderSweepColorRange extends ValueProvider {

        /** The current values of the provider */
        private final float[] currentValues;

        /** The start color where the provider begins */
        private final float[] startColor;

        /** The end color where the provider "turns round" */
        private final float[] endColor;

        /** The step sizes for the color component */
        private final float[] stepSizes;

        /**
         * @param info any kind of information about the provider - a value that can be freely chosen and used by the calling code
         * @param startColor the color where the provider begins
         * @param endColor the color where the provider "turns round""
         * @param relativeStepSize the percentage of the interval between startColor[i] and endColor[i] by which the current value shall be increased/decreased (as a value between 0.0f and 1.0f)
         * @param resultSizeMultiplier to determine the size of the returned arrays, i.e. the "multiple" mentioned in the last paragraph of the class comment.
         */

        public ValueProviderSweepColorRange(Object info, float[] startColor, float[] endColor, float relativeStepSize, int resultSizeMultiplier) {
            super(info);
            this.currentValues = new float[4*resultSizeMultiplier];
            for (int i=0;i<currentValues.length;i++)
                currentValues[i] = startColor[i%4];
            this.startColor = startColor.clone();
            this.endColor = endColor.clone();
            this.stepSizes = new float[4];
            for (int i=0;i<4;i++)
                this.stepSizes[i] = (endColor[i]-startColor[i])*relativeStepSize;
            // Log.v("GLDEMO",this.stepSizes[0]+" "+this.stepSizes[1]+" "+this.stepSizes[2]+" "+this.stepSizes[3]);
        }

        /**
         * @return array with the current values within the intervals (as to the length and content, see the last paragraph of the class comment)
         */

        public float[] getCurrentValues() {
            return currentValues.clone();
        }

        /**
         * Updates the current values by incrementing or decrementing them by the step sizes (as explained in the class comment)
         * @return array with the current values within the intervals (as to the length and content, see the last paragraph of the class comment)
         */

        public float[] getNextValues() {
            for (int i=0;i<currentValues.length;i++) {
                if (((endColor[i%4]>startColor[i%4])&&(currentValues[i] + stepSizes[i%4] > endColor[i%4] || currentValues[i] + stepSizes[i%4] < startColor[i%4])) ||
                        ((endColor[i%4]<=startColor[i%4])&&(currentValues[i] + stepSizes[i%4] < endColor[i%4] || currentValues[i] + stepSizes[i%4] > startColor[i%4])))
                    stepSizes[i%4] = -stepSizes[i%4];
                currentValues[i] += stepSizes[i%4];
            }
            return getCurrentValues();
        }

    }

    /**
     * Writes the values of an array to the LocCat
     * @param tag
     * @param array
     */

    public static void writeArrayToLog(String tag, float[] array) {
        String line = "";
        for (int i=0; i<array.length; i++)
            line += array[i]+" ";
        Log.v(tag,line);
    }

    /**
     * Writes the values of a matrix to the LocCat
     * @param tag
     * @param matrix
     */

    public static void writeMatrixToLog(String tag, float[][] matrix) {
        for (int i=0; i<matrix.length; i++) {
            String line = "";
            for (int j=0; j<matrix[i].length; j++)
                line += matrix[i][j]+" ";
            Log.v(tag,line);
        }

    }

    /**
     * COPIED FROM THE PROJECT ON 2D PROPERTY ANIMATION
     *
     * Method to calculate the horizontal distance between two objects of class AnimatedGuiObjectCV.
     * Let oleft be the left object and oright the right object, i.e. the X coordinate of the center of oleft
     * is smaller than the X coordinate of the center of oright.
     * Then the value returned is the difference between the X coordinate of the leftmost corner of the
     * enclosing rectangle of oright (= its left bound in case of an unrotated object) and the
     * X coordinate of the rightmost corner of the enclosing rectangle of oleft (= its right bound
     * in case of an unrotated object).
     *
     * @param obj1 The first object
     * @param obj2 The first object
     * @return Horizontal distance between the objects, as defined above
     */

    // public static int distCentersHoriz(AnimatedGuiObjectCV obj1, AnimatedGuiObjectCV obj2) {

    /**
     * COPIED FROM THE PROJECT ON 2D PROPERTY ANIMATION
     *
     * Method to calculate the vertical distance between two objects of class AnimatedGuiObjectCV.
     * Let otop be the upper object and obottom the lower object, i.e. the Y coordinate of the center of otop
     * is smaller than the Y coordinate of the center of obottom.
     * Then the value returned is the difference between the Y coordinate of the upmost corner of the
     * enclosing rectangle of obottom (= its top bound in case of an unrotated object) and the
     * Y coordinate of the lowermost corner of the enclosing rectangle of otop (= its lower bound
     * in case of an unrotated object).
     *
     * @param obj1 The first object
     * @param obj2 The first object
     * @return Vertical distance between the objects, as defined above
     */

    // public static int distCentersVert(AnimatedGuiObjectCV obj1, AnimatedGuiObjectCV obj2) {

    /**
     * COPIED FROM THE PROJECT ON 2D PROPERTY ANIMATION
     *
     * Method to find the object with the leftmost bound in a collection of objects.
     * @param objects The collection of objects
     * @return The object with the leftmost bound
     */

    // public static AnimatedGuiObjectCV objectWithLeftmostBound(Collection<AnimatedGuiObjectCV> objects) {

    /**
     * COPIED FROM THE PROJECT ON 2D PROPERTY ANIMATION
     *
     * Method to find the object with the leftmost center in a collection of objects.
     * @param objects The collection of objects
     * @return The object with the leftmost center
     */

    // public static AnimatedGuiObjectCV objectWithLeftmostCenter(Collection<AnimatedGuiObjectCV> objects) {

    /**
     * COPIED FROM THE PROJECT ON 2D PROPERTY ANIMATION
     *
     * Method to find the object with the rightmost bound in a collection of objects.
     * @param objects The collection of objects
     * @return The object with the rightmost bound
     */

    // public static AnimatedGuiObjectCV objectWithRightmostBound(Collection<AnimatedGuiObjectCV> objects) {

    /**
     * COPIED FROM THE PROJECT ON 2D PROPERTY ANIMATION
     *
     * Method to find the object with the rightmost center in a collection of objects.
     * @param objects The collection of objects
     * @return The object with the rightmost center
     */

    // public static AnimatedGuiObjectCV objectWithRightmostCenter(Collection<AnimatedGuiObjectCV> objects) {

    /**
     * COPIED FROM THE PROJECT ON 2D PROPERTY ANIMATION
     *
     * Method to find the object with the topmost bound in a collection of objects.
     * @param objects The collection of objects
     * @return The object with the topmost bound
     */

    // public static AnimatedGuiObjectCV objectWithTopmostBound(Collection<AnimatedGuiObjectCV> objects) {

    /**
     * COPIED FROM THE PROJECT ON 2D PROPERTY ANIMATION
     *
     * Method to find the object with the topmost center in a collection of objects.
     * @param objects The collection of objects
     * @return The object with the topmost center
     */

    // public static AnimatedGuiObjectCV objectWithTopmostCenter(Collection<AnimatedGuiObjectCV> objects) {

    /**
     * COPIED FROM THE PROJECT ON 2D PROPERTY ANIMATION
     *
     * Method to find the object with the bottommost bound in a collection of objects.
     * @param objects The collection of objects
     * @return The object with the bottommost bound
     */

    // public static AnimatedGuiObjectCV objectWithBottommostBound(Collection<AnimatedGuiObjectCV> objects) {

    /**
     * COPIED FROM THE PROJECT ON 2D PROPERTY ANIMATION
     *
     * Method to find the object with the bottommost center in a collection of objects.
     * @param objects The collection of objects
     * @return The object with the bottommost center
     */

    // public static AnimatedGuiObjectCV objectWithBottommostCenter(Collection<AnimatedGuiObjectCV> objects) {

    /**
     * COPIED FROM THE PROJECT ON 2D PROPERTY ANIMATION
     *
     * Method to sort a collection of objects according to their horizontal positions (= x coordinates of their centers).
     * @param objects The collection of objects
     * @return An ArrayList with the objects sorted in ascending order according to their horizontal positions
     */

    // public static ArrayList<AnimatedGuiObjectCV> sortObjectsHorizCenters(Collection<AnimatedGuiObjectCV> objects) {

    /**
     * Method to sort a collection of objects according to their vertical positions (= y coordinates of their centers).
     * @param objects The collection of objects
     * @return An ArrayList with the objects sorted in ascending order according to their vertical positions
     */

    // public static ArrayList<AnimatedGuiObjectCV> sortObjectsVertCenters(Collection<AnimatedGuiObjectCV> objects) {

}