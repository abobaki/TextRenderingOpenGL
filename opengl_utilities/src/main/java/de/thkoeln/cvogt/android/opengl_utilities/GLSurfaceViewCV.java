// This work is provided under GPLv3, the GNU General Public License 3
//   http://www.gnu.org/licenses/gpl-3.0.html

// Prof. Dr. Carsten Vogt
// Technische Hochschule Köln, Germany
// Fakultät für Informations-, Medien- und Elektrotechnik
// carsten.vogt@th-koeln.de
// 26.1.2023

package de.thkoeln.cvogt.android.opengl_utilities;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;
import android.widget.PopupWindow;

import java.util.ArrayList;

/**
 * Class to define views on which shapes, i.e. objects of class <I>GLShapeCV</I>, can be rendered.
 * The rendering is done by a renderer, i.e. an object of class <I>GLRendererCV</I>, that is associated with the <I>GLSurfaceViewCV</I> object.
 * The shapes to be rendered are stored in an <I>ArrayList</I> attribute of the <I>GLSurfaceViewCV</I> object.
 * The renderer calls the <I>draw()</I> methods of these shapes.
 * Shapes can be dynamically removed from and added to this list.
 * Listeners can be registered to react to touch events.
 * <BR>
 * @see de.thkoeln.cvogt.android.opengl_utilities.GLShapeCV
 * @see de.thkoeln.cvogt.android.opengl_utilities.GLRendererCV
 */

public class GLSurfaceViewCV extends GLSurfaceView {

    /** The shapes to render. */

    private final ArrayList<GLShapeCV> shapesToRender;

    /** The registered renderer. */

    private GLRendererCV renderer;

    /** The registered listeners for touch events */

    private final ArrayList<GLOnTouchListenerCV> onTouchListeners;

    /**
     * With this constructor, the render mode of the view will be set to GLSurfaceView.RENDERMODE_WHEN_DIRTY,
     * i.e. animations controlled by the associated thread will NOT become effective.
     * @param context The context in which the view is created.
     * @param renderer The renderer to be associated with the view.
     */

    public GLSurfaceViewCV(Context context, GLRendererCV renderer) {
        this(context,renderer,true);
    }

    /**
     * @param context The context in which the view is created.
     * @param renderer The renderer to be associated with the view.
     * @param renderOnlyWhenDirty If true the render mode of the view will be set to GLSurfaceView.RENDERMODE_WHEN_DIRTY,
     *                         i.e. if an animation shall be shown this parameter must be false.
     */

    public GLSurfaceViewCV(Context context, GLRendererCV renderer, boolean renderOnlyWhenDirty) {
        super(context);
        // OpenGL ES context: version 2.0
        setEGLContextClientVersion(2);
        // associate a renderer with the view - see extended setRenderer() method below
        setRenderer(renderer);
        if (renderOnlyWhenDirty)
            // Android documentation: "The renderer only renders when the surface is created, or when requestRender() is called."
            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        // initialize renderer and shapesToRender
        this.renderer = renderer;
        shapesToRender = new ArrayList<>();
        onTouchListeners = new ArrayList<>();
    }

    /**
     * When this GLSurfaceViewCV object is not shown anymore the control threads and animators of all its GLShapeCV objects are stopped.
     */

    @Override
    public void onPause() {
        for (GLShapeCV shape : shapesToRender)
            shape.stopControlThreadAndAnimators();
    }

    /**
     * Associate a renderer with the view.
     * Note that for renderers of class GLRendererCV THIS method and not the method of the base class GLSurfaceView must be called
     * because it sets the 'surfaceView' attribute of the renderer.
     * @param renderer The renderer to be associated with the view.
     */

    synchronized public void setRenderer(GLRendererCV renderer) {
        super.setRenderer(renderer);
        renderer.setSurfaceView(this);
        this.renderer = renderer;
    }

    /**
     * Get the associated renderer.
     * @return The renderer associated with the view.
     */

    public GLRendererCV getRenderer() {
        return renderer;
    }

    /**
     * Sets the values for the eye/camera position and focus, i.e. for the view matrix (through the attached renderer).
     */

    synchronized public void setEyePosAndFocus(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ) {
        if (renderer==null) return;
        renderer.setEyePosAndFocus(eyeX, eyeY, eyeZ, centerX, centerY, centerZ);
    }

    /**
     * Resets the values for the eye/camera position and focus, i.e. for the view matrix (through the attached renderer).
     */

    synchronized public void resetEyePosAndFocus() {
        if (renderer==null) return;
        renderer.resetEyePosAndFocus();
    }

    /**
     * Gets a popup window
     * through which the view matrix of this surface view and its renderer (i.e. the camera position (eyeX/Y/Z) and camera focus (centerX/Y/Z)) can be controlled.
     */

