// This work is provided under GPLv3, the GNU General Public License 3
//   http://www.gnu.org/licenses/gpl-3.0.html

// Prof. Dr. Carsten Vogt
// Technische Hochschule Köln, Germany
// Fakultät für Informations-, Medien- und Elektrotechnik
// carsten.vogt@th-koeln.de
// 29.1.2023

package de.thkoeln.cvogt.android.opengl_utilities;

import java.util.ArrayList;

/**
 * Class with some utility methods for the OpenGL-related classes and objects defined in this package.
 * The class GraphicsUtilsCV contains utility methods for 2D and 3D graphics programming in general.
 * @see GraphicsUtilsCV
 */

public class GLUtilsCV {

    /** Method to test if a ray goes through a shape.
     * <P>
     * N.B. The current implementation of the test is rather basic.
     * It just checks if the distance between the ray and the center of the shape is smaller than the arithmetic mean of the current sizes of the shape in the x, y,, and z dimension.
     * <P>
     * A precise implementation should check for each triangle of the shape if the ray goes through the triangle,
     * e.g. by the Moeller-Trumbore algorithm (https://en.wikipedia.org/wiki/M%C3%B6ller%E2%80%93Trumbore_intersection_algorithm)
     * or the Badouel algorithm (https://en.wikipedia.org/wiki/Badouel_intersection_algorithm).
     *
     * @param rayStart The start point of the ray (x/y/z coordinates).
     * @param rayVector The direction vector of the ray (x/y/z coordinates).
     * @param shape The shape.
     * @return true iff the distance between the center of the shape and the ray is smaller than the above-mentioned value.
     * (false if one of the parameters is not valid).
     */

    public static boolean rayGoesThroughShape(float[] rayStart, float[] rayVector, GLShapeCV shape) {
        if (rayStart==null||rayStart.length!=3||rayVector==null||rayVector.length!=3||shape==null) return false;
        float sizeX = shape.getIntrinsicSize(0)*shape.getScaleX();
        float sizeY = shape.getIntrinsicSize(1)*shape.getScaleY();
        float sizeZ = shape.getIntrinsicSize(2)*shape.getScaleZ();
        float threshold = (sizeX+sizeY+sizeZ)/3;
        return (GraphicsUtilsCV.distancePointToLine3D(shape.getTrans(),rayStart,rayVector)<threshold);
    }


    /** Method to find, in a set of shapes, those shapes through which a specific ray goes.
     * <P>
     * N.B. The current implementation of the method is rather basic - see method rayGoesThroughShape().
     *
     * @param rayStart The start point of the ray (x/y/z coordinates).
     * @param rayVector The direction vector of the ray (x/y/z coordinates).
     * @param shapes The set of shapes.
     * @return An ArrayList containing those shapes through which the ray goes (or null if one of the parameters is not vald).
     */

    public static ArrayList<GLShapeCV> shapesOnRay(float[] rayStart, float[] rayVector, ArrayList<GLShapeCV> shapes) {
        if (rayStart==null||rayStart.length!=3||rayVector==null||rayVector.length!=3||shapes==null||shapes.size()==0) return null;
        ArrayList<GLShapeCV> result = new ArrayList<>();
        for (GLShapeCV shape : shapes)
            if (rayGoesThroughShape(rayStart,rayVector,shape))
                result.add(shape);
        return result;
    }

    /** Method to find, in a set of shapes, all shapes through which a specific ray goes
     * and then, among these shapes, the shape closest to the start point of the ray.
     * <P>
     * N.B. The current implementation of the method is rather basic - see method rayGoesThroughShape().
     *
     * @param rayStart The start point of the ray (x/y/z coordinates).
     * @param rayVector The direction vector of the ray (x/y/z coordinates).
     * @param shapes The set of shapes.
     * @return The shape on the ray closest to the start point (or null if one of the parameters is not valid).
     */

    public static GLShapeCV closestShapeOnRay(float[] rayStart, float[] rayVector, ArrayList<GLShapeCV> shapes) {
        if (rayStart==null||rayStart.length!=3||rayVector==null||rayVector.length!=3||shapes==null||shapes.size()==0) return null;
        return shapeClosestToPoint(rayStart,shapesOnRay(rayStart,rayVector,shapes));
    }

    /** Method to find, in a set of shapes, the shape whose center lies closest to a given point.
     *
     * @param point The point (x/y/z coordinates).
     * @param shapes The set of shapes.
     * @return The shape closest to the point (or null if one of the parameters is not valid).
     */

    public static GLShapeCV shapeClosestToPoint(float[] point, ArrayList<GLShapeCV> shapes) {
        if (point==null||point.length!=3||shapes==null||shapes.size()==0) return null;
        GLShapeCV result = shapes.get(0);
        float minDist = GraphicsUtilsCV.distance3D(point,result.getTrans());
        for (GLShapeCV shape : shapes) {
            float newDist = GraphicsUtilsCV.distance3D(point,shape.getTrans());
            if (newDist<minDist) {
                minDist = newDist;
                result = shape;
            }
        }
        return result;
    }

}