    public PopupWindow getViewMatrixControlPopup(Context context) {
        if (renderer==null) return null;
        return renderer.getViewMatrixControlPopup(context);
    }

    /**
     * Add another shape that shall be rendered. This will also start the animators defined for the shape.
     * @param shape The shape to be added.
     */

    synchronized public void addShape(GLShapeCV shape) {
        shapesToRender.add(shape);
        shape.setSurfaceView(this);
        this.post(new Runnable() {   // To make sure that the animators will be executed by the main looper; other threads are not permitted to execute them
            @Override
            public void run() {
                shape.startAnimators();
            }
        });
    }

    /**
     * Add some shapes that shall be rendered. This will also start the animators defined for the shapes.
     * @param shapes The shape to be added.
     */

    synchronized public void addShapes(GLShapeCV[] shapes) {
        for (GLShapeCV shape: shapes)
            addShape(shape);
    }

    /**
     * Get the shapes to render.
     * @return A copy of 'shapesToRender', i.e. the list of shapes to be rendered.
     */

    public ArrayList<GLShapeCV> getShapesToRender() {
        // Note: This method is intentionally NOT marked as sysnchronized.
        // Deadlocks have been observed when the renderer calls this method from its (synchronized) onDrawFrame() method - reason not yet clear.
        return (ArrayList<GLShapeCV>) shapesToRender.clone();
    }

    /**
     * Remove a shape from the list of shapes to render.
     * @return A copy of 'shapesToRender', i.e. the list of shapes to be rendered.
     */

    synchronized public void removeShape(GLShapeCV shape) {
        shapesToRender.remove(shape);
    }

    /**
     * Empty the list of the shapes to be rendered.
     */

    synchronized public void clearShapes() {
        for (GLShapeCV shape: shapesToRender)
            shape.setSurfaceView(null);
        shapesToRender.clear();
    }

    /**
     * Display the x, y, and z axes (for testing purposes).
     */

    synchronized public void showAxes() {
        addShape(GLShapeFactoryCV.makeAxes());
    }

    /**
     * Callback method for a touch event.
     * For each listener of interface GLOnTouchListenerCV registered with this surface view,
     * the method calls the onTouchEvent() method of this listener, passing an object of class GLTouchEventCV that describes the touch event.
     * @see GLTouchEventCV
     */

    @Override
    synchronized public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        // for ray tracing: shoot a ray into the 3D scene that is currently displayed on the surface view
        // (based on the explanations in https://antongerdelan.net/opengl/raycasting.html)
        final float[] projectionMatrix = renderer.getProjectionMatrix();
        final float[] viewMatrix = renderer.getViewMatrix();
        // calculate the ray in the view space by applying the inverse projection matrix
        float normalizedX = ((2.0f * event.getX()) / getWidth()) - 1.0f;
        float normalizedY = 1.0f - (2.0f * event.getY()) / getHeight();
        float[] ray_clip = {normalizedX, normalizedY, -1f, 1f};
        final float[] invertedMatrix = new float[16];
        Matrix.invertM(invertedMatrix, 0, projectionMatrix, 0);
        float[] ray_view = new float[4];
        Matrix.multiplyMV(ray_view, 0, invertedMatrix, 0, ray_clip, 0);
        ray_view[2] = -1; ray_view[3] = 0;
        // transform the ray into the world space by applying the inverse view matrix
        Matrix.invertM(invertedMatrix, 0, viewMatrix, 0);
        float[] ray_world = new float[4];
        Matrix.multiplyMV(ray_world, 0, invertedMatrix, 0, ray_view, 0);
        ray_world = GraphicsUtilsCV.coordsFromHomogeneous3D(ray_world);
        GraphicsUtilsCV.normalizeVector3D(ray_world);
        // inform all registered listeners
        for (GLOnTouchListenerCV listener : onTouchListeners)
            listener.onTouch(this,new GLTouchEventCV(event,renderer.getEyePos(),ray_world));
        return true;
    }

    /**
     * Interface for listeners that shall react to touch events.
     */

    public interface GLOnTouchListenerCV {

        /**
         * Method that is called by the onTouchEvent() method of a surface view.
         * @param surfaceView The calling surface view.
         * @param event The reported event.
         * @return
         */
        boolean onTouch(GLSurfaceViewCV surfaceView, GLTouchEventCV event);

    }

    /**
     * Add a touch listener.
     * @param listener The listener to be added.
     */

    synchronized public void addOnTouchListener(GLOnTouchListenerCV listener) {
        onTouchListeners.add(listener);
    }

    /**
     * Removes a touch listener.
     * @param listener The listener to be removed.
     */

    synchronized public void removeOnTouchListener(GLOnTouchListenerCV listener) {
        onTouchListeners.remove(listener);
    }

}
